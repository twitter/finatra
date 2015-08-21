package com.twitter.finatra.http.internal.marshalling

import com.twitter.concurrent.exp.AsyncStream
import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finatra.http.response.{ResponseBuilder, StreamingResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.util.{Future, FuturePool}
import javax.inject.Inject

/*
 * Note: We are using deprecated classManifest and <:< to avoid potential threading issues
 * with 2.10 reflection methods. See http://docs.scala-lang.org/overviews/reflection/thread-safety.html
 */
class CallbackConverter @Inject()(
  messageBodyManager: MessageBodyManager,
  responseBuilder: ResponseBuilder,
  mapper: FinatraObjectMapper,
  jsonStreamParser: JsonStreamParser) {

  /* Public */

  def convertToFutureResponse[RequestType: Manifest, ResponseType: Manifest](callback: RequestType => ResponseType): Request => Future[Response] = {
    val requestConvertedCallback: (Request => ResponseType) = createRequestCallback[RequestType, ResponseType](callback)
    createResponseCallback[ResponseType](requestConvertedCallback)
  }

  /* Private */

  private def createRequestCallback[RequestType: Manifest, ResponseType: Manifest](callback: (RequestType) => ResponseType): (Request) => ResponseType = {
    val requestManifest = manifest[RequestType]
    requestManifest match {

      // The callback takes a finagle Request
      case finagleRequest if finagleRequest <:< classManifest[Request] =>
        callback.asInstanceOf[(Request => ResponseType)]

      // AsyncStream[T] //TODO Also support AsyncStream[Try[T]]
      case stream if isAsyncStream[RequestType] =>
        request: Request =>
          val parsedStream = jsonStreamParser.parseArray(request.reader)(requestManifest.typeArguments.head).asInstanceOf[RequestType]
          callback(parsedStream)

      // Otherwise convert Request into the callback's input type
      case _ =>
        request: Request =>
          val callbackInput = messageBodyManager.parse[RequestType](request)
          callback(callbackInput)
    }
  }

  private def createResponseCallback[ResponseType: Manifest](requestCallback: (Request) => ResponseType): (Request) => Future[Response] = {
    if (isFutureResponse[ResponseType]) {
      requestCallback.asInstanceOf[(Request => Future[Response])]
    }
    else if (isFutureOption[ResponseType]) {
      request: Request =>
        requestCallback(request).asInstanceOf[Future[Option[_]]] map optionToHttpResponse
    }
    else if (isAsyncStream[ResponseType]) {
      request: Request =>
        val asyncStream = requestCallback(request).asInstanceOf[AsyncStream[_]]

        val streamingResponse = StreamingResponse.jsonArray(
          toBuf = mapper.writeValueAsBuf,
          asyncStream = asyncStream)

        streamingResponse.toFutureFinagleResponse
    }
    else if (isFuture[ResponseType]) {
      request: Request =>
        requestCallback(request).asInstanceOf[Future[_]] map createHttpResponse
    }
    else if (isStreamingResponse[ResponseType]) {
      request: Request =>
        requestCallback(request).asInstanceOf[StreamingResponse[_]].toFutureFinagleResponse
    }
    else if (isResponse[ResponseType]) {
      request: Request =>
        Future(
          requestCallback(request).asInstanceOf[Response])
    }
    else if (isOption[ResponseType]) {
      request: Request =>
        Future(
          optionToHttpResponse(requestCallback(request).asInstanceOf[Option[_]]))
    }
    else {
      request: Request =>
        Future(
          createHttpResponse(requestCallback(request)))
    }
  }

  private def optionToHttpResponse(response: Option[_]): Response = {
    response map createHttpResponse getOrElse {
      responseBuilder.notFound("")
    }
  }

  private def createHttpResponse(any: Any): Response = {
    any match {
      case response: Response => response
      case _ => responseBuilder.ok(any)
    }
  }

  private def isFutureResponse[T: Manifest]: Boolean = {
    manifest[T] <:< classManifest[Future[Response]]
  }

  private def isResponse[T: Manifest]: Boolean = {
    manifest[T] <:< classManifest[Response]
  }

  private def isStreamingResponse[T: Manifest]: Boolean = {
    manifest[T].runtimeClass == classOf[StreamingResponse[_]]
  }

  private def isAsyncStream[T: Manifest]: Boolean = {
    manifest[T].runtimeClass == classOf[AsyncStream[_]]
  }

  private def isFutureOption[T: Manifest]: Boolean = {
    isFuture[T] && hasOptionTypeParam
  }

  private def isOption[T: Manifest] = {
    manifest[T].runtimeClass == classOf[Option[_]]
  }

  private def hasOptionTypeParam[T: Manifest] = {
    val typeArgs = manifest[T].typeArguments
    typeArgs.size == 1 &&
      typeArgs.head.runtimeClass == classOf[Option[_]]
  }

  private def isFuture[T: Manifest]: Boolean = {
    manifest[T].runtimeClass == classOf[Future[_]]
  }
}
