package com.twitter.finatra.httpclient

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.logging.Timing
import com.twitter.finatra.utils.{Logging, RetryUtils}
import com.twitter.util.{Await, Future, TimeoutException, Try}
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpMethod
import org.joda.time.Duration
import scala.collection.JavaConverters._

object HttpClient {
  private val EmptyBuffer = ChannelBuffers.wrappedBuffer(Array[Byte]())
}

class HttpClient(
  hostname: String = "",
  httpService: Service[Request, Response],
  retryPolicy: Option[RetryPolicy[Try[Response]]] = None,
  authorizationHeaderValue: Option[String] = None,
  mapper: FinatraObjectMapper)
  extends Logging
  with Timing {

  /* Public */

  def get(uri: String, headers: Seq[(String, String)] = Seq()): Future[Response] = {
    val request = getRequest(uri)
    setHeaders(request, headers)
    debugOutput(request)
    handleRequest(request)
  }

  def createUrl(uri: String): String = {
    hostname + uri
  }

  def post(uri: String, channelBuffer: ChannelBuffer, contentLength: Long, contentType: String, headers: (String, String)*): Future[Response] = {
    val request = postRequest(uri)
    request.setContent(channelBuffer)

    setHeaders(request, headers)
    request.headers().set(CONTENT_LENGTH, contentLength.toString)
    request.headers().set(CONTENT_TYPE, contentType)

    handleRequest(request)
  }

  def postString(uri: String, body: String, contentType: String, headers: (String, String)*): Future[Response] = {
    val channelBuffer = ChannelBuffers.wrappedBuffer(body.getBytes("UTF-8"))
    post(uri, channelBuffer, body.length, contentType, headers: _*)
  }

  def head(uri: String): Future[Response] = {
    val request = headRequest(uri)
    handleRequest(request)
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

  private def postRequest(uri: String) = {
    createRequest(Method.Post, uri)
  }

  private def getRequest(uri: String) = {
    createRequest(Method.Get, uri)
  }

  private def headRequest(uri: String) = {
    createRequest(Method.Head, uri)
  }

  private def createRequest(method: HttpMethod, uri: String): Request = {
    val request = Request(method, uri)
    if (!hostname.isEmpty) {
      request.headers().set("Host", hostname)
    }
    request
  }

  private def debugOutput(request: Request) {
    debug(request)
    debug(request.headers().asScala.mkString("\n"))
  }

  private def handleRequest(request: Request): Future[Response] = {
    debug("")
    debug("=" * 100)
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

  /* TODO: Wait methods below. Move out of finatra */

  def getAndWait(uri: String, timeout: Duration, headers: Seq[(String, String)] = Seq()): Response = {
    Await.result(
      get(uri, headers))
  }

  def getJsonAndWait[T: Manifest](uri: String, timeout: Duration, headers: Seq[(String, String)] = Seq()): T = {
    mapper.parse[T](
      getAndWait(uri, timeout, headers).contentString)
  }

  def postEmptyAndWait(uri: String, timeout: Duration, headers: (String, String)*): Response = {
    postAndWait(
      uri,
      HttpClient.EmptyBuffer,
      contentLength = 0,
      contentType = "",
      timeout = timeout,
      headers = headers: _*)
  }

  def postAndWait(uri: String, channelBuffer: ChannelBuffer, contentLength: Long, contentType: String, timeout: Duration, headers: (String, String)*): Response = {
    time("Posting to " + uri + " took %s ms") {
      try {
        val response = Await.result(
          post(uri, channelBuffer, contentLength, contentType, headers: _*),
          timeout.toTwitterDuration)

        debug(response)
        response
      } catch {
        case e: TimeoutException =>
          throw new TimeoutException(e.getMessage + " timeout posting to " + uri)
      }
    }
  }

  def postStringAndWait(uri: String, body: String, contentType: String, timeout: Duration, headers: (String, String)*): Response = {
    debug("Posting " + body)
    val channelBuffer = ChannelBuffers.wrappedBuffer(body.getBytes("UTF-8"))
    postAndWait(uri, channelBuffer, body.length, contentType, timeout, headers: _*)
  }

  def headAndWait(uri: String, timeout: Duration): Response = {
    try {
      Await.result(
        head(uri),
        timeout.toTwitterDuration)
    } catch {
      case e: TimeoutException =>
        throw new TimeoutException(e.getMessage + " timeout performing head to " + uri)
    }
  }
}
