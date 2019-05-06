package com.twitter.finatra.http.internal.marshalling

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http._
import com.twitter.finatra.http.internal.marshalling.CallbackConverter.url
import com.twitter.finatra.http.response.{ResponseBuilder, StreamingResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.inject.TypeUtils
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Future, FuturePool, Promise}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext => ScalaExecutionContext, Future => ScalaFuture}
import scala.util.{Failure, Success}
import scala.reflect.runtime.universe._

private object CallbackConverter {
  val url =
    "https://twitter.github.io/finatra/user-guide/http/controllers.html#controllers-and-routing"
}

private[http] class CallbackConverter @Inject()(
  messageBodyManager: MessageBodyManager,
  responseBuilder: ResponseBuilder,
  mapper: FinatraObjectMapper,
  jsonStreamParser: JsonStreamParser) {

  /* Public */

  def convertToFutureResponse[RequestType: TypeTag, ResponseType: TypeTag](
    callback: RequestType => ResponseType
  ): Request => Future[Response] = {
    val requestConvertedCallback: (Request => ResponseType) =
      createRequestCallback[RequestType, ResponseType](callback)
    createResponseCallback[RequestType, ResponseType](requestConvertedCallback)
  }

  /* Private */

  /**
   * Returns a Function1 of Request to ResponseType, convert the Request to RequestType
   * based on the Manifest type.
   *
   * @note If the RequestType is a stream of [[com.twitter.io.Buf]], it will not be
   *       parsed as a regular Json request.
   */
  private def createRequestCallback[RequestType: TypeTag, ResponseType: TypeTag](
    callback: RequestType => ResponseType
  ): Request => ResponseType = {
    val manifestRequestType = TypeUtils.asManifest[RequestType]

    if (manifestRequestType == manifest[Request]) {
      callback.asInstanceOf[(Request => ResponseType)]
    } else if (runtimeClassEqs[AsyncStream[_]](manifestRequestType)) {
      val asyncStreamTypeParam = manifestRequestType.typeArguments.head
      request: Request =>
        val asyncStream = jsonStreamParser.parseArray(request.reader)(asyncStreamTypeParam)
        callback(asyncStream.asInstanceOf[RequestType])
    } else if (runtimeClassEqs[Reader[_]](manifestRequestType)) {
      val readerTypeParam = manifestRequestType.typeArguments.head
      request: Request =>
        val reader = jsonStreamParser.parseJson(request.reader)(readerTypeParam)
        callback(reader.asInstanceOf[RequestType])
    } else if (runtimeClassEqs[Int](manifestRequestType) || runtimeClassEqs[String](manifestRequestType)) {
      // NOTE: callback functions with no input param which return a String are inferred as a RequestType of
      // `Int` by Scala because `StringOps.apply` is a function of Int => Char, other return types
      // infer a `RequestType` of `String`.
      throw new Exception(
        s"Improper callback function RequestType: ${manifestRequestType.runtimeClass}. Controller routes defined with a callback function that has no input parameter or with an incorrectly specified input parameter type are not allowed. " +
          s"Please specify an input parameter in your route callback function of the appropriate type. For more details see: $url"
      )
    } else { request: Request =>
      val callbackInput = messageBodyManager.read(request)(manifestRequestType)
      callback(callbackInput)
    }
  }

  /**
   * Returns a Function1 of Request to Future[Response], convert the ResponseType
   * to Future[Response] based on the Manifest type.
   */
  private def createResponseCallback[RequestType: TypeTag, ResponseType: TypeTag](
    requestCallback: Request => ResponseType
  ): Request => Future[Response] = {
    val manifestResponseType = TypeUtils.asManifest[ResponseType]

    manifestResponseType match {
      case futureResponse if futureResponse == manifest[Future[Response]] =>
        requestCallback.asInstanceOf[(Request => Future[Response])]
      case futureOption if isFutureOption(futureOption) =>
        // we special-case in order to convert None --> 404 NotFound
        request: Request =>
          requestCallback(request)
            .asInstanceOf[Future[Option[_]]].map(optionToHttpResponse(request))
      case response if response == manifest[Response] =>
        request: Request =>
          Future(requestCallback(request).asInstanceOf[Response])
      case future if runtimeClassEqs[Future[_]](future) =>
        request: Request =>
          requestCallback(request).asInstanceOf[Future[_]].map(createHttpResponse(request))
      case scalaFuture if runtimeClassEqs[ScalaFuture[_]](scalaFuture) =>
        scalaFutureTypes(requestCallback)(manifestResponseType)
      case option if runtimeClassEqs[Option[_]](option) =>
        request: Request =>
          Future(optionToHttpResponse(request)(requestCallback(request).asInstanceOf[Option[_]]))
      case string if runtimeClassEqs[String](string) =>
        // optimized
        request: Request =>
          Future.value(
            createHttpResponseWithContent(
              status = Status.Ok,
              content = Buf.Utf8(requestCallback(request).asInstanceOf[String]),
              contentType = responseBuilder.plainTextContentType
            )
          )
      case stringMap if isStringMap(stringMap) =>
        // optimized
        request: Request =>
          Future.value(
            createHttpResponseWithContent(
              status = Status.Ok,
              content = mapper
                .writeStringMapAsBuf(requestCallback(request).asInstanceOf[Map[String, String]]),
              contentType = responseBuilder.jsonContentType
            )
          )
      case map if runtimeClassEqs[Map[_, _]](map) =>
        // optimized
        request: Request =>
          val response = Response(Version.Http11, Status.Ok)
          response.content = mapper.writeValueAsBuf(requestCallback(request))
          response.headerMap.addUnsafe(Fields.ContentType, responseBuilder.jsonContentType)
          Future.value(response)
      case streamingType if isStreamingType(streamingType) =>
        streamingTypes[RequestType, ResponseType](requestCallback, streamingType)
      case _ =>
        request: Request =>
          requestCallback(request) match {
            case throwable: Throwable => Future.exception(throwable)
            case futureResult: Future[_] => futureResult.map(createHttpResponse(request))
            case result => Future(createHttpResponse(request)(result))
          }
    }
  }

  /**
   * Returns if the ResponseType is one of the streaming types.
   */
  private def isStreamingType(manifested: Manifest[_]): Boolean = {
    runtimeClassEqs[AsyncStream[_]](manifested) ||
    runtimeClassEqs[Reader[_]](manifested) ||
    runtimeClassEqs[StreamingResponse[_, _]](manifested)
  }

  /**
   * Cases that the ResponseType is one of the streaming types.
   *
   * @note If the RequestType is a stream of [[com.twitter.io.Buf]], it will not be
   *       parsed as a regular Json request.
   * @note If the ResponseType is a stream of [[com.twitter.io.Buf]], it will not be transformed
   *       to Buf.
   */
  private def streamingTypes[RequestType: TypeTag, ResponseType: TypeTag](
    requestCallback: Request => ResponseType,
    manifested: Manifest[_]
  ): Request => Future[Response] = {
    val (jsonPrefix, jsonSeparator, jsonSuffix) = {
      val typeArgs = TypeUtils.asManifest[RequestType].typeArguments
      if (typeArgs.size == 1 && typeArgs.head.runtimeClass == classOf[Buf]) {
        (None, None, None)
      } else {
        (Some(Buf.Utf8("[")), Some(Buf.Utf8(",")), Some(Buf.Utf8("]")))
      }
    }
    manifested match {
      case asyncStreamBuf if asyncStreamBuf == manifest[AsyncStream[Buf]] =>
        request: Request =>
          val asyncStream = requestCallback(request).asInstanceOf[AsyncStream[Buf]]
          val streamingResponse =
            StreamingResponse.apply[Buf](toBuf = a => a)(asyncStream = asyncStream)
          streamingResponse.toFutureFinagleResponse
      case asyncStream if runtimeClassEqs[AsyncStream[_]](asyncStream) =>
        request: Request =>
          val asyncStream = requestCallback(request).asInstanceOf[AsyncStream[_]]
          val streamingResponse =
            StreamingResponse.apply(
              toBuf = mapper.writeValueAsBuf,
              prefix = jsonPrefix,
              separator = jsonSeparator,
              suffix = jsonSuffix)(asyncStream = asyncStream)
          streamingResponse.toFutureFinagleResponse
      case readerBuf if readerBuf == manifest[Reader[Buf]] =>
        request: Request =>
          val reader = requestCallback(request).asInstanceOf[Reader[Buf]]
          val streamingResponse =
            StreamingResponse.apply[Buf](toBuf = a => a)(asyncStream = Reader.toAsyncStream(reader))
          streamingResponse.toFutureFinagleResponse
      case reader if runtimeClassEqs[Reader[_]](reader) =>
        request: Request =>
          val reader = requestCallback(request).asInstanceOf[Reader[_]]
          val streamingResponse = StreamingResponse.apply(
            toBuf = mapper.writeValueAsBuf,
            prefix = jsonPrefix,
            separator = jsonSeparator,
            suffix = jsonSuffix)(asyncStream = Reader.toAsyncStream(reader))
          streamingResponse.toFutureFinagleResponse
      case streamingResponse if runtimeClassEqs[StreamingResponse[_, _]](streamingResponse) =>
        request: Request =>
          requestCallback(request).asInstanceOf[StreamingResponse[_, _]].toFutureFinagleResponse
    }
  }

  /**
   * Cases that the ResponseType is a ScalaFuture.
   */
  private def scalaFutureTypes[ResponseType: Manifest](
    requestCallback: Request => ResponseType
  ): Request => Future[Response] = {
    if (isScalaFutureOption[ResponseType]) {
      val fn = (request: Request) =>
        toTwitterFuture(
          requestCallback
            .asInstanceOf[Request => ScalaFuture[Option[_]]](request))(immediatePoolExcCtx)
      createResponseCallback(fn)
    } else {
      val fn = (request: Request) =>
        toTwitterFuture(
          requestCallback
            .asInstanceOf[Request => ScalaFuture[_]](request))(immediatePoolExcCtx)
      createResponseCallback(fn)
    }
  }

  private def createHttpResponseWithContent(
    status: Status,
    content: Buf,
    contentType: String
  ): Response = {
    assert(
      contentType == responseBuilder.jsonContentType ||
        contentType == responseBuilder.plainTextContentType
    )

    val orig = Response(status)
    orig.content = content
    orig.headerMap.addUnsafe(Fields.ContentType, contentType)
    orig
  }

  private def optionToHttpResponse(request: Request)(response: Option[_]): Response = {
    response.map(createHttpResponse(request)).getOrElse {
      responseBuilder.notFound("")
    }
  }

  private def createHttpResponse(request: Request)(any: Any): Response = {
    any match {
      case response: Response => response
      case _ => responseBuilder.ok(request, any)
    }
  }

  private def runtimeClassEq[T: Manifest, U: Manifest]: Boolean = {
    manifest[T].runtimeClass == manifest[U].runtimeClass
  }

  private def runtimeClassEqs[U: Manifest](manifested: Manifest[_]): Boolean = {
    manifested.runtimeClass == manifest[U].runtimeClass
  }

  private def isStringMap(manifested: Manifest[_]): Boolean = {
    val typeArgs = manifested.typeArguments
    runtimeClassEqs[Map[_, _]](manifested) &&
    typeArgs.size == 2 &&
    typeArgs.head.runtimeClass == classOf[String] &&
    typeArgs.tail.head.runtimeClass == classOf[String]
  }

  private def isFutureOption(manifested: Manifest[_]): Boolean = {
    val typeArgs = manifested.typeArguments
    runtimeClassEqs[Future[_]](manifested) &&
    typeArgs.size == 1 &&
    typeArgs.head.runtimeClass == classOf[Option[_]]
  }

  private def isScalaFutureOption[T: Manifest]: Boolean = {
    val typeArgs = manifest[T].typeArguments
    runtimeClassEq[T, ScalaFuture[_]] &&
    typeArgs.size == 1 &&
    typeArgs.head.runtimeClass == classOf[Option[_]]
  }

  private def toTwitterFuture[A](
    scalaFuture: ScalaFuture[A]
  )(
    implicit executor: ScalaExecutionContext
  ): Future[A] = {
    val p = new Promise[A]()
    scalaFuture.onComplete {
      case Success(value) => p.setValue(value)
      case Failure(exception) => p.setException(exception)
    }
    p
  }

  // Ideally, it would be up to the user to determine where next to run their code
  // but exposing that explicitly would be very difficult just for addressing this case.
  // By using the `c.t.util.FuturePool.immediatePool`, the user will still have control
  // over the thread pool at least tangentially in that whatever thread satisfied the
  // Future (Promise) is where the Future conversion will be run (and presumably whatever comes
  // after since we use a thread local scheduler by default).
  private[this] val immediatePoolExcCtx = new ExecutionContext(FuturePool.immediatePool)

  /** ExecutionContext adapter using a FuturePool; see bijection/TwitterExecutionContext */
  private[this] class ExecutionContext(pool: FuturePool, report: Throwable => Unit)
      extends ScalaExecutionContext {
    def this(pool: FuturePool) = this(pool, ExecutionContext.ignore)
    override def execute(runnable: Runnable): Unit = {
      pool(runnable.run())
      ()
    }

    override def reportFailure(t: Throwable): Unit = report(t)
  }

  private[this] object ExecutionContext {
    private def ignore(throwable: Throwable): Unit = {
      // do nothing
    }
  }
}
