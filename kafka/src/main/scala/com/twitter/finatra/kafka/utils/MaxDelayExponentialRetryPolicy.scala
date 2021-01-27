package com.twitter.finatra.kafka.utils

import com.twitter.finagle.service.RetryPolicy
import com.twitter.util.{Duration, Future, Throw, Try}
import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.utils.MaxDelayExponentialRetryPolicy.alwaysFalse
import org.apache.kafka.clients.producer.RecordMetadata

/** Retry policy that will have an exponential backoff but will
 * stop retrying after the cumulative delay reaches `maxDelay`
 *
 * todo: this is temporary until `TakeWhile` is added to `BackoffStrategy`
 *
 * @param maxDelay the maximum amount of cumulative delay before stopping retries
 * @param prevDelay the delay of the previous policy
 * @param cumulativeDelay the cumulative delay of all tries. This only includes artificially
 *                        added delay, does not include the runtime.
 * @param initialDelay the initial delay
 * @param multiplier the multiplier to increase the delay by each time
 */
private[kafka] class MaxDelayExponentialRetryPolicy(
  maxDelay: Duration,
  retryableExceptions: PartialFunction[Throwable, Boolean],
  prevDelay: Duration = Duration.Zero,
  cumulativeDelay: Duration = Duration.Zero,
  initialDelay: Duration = 1.millisecond,
  multiplier: Int = 2)
    extends RetryPolicy[Try[Future[RecordMetadata]]] {
  override def apply(
    v: Try[Future[RecordMetadata]]
  ): Option[(Duration, RetryPolicy[Try[Future[RecordMetadata]]])] = {
    val shouldRetry = v match {
      case _ if cumulativeDelay >= maxDelay =>
        false // already delayed too long, no more retries
      case Throw(t) => retryableExceptions.applyOrElse(t, alwaysFalse)
      case _ => false
    }
    if (shouldRetry) {
      // handle the base case of 0 by jumping to `initialDelay`, otherwise apply the `multiplier`
      val nextDelay = if (prevDelay == Duration.Zero) initialDelay else prevDelay * multiplier
      val nextPolicy =
        new MaxDelayExponentialRetryPolicy(
          maxDelay,
          retryableExceptions,
          nextDelay,
          cumulativeDelay + nextDelay, // `apply` is called AFTER `nextDelay` is occurs so add it here
          initialDelay,
          multiplier
        )
      Some((nextDelay, nextPolicy))
    } else {
      None
    }
  }
}

private object MaxDelayExponentialRetryPolicy {
  // avoids recreating the lambda each time
  val alwaysFalse: Any => Boolean = (_: Any) => false
}
