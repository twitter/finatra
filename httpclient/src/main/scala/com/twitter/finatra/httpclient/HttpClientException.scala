package com.twitter.finatra.httpclient

import scala.util.control.NoStackTrace
import com.twitter.finagle.http.Status

class HttpClientException(
  val status: Status,
  msg: String)
  extends Exception(msg)
  with NoStackTrace
