package com.twitter.finatra.tests.exceptions

import com.twitter.conversions.time._
import com.twitter.finagle._
import com.twitter.finatra.exceptions.ExternalServiceExceptionMatcher
import com.twitter.inject.Test
import com.twitter.util.Duration
import java.io.IOException
import java.net.ConnectException
import org.apache.thrift.transport.TTransportException

class ExternalServiceExceptionMatcherTest extends Test {

  "match external exceptions" in {
    val externalExceptions: Seq[Exception] = Seq(
      new RequestException(),
      new ApiException(),
      new ChannelWriteException(new Exception),
      new GlobalRequestTimeoutException(5.seconds),
      new com.twitter.util.TimeoutException("timeout"),
      new ChannelClosedException(),
      new InterruptedException(),
      new TTransportException(),
      new ConnectException(),
      new TimeoutException() {
        override protected val timeout: Duration = 5.seconds
        override protected def explanation: String = "timeout!"
      })

    externalExceptions forall ExternalServiceExceptionMatcher.apply should equal(true)

    externalExceptions forall { e =>
      ExternalServiceExceptionMatcher.unapply(e).isDefined
    } should equal(true)
  }

  "not match non-external exceptions" in {
    val nonExternalExceptions = Seq(
      new RuntimeException(),
      new IOException(),
      new NullPointerException)

    nonExternalExceptions forall ExternalServiceExceptionMatcher.apply should equal(false)

    nonExternalExceptions forall { e =>
      ExternalServiceExceptionMatcher.unapply(e).isDefined
    } should equal(false)
  }
}
