package com.twitter.finatra.http.response

import com.twitter.finagle.httpx.Request

object ErrorsResponse {
  def apply(error: String): ErrorsResponse = {
    ErrorsResponse(Seq(error))
  }

  def apply(request: Request, throwable: Throwable, msg: String): ErrorsResponse = {
    if (request.path.startsWith("/admin")) {
      ErrorsResponse(throwable.getMessage)
     } else {
      ErrorsResponse(msg)
    }
  }
}

case class ErrorsResponse(
  errors: Seq[String])
