package com.twitter.finatra.tests.exceptions

import com.twitter.conversions.time._
import com.twitter.finagle._
import com.twitter.finatra.exceptions.ExternalServiceExceptionMatcher
import com.twitter.inject.Test
import java.io.IOException
import java.net.ConnectException
import org.apache.thrift.transport.TTransportException

class ExternalServiceExceptionMatcherTest extends Test {

  "match external exceptions" in {
    val externalExceptions = Seq(
      new RequestException(),
      new ApiException(),
      new ChannelWriteException(new Exception),
      new GlobalRequestTimeoutException(5.seconds),
      new com.twitter.util.TimeoutException("timeout"),
      new ChannelClosedException(),
      new InterruptedException(),
      new TTransportException(),
      new ConnectException())

    externalExceptions forall ExternalServiceExceptionMatcher.apply should equal(true)
  }

  "not match non-external exceptions" in {
    val nonExternalExceptions = Seq(
      new RuntimeException(),
      new IOException(),
      new NullPointerException)

    nonExternalExceptions forall ExternalServiceExceptionMatcher.apply should equal(false)
  }

}
