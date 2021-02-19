package com.twitter.inject.utils

import com.twitter.finagle.Backoff._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.util.{Duration, Throw, Try}
import scala.util.control.NonFatal

object RetryPolicyUtils {

  val NonFatalExceptions: PartialFunction[Try[Any], Boolean] = {
    case Throw(NonFatal(_)) => true
  }

  def exponentialRetry[T](
    start: Duration,
    multiplier: Int,
    numRetries: Int,
    shouldRetry: PartialFunction[Try[T], Boolean]
  ): RetryPolicy[Try[T]] = {

    backoff(exponential(start, multiplier) take numRetries)(shouldRetry)
  }

  def constantRetry[T](
    start: Duration,
    numRetries: Int,
    shouldRetry: PartialFunction[Try[T], Boolean]
  ): RetryPolicy[Try[T]] = {

    backoff(constant(start) take numRetries)(shouldRetry)
  }
}
