package com.twitter.inject.server

import com.twitter.finagle.httpx.{Request, Response, Status, Version}
import com.twitter.finagle.netty3.BufChannelBuffer
import com.twitter.finagle.{Service, httpx}
import com.twitter.io.Buf
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http._

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
    version: Version = Version.Http11,
    status: Status = Status.Ok,
    headers: Iterable[(String, Object)] = Seq(),
    contentType: String,
    content: Buf
  ): Future[Response] = {
    val response = Response(version, status)
    response.content = content
    for ((k, v) <- headers) response.headerMap.add(k, v.toString)
    response.headerMap.add("Content-Language", "en")
    response.headerMap.add("Content-Length", content.length.toString)
    response.headerMap.add("Content-Type", contentType)
    Future.value(response)
  }
}

