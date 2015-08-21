package com.twitter.finatra.httpclient

import com.twitter.finagle.httpx.Status
import com.twitter.finagle.NoStacktrace

class HttpClientException(
  status: Status,
  msg: String)
  extends Exception(msg)
  with NoStacktrace
