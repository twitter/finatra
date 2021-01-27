package com.twitter.finatra.kafka.utils

import com.twitter.inject.Test
import com.twitter.conversions.DurationOps._
import com.twitter.util.{Future, Throw, Try}
import org.apache.kafka.clients.producer.RecordMetadata

class MaxDelayExponentialRetryPolicyTest extends Test {

  class Retryable extends Exception()
  class NonRetryable extends Exception()
  val retryable: Try[Future[RecordMetadata]] = Throw(new Retryable)
  val nonRetryable: Try[Future[RecordMetadata]] = Throw(new NonRetryable)

  val retryablePf: PartialFunction[Throwable, Boolean] = { case _: Retryable => true }

  test("Retries specific failures") {
    val policy = new MaxDelayExponentialRetryPolicy(
      maxDelay = 1.second,
      retryableExceptions = retryablePf
    )
    assert(policy(retryable).nonEmpty)
  }

  test("Doesn't retry other failures") {
    val implicitPolicy = new MaxDelayExponentialRetryPolicy(
      maxDelay = 1.second,
      retryableExceptions = retryablePf
    )
    assert(implicitPolicy(nonRetryable).isEmpty)

    val explicitPolicy = new MaxDelayExponentialRetryPolicy(
      maxDelay = 1.second,
      {
        case _: Retryable => true
        case _: NonRetryable => false
      }
    )
    assert(explicitPolicy(nonRetryable).isEmpty)
  }

  test("Stops retrying after the cumulative delay is met") {
    val policy = new MaxDelayExponentialRetryPolicy(
      maxDelay = 1.second,
      retryableExceptions = retryablePf,
      cumulativeDelay = 1.second
    )
    assert(policy(retryable).isEmpty)
  }

  test("Exponentially backs off") {
    val policy = new MaxDelayExponentialRetryPolicy(
      maxDelay = 10.milliseconds,
      retryableExceptions = retryablePf
    )

    val Some((backoffs, lastPolicy)) = for {
      (delay1, next) <- policy(retryable)
      (delay2, next) <- next(retryable)
      (delay4, next) <- next(retryable)
      (delay8, next) <- next(retryable)
    } yield (Seq(delay1, delay2, delay4, delay8), next)

    assert(backoffs == Seq(1.millisecond, 2.milliseconds, 4.milliseconds, 8.milliseconds))
    assert(lastPolicy(retryable).isEmpty)
  }
}
