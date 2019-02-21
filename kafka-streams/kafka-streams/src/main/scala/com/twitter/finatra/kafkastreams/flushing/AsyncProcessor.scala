package com.twitter.finatra.kafkastreams.flushing

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafkastreams.utils.MessageTimestamp
import com.twitter.util.{Duration, Future}

abstract class AsyncProcessor[K, V](
  override val statsReceiver: StatsReceiver,
  override val maxOutstandingFuturesPerTask: Int,
  override val commitInterval: Duration,
  override val flushTimeout: Duration)
  extends FlushingProcessor[K, V]
    with AsyncFlushing[K, V, Unit, Unit] {

  protected def processAsync(key: K, value: V, timestamp: MessageTimestamp): Future[Unit]

  override final def process(key: K, value: V): Unit = {
    val processAsyncResult =
      processAsync(key = key, value = value, timestamp = processorContext.timestamp())

    addFuture(key = key, value = value, future = processAsyncResult.map(_ => Iterable()))
  }
}
