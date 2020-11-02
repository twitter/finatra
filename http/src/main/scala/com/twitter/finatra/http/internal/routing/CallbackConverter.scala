package com.twitter.finatra.http.internal.routing

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Fields, Request, Response, Status, Version}
import com.twitter.finatra.http.marshalling.MessageBodyManager
import com.twitter.finatra.http.response.{
  ResponseBuilder,
  StreamingResponse => DeprecatedStreamingResponse
}
import com.twitter.finatra.http.streaming.{FromReader, StreamingRequest, StreamingResponse}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.streaming.JsonStreamParser
import com.twitter.inject.TypeUtils
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Future, FuturePool, Promise}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext => ScalaExecutionContext, Future => ScalaFuture}
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}

private[http] trait CallbackConverter {
  def convertToFutureResponse[RequestType: TypeTag, ResponseType: TypeTag](
    callback: RequestType => ResponseType
  ): Request => Future[Response]
}

private object CallbackConverterImpl {
  val url =
    "https://twitter.github.io/finatra/user-guide/http/controllers.html#controllers-and-routing"
}

private[http] class CallbackConverterImpl @Inject() (
  messageBodyManager: MessageBodyManager,
  responseBuilder: ResponseBuilder,
  mapper: ScalaObjectMapper,
  jsonStreamParser: JsonStreamParser)
    extends CallbackConverter {

  import CallbackConverterImpl._

  /* Public */

  def convertToFutureResponse[RequestType: TypeTag, ResponseType: TypeTag](
    callback: RequestType => ResponseType
  ): Request => Future[Response] = {
    val requestConvertedCallback: Request => ResponseType =
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

    manifestRequestType match {
      case request if request == manifest[Request] =>
        callback.asInstanceOf[Request => ResponseType]
      case streamingRequest if runtimeClassEqs[StreamingRequest[Any, _]](streamingRequest) =>
        val streamIdentity = streamingRequest.typeArguments.head
        val streamType = streamingRequest.typeArguments.last
        request: Request =>
          val streamingRequest = streamIdentity match {
            case reader if runtimeClassEqs[Reader[_]](reader) =>
              StreamingRequest(jsonStreamParser, request)(FromReader.ReaderIdentity, streamType)
            case asyncStream if runtimeClassEqs[AsyncStream[_]](asyncStream) =>
              StreamingRequest.fromRequestToAsyncStream(jsonStreamParser, request)(streamType)
            case _ =>
              throw new Exception(
                s"Unsupported StreamingRequest type detected as $streamIdentity"
              )
          }
          callback(streamingRequest.asInstanceOf[RequestType])
      case asyncStream if runtimeClassEqs[AsyncStream[_]](asyncStream) =>
        val asyncStreamTypeParam = asyncStream.typeArguments.head
        request: Request =>
          val asyncStream = jsonStreamParser.parseArray(request.reader)(asyncStreamTypeParam)
          callback(asyncStream.asInstanceOf[RequestType])
      case reader if runtimeClassEqs[Reader[_]](reader) =>
        val readerTypeParam = reader.typeArguments.head
        request: Request =>
          val reader = jsonStreamParser.parseJson(request.reader)(readerTypeParam)
          callback(reader.asInstanceOf[RequestType])
      case intOrString
          if runtimeClassEqs[Int](intOrString) || runtimeClassEqs[String](intOrString) =>
        // NOTE: callback functions with no input param which return a String are inferred as a RequestType of
        // `Int` by Scala because `StringOps.apply` is a function of Int => Char, other return types
        // infer a `RequestType` of `String`.
        throw new Exception(
          s"Improper callback function RequestType: ${manifestRequestType.runtimeClass}. Controller routes defined with a callback function that has no input parameter or with an incorrectly specified input parameter type are not allowed. " +
            s"Please specify an input parameter in your route callback function of the appropriate type. For more details see: $url"
        )
      case _ =>
        request: Request =>
          val callbackInput = messageBodyManager.read[RequestType](request)
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
        requestCallback.asInstanceOf[Request => Future[Response]]
      case futureOption if isFutureOption(futureOption) =>
        // we special-case in order to convert None --> 404 NotFound
        request: Request =>
          requestCallback(request)
            .asInstanceOf[Future[Option[_]]].map(optionToHttpResponse(request))
      case response if response == manifest[Response] =>
        request: Request => Future(requestCallback(request).asInstanceOf[Response])
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
    runtimeClassEqs[DeprecatedStreamingResponse[_, _]](manifested) ||
    runtimeClassEqs[StreamingResponse[Any, _]](manifested)
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
    manifested match {
      case asyncStreamBuf if asyncStreamBuf == manifest[AsyncStream[Buf]] =>
        request: Request =>
          val asyncStream = requestCallback(request).asInstanceOf[AsyncStream[Buf]]
          responseBuilder.streaming(asyncStream).toFutureResponse()
      case asyncStream if runtimeClassEqs[AsyncStream[_]](asyncStream) =>
        request: Request =>
          val asyncStream = requestCallback(request).asInstanceOf[AsyncStream[_]]
          responseBuilder.streaming(asyncStream).toFutureResponse()
      case readerBuf if readerBuf == manifest[Reader[Buf]] =>
        request: Request =>
          val reader = requestCallback(request).asInstanceOf[Reader[Buf]]
          responseBuilder.streaming(reader).toFutureResponse()
      case reader if runtimeClassEqs[Reader[_]](reader) =>
        request: Request =>
          val reader = requestCallback(request).asInstanceOf[Reader[_]]
          responseBuilder.streaming(reader).toFutureResponse()
      case depStreamingResponse
          if runtimeClassEqs[DeprecatedStreamingResponse[_, _]](depStreamingResponse) =>
        request: Request =>
          requestCallback(request)
            .asInstanceOf[DeprecatedStreamingResponse[_, _]].toFutureFinagleResponse
      case streamingResponse if runtimeClassEqs[StreamingResponse[Any, _]](streamingResponse) =>
        request: Request =>
          streamingResponse.typeArguments.head match {
            case _: Reader[_] =>
              requestCallback(request).asInstanceOf[StreamingResponse[Reader, _]].toFutureResponse()
            case _: AsyncStream[_] =>
              requestCallback(request)
                .asInstanceOf[StreamingResponse[AsyncStream, _]].toFutureResponse()
            case _ =>
              requestCallback(request).asInstanceOf[StreamingResponse[Any, _]].toFutureResponse()
          }
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
