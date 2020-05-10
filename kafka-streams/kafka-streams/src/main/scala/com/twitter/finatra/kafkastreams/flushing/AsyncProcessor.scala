package com.twitter.finatra.kafkastreams.flushing

import com.twitter.finagle.stats.{Stat, StatsReceiver}
import com.twitter.finatra.kafkastreams.utils.MessageTimestamp
import com.twitter.util.{Duration, Future}

/**
 * @param statsReceiver Receiver for AsyncProcessor metrics. Scope the receiver to differentiate
 *                      metrics between different AsyncProcessors in the topology.
 */
abstract class AsyncProcessor[K, V](
  override val statsReceiver: StatsReceiver,
  override val maxOutstandingFuturesPerTask: Int,
  override val commitInterval: Duration,
  override val flushTimeout: Duration)
    extends FlushingProcessor[K, V]
    with AsyncFlushing[K, V, Unit, Unit] {

  private[this] val latencyStat = statsReceiver.stat("process_async_latency_ms")

  protected def processAsync(key: K, value: V, timestamp: MessageTimestamp): Future[Unit]

  override final def process(key: K, value: V): Unit = {
    val processAsyncResult =
      Stat.timeFuture(latencyStat)(
        processAsync(key = key, value = value, timestamp = processorContext.timestamp())
      )

    addFuture(key = key, value = value, future = processAsyncResult.map(_ => Iterable()))
  }
}
