package com.twitter.finatra.httpclient

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.RetryUtils
import com.twitter.inject.Logging
import com.twitter.util.{Future, Try}
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpMethod

// Barebones HTTP client
// TODO: Add additional HTTP methods
class HttpClient(
  hostname: String = "",
  httpService: Service[Request, Response],
  retryPolicy: Option[RetryPolicy[Try[Response]]] = None,
  authorizationHeaderValue: Option[String] = None,
  mapper: FinatraObjectMapper)
  extends Logging {

  /* Public */

  def get(uri: String, headers: Seq[(String, String)] = Seq()): Future[Response] = {
    val request = createRequest(Method.Get, uri)
    setHeaders(request, headers)

    sendRequest(request)
  }

  def post(uri: String, channelBuffer: ChannelBuffer, contentLength: Long, contentType: String, headers: (String, String)*): Future[Response] = {
    val request = createRequest(Method.Post, uri)
    request.setContent(channelBuffer)
    setHeaders(request, headers)
    request.headers().set(CONTENT_LENGTH, contentLength.toString)
    request.headers().set(CONTENT_TYPE, contentType)

    sendRequest(request)
  }

  def head(uri: String): Future[Response] = {
    sendRequest(
      createRequest(Method.Head, uri))
  }

  def sendRequest(request: Request): Future[Response] = {
    debug(request + " with headers: " + request.headerMap.mkString(", "))

    retryPolicy match {
      case Some(policy) =>
        RetryUtils.retryFuture(policy) {
          httpService.apply(request)
        }
      case _ =>
        httpService(request)
    }
  }

  /* Private */

  private def setHeaders(request: Request, headers: Seq[(String, String)]) {
    for ((key, value) <- headers) {
      request.headers().set(key, value)
    }
    for (value <- authorizationHeaderValue) {
      request.headers().set("Authorization", "Bearer " + value)
    }
  }

  private def createRequest(method: HttpMethod, uri: String): Request = {
    val request = Request(method, uri)
    if (!hostname.isEmpty) {
      request.headers().set("Host", hostname)
    }
    request
  }
}
