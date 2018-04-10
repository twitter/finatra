package com.twitter.finatra.http.internal.marshalling

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http._
import com.twitter.finatra.http.response.{ResponseBuilder, StreamingResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.io.Buf
import com.twitter.util.{Future, FuturePool, Promise}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext => ScalaExecutionContext, Future => ScalaFuture}
import scala.util.{Failure, Success}

private[http] class CallbackConverter @Inject()(
  messageBodyManager: MessageBodyManager,
  responseBuilder: ResponseBuilder,
  mapper: FinatraObjectMapper,
  jsonStreamParser: JsonStreamParser
) {

  /* Public */

  def convertToFutureResponse[RequestType: Manifest, ResponseType: Manifest](
    callback: RequestType => ResponseType
  ): Request => Future[Response] = {
    val requestConvertedCallback: (Request => ResponseType) =
      createRequestCallback[RequestType, ResponseType](callback)
    createResponseCallback[ResponseType](requestConvertedCallback)
  }

  /* Private */

  private def createRequestCallback[RequestType: Manifest, ResponseType: Manifest](
    callback: RequestType => ResponseType
  ): Request => ResponseType = {
    if (manifest[RequestType] == manifest[Request]) {
      callback.asInstanceOf[(Request => ResponseType)]
    } else if (runtimeClassEq[RequestType, AsyncStream[_]]) {
      val asyncStreamTypeParam = manifest[RequestType].typeArguments.head
      request: Request =>
        val asyncStream = jsonStreamParser.parseArray(request.reader)(asyncStreamTypeParam)
        callback(asyncStream.asInstanceOf[RequestType])
    } else if (runtimeClassEq[RequestType, Int]) {
      // NOTE: "empty" route callbacks that return a String are inferred as a RequestType of
      // Int by Scala because StringOps.apply is a function of Int => Char.
      throw new Exception(
        "Routes with empty (with no parameter) route callbacks or route callbacks with a parameter of type Int are not allowed. " +
          "Please specify a parameter in your route callback of the appropriate type."
      )
    } else { request: Request =>
      val callbackInput = messageBodyManager.read[RequestType](request)
      callback(callbackInput)
    }
  }

  private def createResponseCallback[ResponseType: Manifest](
    requestCallback: (Request) => ResponseType
  ): (Request) => Future[Response] = {
    if (manifest[ResponseType] == manifest[Future[Response]]) {
      requestCallback.asInstanceOf[(Request => Future[Response])]
    } else if (isFutureOption[ResponseType]) {
      // we special-case in order to convert None --> 404 NotFound
      request: Request =>
        requestCallback(request).asInstanceOf[Future[Option[_]]].map(optionToHttpResponse(request))
    } else if (runtimeClassEq[ResponseType, AsyncStream[_]]) { request: Request =>
      val asyncStream = requestCallback(request).asInstanceOf[AsyncStream[_]]

      val streamingResponse =
        StreamingResponse.jsonArray(toBuf = mapper.writeValueAsBuf, asyncStream = asyncStream)

      streamingResponse.toFutureFinagleResponse
    } else if (runtimeClassEq[ResponseType, Future[_]]) { request: Request =>
      requestCallback(request).asInstanceOf[Future[_]].map(createHttpResponse(request))
    } else if (runtimeClassEq[ResponseType, StreamingResponse[_, _]]) { request: Request =>
      requestCallback(request).asInstanceOf[StreamingResponse[_, _]].toFutureFinagleResponse
    } else if (manifest[ResponseType] == manifest[Response]) { request: Request =>
      Future(requestCallback(request).asInstanceOf[Response])
    } else if (runtimeClassEq[ResponseType, Option[_]]) { request: Request =>
      Future(optionToHttpResponse(request)(requestCallback(request).asInstanceOf[Option[_]]))
    } else if (runtimeClassEq[ResponseType, String]) {
      // optimized
      request: Request =>
        Future.value(
          createHttpResponseWithContent(
            status = Status.Ok,
            content = Buf.Utf8(requestCallback(request).asInstanceOf[String]),
            contentType = responseBuilder.plainTextContentType
          )
        )
    } else if (isStringMap[ResponseType]) {
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
    } else if (runtimeClassEq[ResponseType, Map[_, _]]) {
      // optimized
      request: Request =>
        val response = Response(Version.Http11, Status.Ok)
        response.content = mapper.writeValueAsBuf(requestCallback(request))
        response.headerMap.add(Fields.ContentType, responseBuilder.jsonContentType)
        Future.value(response)
    } else if (runtimeClassEq[ResponseType, ScalaFuture[_]]) {
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
    } else { request: Request =>
      requestCallback(request) match {
        case throwable: Throwable => Future.exception(throwable)
        case futureResult: Future[_] => futureResult.map(createHttpResponse(request))
        case result => Future(createHttpResponse(request)(result))
      }
    }
  }

  private def createHttpResponseWithContent(
    status: Status,
    content: Buf,
    contentType: String
  ): Response = {
    val orig = Response(status)
    orig.content = content
    orig.headerMap.add(Fields.ContentType, contentType)
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

  private def isStringMap[T: Manifest]: Boolean = {
    val typeArgs = manifest[T].typeArguments
    runtimeClassEq[T, Map[_, _]] &&
    typeArgs.size == 2 &&
    typeArgs.head.runtimeClass == classOf[String] &&
    typeArgs.tail.head.runtimeClass == classOf[String]
  }

  private def isFutureOption[T: Manifest]: Boolean = {
    val typeArgs = manifest[T].typeArguments
    runtimeClassEq[T, Future[_]] &&
    typeArgs.size == 1 &&
    typeArgs.head.runtimeClass == classOf[Option[_]]
  }

  private def isScalaFutureOption[T: Manifest]: Boolean = {
    val typeArgs = manifest[T].typeArguments
    runtimeClassEq[T, ScalaFuture[_]] &&
    typeArgs.size == 1 &&
    typeArgs.head.runtimeClass == classOf[Option[_]]
  }

  private def toTwitterFuture[A](scalaFuture: ScalaFuture[A])
    (implicit executor: ScalaExecutionContext): Future[A] = {
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
  private[this] class ExecutionContext(
   pool: FuturePool,
   report: Throwable => Unit
  ) extends ScalaExecutionContext {
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
