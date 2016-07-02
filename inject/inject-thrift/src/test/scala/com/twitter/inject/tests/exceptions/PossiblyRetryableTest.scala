package com.twitter.inject.tests.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{BackupRequestLost, CancelledConnectionException, CancelledRequestException, Failure}
import com.twitter.inject.Test
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.scrooge.{ThriftException, ThriftResponse}
import com.twitter.util.{Throw, Return}

class PossiblyRetryableTest extends Test {

  object PossiblyRetryableException extends ThriftException
  object NonRetryableException extends ThriftException with com.twitter.inject.exceptions.NonRetryableException

  case class TestResult(exceptions: Seq[Option[ThriftException]]) extends ThriftResponse[Unit] {
    def successField: Option[Unit] = None
    def exceptionFields: Iterable[Option[ThriftException]] = exceptions
  }

  "test isCancellation" in {
    assertIsCancellation(BackupRequestLost)
    assertIsCancellation(new CancelledRequestException)
    assertIsCancellation(new CancelledConnectionException(new Exception("cause")))
    assertIsCancellation(new ClientDiscardedRequestException("cause"))
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
      Return(
        TestResult(
          Seq(Some(NonRetryableException))))) should be(false)

    // We only ever look at the first exception, See: com.twitter.finagle.thrift.ThriftServiceIface#resultFilter
    PossiblyRetryable.PossiblyRetryableExceptions(
      Return(
        TestResult(
          Seq(
            Some(NonRetryableException),
            Some(PossiblyRetryableException))))) should be(false)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Return(TestResult(Seq(None)))) should be(false)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Return(TestResult(Seq(None, None)))) should be(false)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Return(TestResult(Seq()))) should be(false)

    // cancellations shouldn't be retried
    PossiblyRetryable.PossiblyRetryableExceptions(
      Throw(new CancelledRequestException(new Exception("FORCED EXCEPTION")))) should be(false)

    /* Only possibly retryable if there exists a possibly retryable exception in the first position. */

    PossiblyRetryable.PossiblyRetryableExceptions(
      Return(
        TestResult(
          Seq(
            Some(PossiblyRetryableException),
            Some(NonRetryableException))))) should be(true)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Return(
        TestResult(
          Seq(Some(PossiblyRetryableException))))) should be(true)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Return(
        TestResult(
          Seq(
            Some(PossiblyRetryableException),
            Some(PossiblyRetryableException))))) should be(true)

    PossiblyRetryable.PossiblyRetryableExceptions(
      Throw(new Exception("FORCED EXCEPTION"))) should be(true)
  }

  private def assertIsCancellation(e: Throwable) {
    PossiblyRetryable.isCancellation(e) should be(true)
  }
}
