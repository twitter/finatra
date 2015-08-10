package com.twitter.finatra.http.response

import com.twitter.finagle.http.Response
import org.jboss.netty.handler.codec.http.HttpResponseStatus

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

  override val httpResponse = Response(status)
}
