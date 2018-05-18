package com.twitter.finatra.http.exceptions

import scala.util.control.NoStackTrace

class HttpNackException(val retryable: Boolean)
    extends Exception
    with NoStackTrace {

  override def getMessage: String = s"HttpNackException(retryable = $retryable)"
}
