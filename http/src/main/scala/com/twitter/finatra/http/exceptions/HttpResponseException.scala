package com.twitter.finatra.http.exceptions

import com.twitter.finagle.http.Response
import com.twitter.inject.exceptions.NonRetryableException
import scala.util.control.NoStackTrace

class HttpResponseException(
  val response: Response)
  extends Exception
  with NonRetryableException
  with NoStackTrace {

  override def getMessage: String = {
    s"HttpResponseException with response $response"
  }
}
