package com.twitter.finatra.http.exceptions

import com.twitter.finagle.http.Response
import com.twitter.inject.exceptions.NotRetryableException

class HttpResponseException(
  val response: Response)
  extends Exception
  with NotRetryableException {

  override def getMessage: String = {
    s"HttpResponseException with response $response"
  }
}
