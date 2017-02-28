package com.twitter.inject.tests.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{BackupRequestLost, CancelledConnectionException, CancelledRequestException, Failure}
import com.twitter.inject.WordSpecTest
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.scrooge.ThriftException
import com.twitter.util.{Return, Throw}

class PossiblyRetryableTest extends WordSpecTest {

  object PossiblyRetryableException extends ThriftException
  object NonRetryableException extends ThriftException with com.twitter.inject.exceptions.NonRetryableException

  "test isCancellation" in {
    assertIsCancellation(BackupRequestLost)
    assertIsCancellation(new CancelledRequestException)
    assertIsCancellation(new CancelledConnectionException(new Exception("cause")))
    assertIsCancellation(ClientDiscardedRequestException("cause"))
    assertIsCancellation(Failure("int", Failure.Interrupted))
    assertIsCancellation(Failure.rejected("", new CancelledRequestException))
  }

  "test apply" in {
    PossiblyRetryable(PossiblyRetryableException) should be(true)
    PossiblyRetryable(NonRetryableException) should be(false)
  }

  "test unapply" in {
    NonRetryableException match {
      case PossiblyRetryable(e) => fail("NonRetryableException should not be PossiblyRetryable")
      case _ => // nothing
    }

    PossiblyRetryableException match {
      case PossiblyRetryable(e) => // nothing
      case _ => fail("PossiblyRetryableException should be PossiblyRetryable")
    }
  }

  "test PossiblyRetryableExceptions correctly identifies possibly retryable thrift exception responses" in {
    /* The successField = None is a possibility on void methods. We only want to possibly retry
       if the success field is empty AND there exists a possibly retryable exception in the first
       position in the returned Seq of exceptions. */

    PossiblyRetryable.PossiblyRetryableExceptions(
      Throw(NonRetryableException)) should be(false)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Return.Unit) should be(false)

    // cancellations shouldn't be retried
    PossiblyRetryable.PossiblyRetryableExceptions(
      Throw(new CancelledRequestException(new Exception("FORCED EXCEPTION")))) should be(false)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Throw(PossiblyRetryableException)) should be(true)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Throw(new Exception("FORCED EXCEPTION"))) should be(true)
  }

  private def assertIsCancellation(e: Throwable) {
    PossiblyRetryable.isCancellation(e) should be(true)
  }
}
