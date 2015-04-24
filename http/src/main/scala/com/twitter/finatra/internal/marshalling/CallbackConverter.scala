package com.twitter.finatra.internal.marshalling

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.bindings.CallbackConverterPool
import com.twitter.finatra.response.ResponseBuilder
import com.twitter.util.{Future, FuturePool}
import javax.inject.Inject

/*
 * Note: We are using deprecated classManifest and <:< to avoid potential threading issues
 * with 2.10 reflection methods. See http://docs.scala-lang.org/overviews/reflection/thread-safety.html
 */
class CallbackConverter @Inject()(
  @CallbackConverterPool pool: FuturePool,
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

      // The callback takes a finagle Request
      case finagleRequest if finagleRequest <:< classManifest[Request] =>
        callback.asInstanceOf[(Request => ResponseType)]

      // Otherwise convert Request into the callback's input type
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
        requestCallback(request).asInstanceOf[Future[Option[U]]] map optionToHttpResponse
    }
    else if (isFuture[ResponseType]) {
      request: Request =>
        requestCallback(request).asInstanceOf[Future[U]] map createHttpResponse
    }
    else if (isResponse[ResponseType]) {
      request: Request =>
        pool {
          requestCallback(request).asInstanceOf[Response]
        }
    }
    else if (isOption[ResponseType]) {
      request: Request =>
        pool {
          optionToHttpResponse(
            requestCallback(request).asInstanceOf[Option[_]])
        }
    }
    else {
      request: Request =>
        pool {
          createHttpResponse(
            requestCallback(request))
        }
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
