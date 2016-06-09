package com.twitter.inject.tests.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{CancelledConnectionException, CancelledRequestException, Failure}
import com.twitter.inject.Test
import com.twitter.inject.exceptions.PossiblyRetryable

class PossiblyRetryableTest extends Test {

  "test NonCancelledExceptions matcher" in {
    assertIsCancellation(new CancelledRequestException)
    assertIsCancellation(new CancelledConnectionException(new Exception("cause")))
    assertIsCancellation(new ClientDiscardedRequestException("cause"))
    assertIsCancellation(Failure("int", Failure.Interrupted))
    assertIsCancellation(Failure.rejected("", new CancelledRequestException))
  }

  private def assertIsCancellation(e: Throwable) {
    PossiblyRetryable.isCancellation(e) should be(true)
  }

}
