package com.twitter.finatra.http.response

import com.twitter.finagle.http.Request

object ErrorsResponse {
  def apply(error: String): ErrorsResponse = {
    ErrorsResponse(Seq(error))
  }

  @deprecated("Use apply(msg: String)", "now")
  def apply(request: Request, throwable: Throwable, msg: String): ErrorsResponse = {
    if (request.path.startsWith("/admin")) {
      ErrorsResponse(msg + ": " + throwable.getMessage)
     } else {
      ErrorsResponse(msg)
    }
  }
}

case class ErrorsResponse(
  errors: Seq[String])
