package com.twitter.inject.tests.exceptions

import com.twitter.finagle.CancelledConnectionException
import com.twitter.finagle.CancelledRequestException
import com.twitter.finagle.Failure
import com.twitter.finagle.FailureFlags
import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.inject.Test
import com.twitter.inject.utils.ExceptionUtils

class ExceptionUtilsTest extends Test {

  test("test isCancellation") {
    assertIsCancellation(new CancelledRequestException)
    assertIsCancellation(new CancelledConnectionException(new Exception("cause")))
    assertIsCancellation(new ClientDiscardedRequestException("cause"))
    assertIsCancellation(Failure("int", FailureFlags.Interrupted))
    assertIsCancellation(Failure.rejected("", new CancelledRequestException))
  }

  test("test isNonRetryable") {
    assertIsNonRetryable(Failure("int", FailureFlags.Ignorable))
  }

  private def assertIsCancellation(e: Throwable): Unit = {
    ExceptionUtils.isCancellation(e) should be(true)
  }

  private def assertIsNonRetryable(e: Throwable): Unit = {
    ExceptionUtils.isNonRetryable(e) should be(true)
  }
}
