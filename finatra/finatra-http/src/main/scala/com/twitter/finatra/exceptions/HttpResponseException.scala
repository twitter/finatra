package com.twitter.finatra.exceptions

import com.twitter.finagle.http.Response

class HttpResponseException(
  val response: Response)
  extends Exception {

  override def getMessage: String = {
    response.toString
  }
}
