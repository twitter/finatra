package com.twitter.inject.server

import com.twitter.finagle.http.{Status, Version}
import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finagle.netty3.BufChannelBuffer
import com.twitter.finagle.{Service, httpx}
import com.twitter.io.Buf
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http._

/*
 * Copied from twitter-server 1.13.0
 * TODO: Remove once new version of twitter-server is open-source released
 */
class HttpReplyHandler(msg: String) extends Service[HttpRequest, HttpResponse] {
  def apply(req: HttpRequest) = newOk(msg)

  def newOk(msg: String): Future[HttpResponse] =
    newResponse(
      contentType = "text/plain;charset=UTF-8",
      content = Buf.Utf8(msg)
    )

  def newResponse(
    version: HttpVersion = Version.Http11,
    status: HttpResponseStatus = Status.Ok,
    headers: Iterable[(String, Object)] = Seq(),
    contentType: String,
    content: Buf
  ): Future[HttpResponse] = {
    val response = new DefaultHttpResponse(version, status)
    response.setContent(BufChannelBuffer(content))
    for ((k, v) <- headers) response.headers.set(k, v)
    response.headers.set(HttpHeaders.Names.CONTENT_LANGUAGE, "en")
    response.headers.set(HttpHeaders.Names.CONTENT_LENGTH, content.length)
    response.headers.set(HttpHeaders.Names.CONTENT_TYPE, contentType)
    Future.value(response)
  }
}

/*
 * Copied from twitter-server master
 * TODO: Remove once new version of twitter-server is open-source released
 */
class HttpxReplyHandler(msg: String) extends Service[Request, Response] {
  def apply(req: Request) = newOk(msg)

  private def newOk(msg: String): Future[Response] = {
    newResponse(
      contentType = "text/plain;charset=UTF-8",
      content = Buf.Utf8(msg)
    )
  }

  def newResponse(
    version: httpx.Version = httpx.Version.Http11,
    status: httpx.Status = httpx.Status.Ok,
    headers: Iterable[(String, Object)] = Seq(),
    contentType: String,
    content: Buf
  ): Future[Response] = {
    val response = httpx.Response(version, status)
    response.content = content
    for ((k, v) <- headers) response.headerMap.add(k, v.toString)
    response.headerMap.add("Content-Language", "en")
    response.headerMap.add("Content-Length", content.length.toString)
    response.headerMap.add("Content-Type", contentType)
    Future.value(response)
  }
}

