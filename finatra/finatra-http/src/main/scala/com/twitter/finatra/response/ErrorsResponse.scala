package com.twitter.finatra.response

object ErrorsResponse {
  def apply(error: String): ErrorsResponse = {
    ErrorsResponse(Seq(error))
  }
}

case class ErrorsResponse(
  errors: Seq[String])
