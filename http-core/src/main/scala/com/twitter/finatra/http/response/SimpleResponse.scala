package com.twitter.finatra.http.response

import com.twitter.finagle.http.{Response, Status, Version}

object SimpleResponse {
  def apply(status: Status, body: String = ""): Response = {
    val response = Response(Version.Http11, status)
    response.setContentString(body)
    response
  }
}
