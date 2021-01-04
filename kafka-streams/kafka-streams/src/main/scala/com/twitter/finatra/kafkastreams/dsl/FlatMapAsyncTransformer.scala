package com.twitter.finatra.kafkastreams.dsl

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafkastreams.flushing.AsyncTransformer
import com.twitter.util.{Duration, Future}
import org.apache.kafka.streams.processor.internals.ProcessorRecordContext

private[dsl] class FlatMapAsyncTransformer[K1, V1, K2, V2](
  func: (K1, V1) => Future[Iterable[(K2, V2)]],
  statsReceiver: StatsReceiver,
  commitInterval: Duration,
  maxOutstandingFutures: Int)
    extends AsyncTransformer[K1, V1, K2, V2](
      statsReceiver = statsReceiver,
      maxOutstandingFuturesPerTask = maxOutstandingFutures,
      flushAsyncRecordsInterval = 1.second,
      commitInterval = commitInterval,
      flushTimeout = commitInterval
    ) {
  override protected def transformAsync(
    key: K1,
    value: V1,
    recordContext: ProcessorRecordContext
  ): Future[Iterable[(K2, V2, ProcessorRecordContext)]] = {
    func(key, value).map { iterable: Iterable[(K2, V2)] =>
      iterable.map {
        case (returnKey, returnValue) => (returnKey, returnValue, recordContext)
      }
    }
  }
}
