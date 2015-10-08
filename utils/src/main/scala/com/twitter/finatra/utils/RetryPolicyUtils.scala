package com.twitter.finatra.utils

import com.twitter.finagle.http.Response
import com.twitter.finagle.service.Backoff._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.utils.ResponseUtils._
import com.twitter.util.{NonFatal, Return, Throw, Try}
import org.joda.time.Duration

object RetryPolicyUtils {

  val NonFatalExceptions: PartialFunction[Try[Any], Boolean] = {
    case Throw(NonFatal(_)) => true
  }

  val Http4xxOr5xxResponses: PartialFunction[Try[Response], Boolean] = {
    case Return(response) if is4xxOr5xxResponse(response) => true
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
