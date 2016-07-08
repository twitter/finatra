package com.twitter.finatra.http.internal.marshalling

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http._
import com.twitter.finatra.http.response.{ResponseBuilder, StreamingResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.util.Future
import javax.inject.Inject

private[http] class CallbackConverter @Inject()(
  messageBodyManager: MessageBodyManager,
  responseBuilder: ResponseBuilder,
  mapper: FinatraObjectMapper,
  jsonStreamParser: JsonStreamParser) {

  /* Public */

  // Note we use Manifest here as we support Scala 2.10 and reflection is not thread-safe in 2.10 (precluding the use
  // of typeTag and/or classTag). See: http://docs.scala-lang.org/overviews/reflection/thread-safety.html
  def convertToFutureResponse[RequestType: Manifest, ResponseType: Manifest](callback: RequestType => ResponseType): Request => Future[Response] = {
    val requestConvertedCallback: (Request => ResponseType) = createRequestCallback[RequestType, ResponseType](callback)
    createResponseCallback[ResponseType](requestConvertedCallback)
  }

  /* Private */

  private def createRequestCallback[RequestType: Manifest, ResponseType: Manifest](callback: RequestType => ResponseType): Request => ResponseType = {
    if (manifest[RequestType] == manifest[Request]) {
      callback.asInstanceOf[(Request => ResponseType)]
    }
    else if (runtimeClassEq[RequestType, AsyncStream[_]]) {
      val asyncStreamTypeParam = manifest[RequestType].typeArguments.head
      request: Request =>
        val asyncStream = jsonStreamParser.parseArray(request.reader)(asyncStreamTypeParam)
        callback(asyncStream.asInstanceOf[RequestType])
    }
    else if (runtimeClassEq[RequestType, Int]) {
      // NOTE: "empty" route callbacks that return a String are inferred as a RequestType of
      // Int by Scala because StringOps.apply is a function of Int => Char.
      throw new Exception(
        "Routes with empty (with no parameter) route callbacks or route callbacks with a parameter of type Int are not allowed. " +
          "Please specify a parameter in your route callback of the appropriate type.")
    }
    else {
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
        requestCallback(request).asInstanceOf[Future[Option[_]]].map(optionToHttpResponse(request))
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
        requestCallback(request).asInstanceOf[Future[_]].map(createHttpResponse(request))
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
          optionToHttpResponse(request)(requestCallback(request).asInstanceOf[Option[_]]))
    }
    else if (runtimeClassEq[ResponseType, String]) {
      // optimized
      request: Request =>
        val response = Response(Version.Http11, Status.Ok)
        response.setContentString(requestCallback(request).asInstanceOf[String])
        response.headerMap.add(Fields.ContentType, ResponseBuilder.PlainTextContentType)
        Future.value(response)
    }
    else {
      request: Request =>
        requestCallback(request) match {
          case futureResult: Future[_] => futureResult.map(createHttpResponse(request))
          case result => Future(createHttpResponse(request)(result))
        }
    }
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

  private def isFutureOption[T: Manifest]: Boolean = {
    val typeArgs = manifest[T].typeArguments
    runtimeClassEq[T, Future[_]] &&
      typeArgs.size == 1 &&
      typeArgs.head.runtimeClass == classOf[Option[_]]
  }
}
