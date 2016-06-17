package com.twitter.inject.tests.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{CancelledConnectionException, CancelledRequestException, Failure}
import com.twitter.inject.Test
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.scrooge.{ThriftException, ThriftResponse}
import com.twitter.util.Return

class PossiblyRetryableTest extends Test {

  "test NonCancelledExceptions matcher" in {
    assertIsCancellation(new CancelledRequestException)
    assertIsCancellation(new CancelledConnectionException(new Exception("cause")))
    assertIsCancellation(new ClientDiscardedRequestException("cause"))
    assertIsCancellation(Failure("int", Failure.Interrupted))
    assertIsCancellation(Failure.rejected("", new CancelledRequestException))
  }

  "test PossiblyRetryableExceptions correctly identifies possibly retryable thrift exception responses" in {
    object PossiblyRetryableException extends ThriftException
    object NonRetryableException extends ThriftException with com.twitter.inject.exceptions.NonRetryableException

    case class TestResult(exceptions: Seq[Option[ThriftException]]) extends ThriftResponse[Unit] {
      def successField: Option[Unit] = None
      def exceptionFields: Iterable[Option[ThriftException]] = exceptions
    }

    /* The successField = None is a possibility on void methods. We only want to possibly retry
       if the success field is empty AND there exists a possibly retryable exception in the first
       position in the returned Seq of exceptions. */

    PossiblyRetryable.PossiblyRetryableExceptions(Return(TestResult(
      exceptions = Seq(Some(NonRetryableException))
    ))) shouldBe false

    // We only ever look at the first exception, See: com.twitter.finagle.thrift.ThriftServiceIface#resultFilter
    PossiblyRetryable.PossiblyRetryableExceptions(Return(TestResult(
      exceptions = Seq(Some(NonRetryableException), Some(PossiblyRetryableException))
    ))) shouldBe false

    PossiblyRetryable.PossiblyRetryableExceptions(Return(TestResult(
      exceptions = Seq(None)
    ))) shouldBe false

    PossiblyRetryable.PossiblyRetryableExceptions(Return(TestResult(
      exceptions = Seq(None, None)
    ))) shouldBe false

    PossiblyRetryable.PossiblyRetryableExceptions(Return(TestResult(
      exceptions = Seq()
    ))) shouldBe false

    /* Only possibly retryable if there exists a possibly retryable exception in the first position. */

    PossiblyRetryable.PossiblyRetryableExceptions(Return(TestResult(
      exceptions = Seq(Some(PossiblyRetryableException), Some(NonRetryableException))
    ))) shouldBe true

    PossiblyRetryable.PossiblyRetryableExceptions(Return(TestResult(
      exceptions = Seq(Some(PossiblyRetryableException))
    ))) shouldBe true

    PossiblyRetryable.PossiblyRetryableExceptions(Return(TestResult(
      exceptions = Seq(Some(PossiblyRetryableException), Some(PossiblyRetryableException))
    ))) shouldBe true
  }

  private def assertIsCancellation(e: Throwable) {
    PossiblyRetryable.isCancellation(e) should be(true)
  }
}
