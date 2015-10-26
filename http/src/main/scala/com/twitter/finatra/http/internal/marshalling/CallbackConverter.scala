package com.twitter.finatra.http.internal.marshalling

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.response.{ResponseBuilder, StreamingResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.util.Future
import javax.inject.Inject

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
    if (manifest[RequestType] == manifest[Request]) {
      callback.asInstanceOf[(Request => ResponseType)]
    }
    else if (runtimeClassEq[RequestType, AsyncStream[_]]) {
      val asyncStreamTypeParam = manifest[RequestType].typeArguments.head
      request: Request =>
        val asyncStream = jsonStreamParser.parseArray(request.reader)(asyncStreamTypeParam)
        callback(asyncStream.asInstanceOf[RequestType])
    } else {
      request: Request =>
        val callbackInput = messageBodyManager.read[RequestType](request)
        callback(callbackInput)
    }
  }

  private def createResponseCallback[ResponseType: Manifest](requestCallback: (Request) => ResponseType): (Request) => Future[Response] = {
    if (manifest[ResponseType] == manifest[Future[Response]]) {
      requestCallback.asInstanceOf[(Request => Future[Response])]
    }
    else if (isFutureOption[ResponseType]) {
      request: Request =>
        requestCallback(request).asInstanceOf[Future[Option[_]]] map optionToHttpResponse
    }
    else if (runtimeClassEq[ResponseType, AsyncStream[_]]) {
      request: Request =>
        val asyncStream = requestCallback(request).asInstanceOf[AsyncStream[_]]

        val streamingResponse = StreamingResponse.jsonArray(
          toBuf = mapper.writeValueAsBuf,
          asyncStream = asyncStream)

        streamingResponse.toFutureFinagleResponse
    }
    else if (runtimeClassEq[ResponseType, Future[_]]) {
      request: Request =>
        requestCallback(request).asInstanceOf[Future[_]] map createHttpResponse
    }
    else if (runtimeClassEq[ResponseType, StreamingResponse[_]]) {
      request: Request =>
        requestCallback(request).asInstanceOf[StreamingResponse[_]].toFutureFinagleResponse
    }
    else if (manifest[ResponseType] == manifest[Response]) {
      request: Request =>
        Future(
          requestCallback(request).asInstanceOf[Response])
    }
    else if (runtimeClassEq[ResponseType, Option[_]]) {
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

  private def runtimeClassEq[T: Manifest, U: Manifest]: Boolean = {
    manifest[T].runtimeClass == manifest[U].runtimeClass
  }

  private def isFutureOption[T: Manifest]: Boolean = {
    val typeArgs = manifest[T].typeArguments
    runtimeClassEq[T, Future[_]] &&
      typeArgs.size == 1 &&
      typeArgs.head.runtimeClass == classOf[Option[_]]
  }
}
