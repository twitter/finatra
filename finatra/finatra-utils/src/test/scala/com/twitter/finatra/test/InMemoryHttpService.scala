package com.twitter.finatra.test

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.conversions.strings._
import com.twitter.finatra.test.Banner._
import com.twitter.finatra.utils.{Clearable, Logging}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpMethod._
import scala.collection._
import scala.collection.mutable.ArrayBuffer

class InMemoryHttpService
  extends Service[Request, Response]
  with Clearable
  with Logging {

  private val responseMap = mutable.Map[RequestKey, ArrayBuffer[ResponseWithExpectedBody]]().withDefaultValue(ArrayBuffer())
  val recordedRequests = ArrayBuffer[Request]()
  var overrideResponse: Option[Response] = None

  /* Service Apply */

  def apply(request: Request): Future[Response] = synchronized {
    recordedRequests += request
    Future {
      overrideResponse getOrElse lookupResponse(request)
    }
  }

  /* Mock Support */

  def mockGet(path: String, andReturn: Response, sticky: Boolean = false) {
    val existing = responseMap(RequestKey(POST, path))
    val newEntry = ResponseWithExpectedBody(andReturn, None, sticky = sticky)
    responseMap(
      RequestKey(GET, path)) = existing :+ newEntry
  }

  def mockPost(path: String, withBody: String = null, andReturn: Response, sticky: Boolean = false) {
    val existing = responseMap(RequestKey(POST, path))
    val newEntry = ResponseWithExpectedBody(andReturn, withBody.toOption, sticky = sticky)
    responseMap(
      RequestKey(POST, path)) = existing :+ newEntry
  }

  override def clear() {
    responseMap.clear()
    recordedRequests.clear()
    overrideResponse = None
  }

  def printRequests() {
    banner("Requests")
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

    if (request.method == HttpMethod.POST && hasExpectedBodies(existing))
      lookupPostResponseWithBody(request, existing)
    else if (existing.head.sticky)
      existing.head.response
    else
      existing.remove(0).response
  }

  private def hasExpectedBodies(existing: ArrayBuffer[ResponseWithExpectedBody]): Boolean = {
    existing exists {_.expectedPostBody.isDefined}
  }

  private def lookupPostResponseWithBody(request: Request, existing: ArrayBuffer[ResponseWithExpectedBody]): Response = {
    val found = existing find {_.expectedPostBody == Some(request.contentString)} getOrElse {
      throw new Exception(request + " with expected body not mocked")
    }

    if (!found.sticky) {
      existing -= found
    }

    found.response
  }
}

case class RequestKey(
  method: HttpMethod,
  path: String)

case class ResponseWithExpectedBody(
  response: Response,
  expectedPostBody: Option[String],
  sticky: Boolean)