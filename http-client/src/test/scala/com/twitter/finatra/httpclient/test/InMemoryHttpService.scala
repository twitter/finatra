package com.twitter.finatra.httpclient.test

import com.twitter.finagle.Service
import com.twitter.finagle.http.Method
import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.inject.Injector
import com.twitter.inject.app.Banner
import com.twitter.util.Future
import java.lang.annotation.Annotation
import scala.collection._
import scala.collection.mutable.ArrayBuffer

object InMemoryHttpService {
  def fromInjector[Ann <: Annotation: Manifest](injector: Injector): InMemoryHttpService = {
    injector.instance[Service[Request, Response], Ann].asInstanceOf[InMemoryHttpService]
  }
}

class InMemoryHttpService extends Service[Request, Response] {

  private[this] val responseMap =
    mutable.Map[RequestKey, ArrayBuffer[ResponseWithExpectedBody]]().withDefaultValue(ArrayBuffer())
  val recordedRequests: ArrayBuffer[Request] = ArrayBuffer[Request]()
  var overrideResponse: Option[Response] = None

  /* Service Apply */

  def apply(request: Request): Future[Response] = synchronized {
    recordedRequests += request
    Future(overrideResponse.getOrElse(lookupResponse(request)))
  }

  /* Mock Support */

  def mockGet(path: String, andReturn: Response, sticky: Boolean = false): Unit = {
    mock(Get, path, andReturn, sticky)
  }

  def mockPost(
    path: String,
    withBody: String = null,
    andReturn: Response,
    sticky: Boolean = false
  ): Unit = {
    mock(Post, path, andReturn, sticky, Option(withBody))
  }

  def mockPut(
    path: String,
    withBody: String = null,
    andReturn: Response,
    sticky: Boolean = false
  ): Unit = {
    mock(Put, path, andReturn, sticky, Option(withBody))
  }

  def mock(
    method: Method,
    path: String,
    andReturn: Response,
    sticky: Boolean,
    withBody: Option[String] = None
  ): Unit = {
    val existing = responseMap(RequestKey(method, path))
    val newEntry = ResponseWithExpectedBody(andReturn, withBody, sticky = sticky)
    responseMap(RequestKey(method, path)) = existing :+ newEntry
  }

  @deprecated("Use clear()", "2020-07-22")
  def reset(): Unit = clear()

  def clear(): Unit = {
    responseMap.clear()
    recordedRequests.clear()
    overrideResponse = None
  }

  def printRequests(): Unit = {
    Banner.banner("Requests")
    for (request <- recordedRequests) {
      println(request + " " + request.contentString)
    }
  }

  /* Private */

  private def lookupResponse(request: Request): Response = {
    val key = RequestKey(request.method, request.uri)
    val existing = responseMap(key)
    if (existing.isEmpty) {
      throw new Exception(key + " not mocked in\n" + responseMap.mkString("\n"))
    }

    if (request.method != Method.Get && hasExpectedBodies(existing))
      lookupPostResponseWithBody(request, existing)
    else if (existing.head.sticky)
      existing.head.response
    else
      existing.remove(0).response
  }

  private def hasExpectedBodies(existing: ArrayBuffer[ResponseWithExpectedBody]): Boolean = {
    existing.exists(_.expectedBody.isDefined)
  }

  private def lookupPostResponseWithBody(
    request: Request,
    existing: ArrayBuffer[ResponseWithExpectedBody]
  ): Response = {
    val found = existing.find(_.expectedBody.contains(request.contentString)).getOrElse {
      throw new PostRequestWithIncorrectBodyException(request + " with expected body not mocked")
    }

    if (!found.sticky) {
      existing -= found
    }

    found.response
  }

  case class RequestKey(method: Method, path: String)

  case class ResponseWithExpectedBody(
    response: Response,
    expectedBody: Option[String],
    sticky: Boolean)

}
