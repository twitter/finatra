package com.twitter.finatra.httpclient

import com.twitter.finagle.NoStacktrace
import org.jboss.netty.handler.codec.http.HttpResponseStatus

class HttpClientException(
  status: HttpResponseStatus,
  msg: String)
  extends Exception(msg)
  with NoStacktrace
