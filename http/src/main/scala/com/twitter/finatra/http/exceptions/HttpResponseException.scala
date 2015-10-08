package com.twitter.finatra.http.exceptions

import com.twitter.finagle.http.Response

class HttpResponseException(
  val response: Response)
  extends Exception {

  override def getMessage: String = {
    s"HttpResponseException with response $response"
  }
}
