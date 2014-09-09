package com.twitter.finatra.marshalling

import com.google.inject.Injector
import com.twitter.finagle.http.{Response, Request => FinagleRequest}
import com.twitter.finatra.Request
import com.twitter.finatra.conversions.httpfuture._
import com.twitter.finatra.conversions.httpoption._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.response._
import com.twitter.util.Future
import javax.inject.Inject

class CallbackConvertor @Inject()(
  injector: Injector,
  objectMapper: FinatraObjectMapper,
  messageBodyManager: MessageBodyManager,
  responseBuilder: ResponseBuilder) {

  /* Public */

  def convertToFutureResponse[RequestType: Manifest, ResponseType: Manifest, U](callback: RequestType => ResponseType): Request => Future[Response] = {
    val requestConvertedCallback: (Request => ResponseType) = createRequestCallback[RequestType, ResponseType](callback)
    createResponseCallback[ResponseType, U](requestConvertedCallback)
  }

  /* Private */

  private def createRequestCallback[RequestType: Manifest, ResponseType: Manifest](callback: (RequestType) => ResponseType): (Request) => ResponseType = {
    manifest[RequestType] match {

      //If the callback had a Finatra Request as an input, directly return it
      case request if request <:< classManifest[Request] =>
        callback.asInstanceOf[(Request => ResponseType)]

      //If the callback had a Finagle Request as an input, grab it from the Finatra Request
      case finagleRequest if finagleRequest <:< classManifest[FinagleRequest] =>
        request: Request =>
          callback.asInstanceOf[(FinagleRequest => ResponseType)](request.request)

      //Otherwise convert our Request into the callback's input type
      case _ =>
        request: Request =>
          val callbackInput = messageBodyManager.parse[RequestType](request)
          callback(callbackInput)
    }
  }

  private def createResponseCallback[ResponseType: Manifest, U](requestCallback: (Request) => ResponseType): (Request) => Future[Response] = {
    if (isFutureResponse[ResponseType]) {
      requestCallback.asInstanceOf[(Request => Future[Response])]
    }
    else if (isFutureOption[ResponseType]) {
      request: Request =>
        requestCallback(request).asInstanceOf[Future[Option[U]]].valueOrNotFound("") map createResponse
    }
    else if (isFuture[ResponseType]) {
      request: Request =>
        requestCallback(request).asInstanceOf[Future[U]] map createResponse
    }
    else if (isResponse[ResponseType]) {
      request: Request =>
        Future(requestCallback(request).asInstanceOf[Response])
    }
    else if (isOption[ResponseType]) {
      request: Request =>
        Future(
          createResponse(
            requestCallback(request).asInstanceOf[Option[_]].valueOrNotFound()))
    }
    else {
      request: Request =>
        Future(
          createResponse(
            requestCallback(request)))
    }
  }

  private def createResponse(any: Any): Response = {
    val writer = messageBodyManager.writerOrDefault(any)
    val writerResponse = writer.write(any)
    responseBuilder.ok.
      body(writerResponse.body).
      headers(writerResponse.headers)
  }

  private def isFutureResponse[T: Manifest]: Boolean = {
    manifest[T] <:< classManifest[Future[Response]]
  }

  private def isResponse[T: Manifest]: Boolean = {
    manifest[T] <:< classManifest[Response]
  }

  private def isFutureOption[T: Manifest]: Boolean = {
    isFuture[T] && hasOptionTypeParam
  }

  private def isOption[T: Manifest] = {
    manifest[T].erasure == classOf[Option[_]]
  }

  private def hasOptionTypeParam[T: Manifest] = {
    val typeArgs = manifest[T].typeArguments
    typeArgs.size == 1 &&
      typeArgs.head.erasure == classOf[Option[_]]
  }

  private def isFuture[T: Manifest]: Boolean = {
    manifest[T].erasure == classOf[Future[_]]
  }
}
