package com.twitter.inject.thrift.internal

import com.github.nscala_time.time
import com.twitter.finagle._
import com.twitter.finagle.service.Backoff._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.util.{Duration => TwitterDuration, TimeoutException => UtilTimeoutException, NonFatal, Throw, Try}
import org.joda.time.Duration

trait RetryUtils extends time.Implicits {

  val MaxDuration = Duration.millis(Long.MaxValue)

  val WriteExceptionsOnly: PartialFunction[Try[FinatraThriftClientRequest], Boolean] = {
    case Throw(RetryableWriteException(_)) => true
  }

  val NonFatalExceptions: PartialFunction[Try[FinatraThriftClientRequest], Boolean] = {
    case Throw(NonFatal(_)) =>
      true
  }

  val TimeoutAndWriteExceptionsOnly: PartialFunction[Try[FinatraThriftClientRequest], Boolean] = WriteExceptionsOnly orElse {
    case Throw(Failure(Some(_: TimeoutException))) => true
    case Throw(Failure(Some(_: UtilTimeoutException))) => true
    case Throw(_: TimeoutException) => true
    case Throw(_: UtilTimeoutException) => true
  }

  implicit class RichDuration(duration: Duration) {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        duration.getMillis)
    }
  }

  val Never: RetryPolicy[Try[_]] = new RetryPolicy[Try[_]] {
    def apply(t: Try[_]) = None
  }

  protected def exponentialRetry[T](
    start: Duration,
    multiplier: Int,
    numRetries: Int,
    shouldRetry: PartialFunction[Try[Nothing], Boolean]): RetryPolicy[Try[Nothing]] = {

    backoff(
      exponential(start.toTwitterDuration, multiplier) take numRetries)(shouldRetry)
  }

  protected def constantRetry[T](
    start: Duration,
    numRetries: Int,
    shouldRetry: PartialFunction[Try[Nothing], Boolean]): RetryPolicy[Try[Nothing]] = {

    backoff(
      constant(start.toTwitterDuration) take numRetries)(shouldRetry)
  }
}
