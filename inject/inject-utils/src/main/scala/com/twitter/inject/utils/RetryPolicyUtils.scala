package com.twitter.inject.utils

import com.twitter.finagle.service.Backoff._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.inject.conversions.time._
import com.twitter.util.{Throw, Try}
import org.joda.time.Duration
import scala.util.control.NonFatal

object RetryPolicyUtils {

  val NonFatalExceptions: PartialFunction[Try[Any], Boolean] = {
    case Throw(NonFatal(_)) => true
  }

  def exponentialRetry[T](
    start: Duration,
    multiplier: Int,
    numRetries: Int,
    shouldRetry: PartialFunction[Try[T], Boolean]): RetryPolicy[Try[T]] = {

    backoff(
      exponential(start.toTwitterDuration, multiplier) take numRetries)(shouldRetry)
  }

  def constantRetry[T](
    start: Duration,
    numRetries: Int,
    shouldRetry: PartialFunction[Try[T], Boolean]): RetryPolicy[Try[T]] = {

    backoff(
      constant(start.toTwitterDuration) take numRetries)(shouldRetry)
  }
}
