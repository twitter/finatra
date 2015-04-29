package com.twitter.finatra.http.response

import com.twitter.finagle.http.{Response, Version}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponseStatus}

object SimpleResponse {
  def apply(status: HttpResponseStatus, body: String = "") = {
    val response = new SimpleResponse(status)
    response.setContentString(body)
    response
  }
}

class SimpleResponse(
  override val status: HttpResponseStatus)
  extends Response {

  override val httpResponse = Response(
    new DefaultHttpResponse(
      Version.Http11,
      status))
}
