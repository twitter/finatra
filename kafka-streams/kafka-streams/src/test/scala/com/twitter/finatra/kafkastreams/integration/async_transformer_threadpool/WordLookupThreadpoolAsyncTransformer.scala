package com.twitter.finatra.kafkastreams.integration.async_transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.serde.UnKeyed
import com.twitter.finatra.kafkastreams.flushing.AsyncTransformer
import com.twitter.finatra.utils.FuturePools
import com.twitter.util.{Duration, Future}
import java.util.concurrent.atomic.AtomicInteger
import org.apache.kafka.streams.processor.internals.ProcessorRecordContext

class WordLookupThreadpoolAsyncTransformer(
  statsReceiver: StatsReceiver,
  commitInterval: Duration,
  expectedOnFutureSuccessCount: Int)
    extends AsyncTransformer[UnKeyed, String, String, Long](
      statsReceiver,
      maxOutstandingFuturesPerTask = 10,
      flushAsyncRecordsInterval = 1.seconds,
      commitInterval = commitInterval,
      flushTimeout = commitInterval
    ) {

  private val threadpool = FuturePools.fixedPool("word-lookup-threadpool", 100)

  private val onFutureSuccessCounter = new AtomicInteger()

  override def transformAsync(
    key: UnKeyed,
    value: String,
    recordContext: ProcessorRecordContext
  ): Future[Iterable[(String, Long, ProcessorRecordContext)]] = {
    threadpool({

      Thread.sleep(500)

      Seq((value, value.length, recordContext))
    })
  }

  override def onFutureSuccess(
    key: UnKeyed,
    value: String,
    result: Iterable[(String, Long, ProcessorRecordContext)]
  ): Unit = {
    super.onFutureSuccess(key, value, result)
    // Inject artificial delay so that onFutureSuccess may not complete before
    // the assertion in onFlush is made
    Thread.sleep(500)
    onFutureSuccessCounter.incrementAndGet()
  }

  override def onFlush(): Unit = {
    super.onFlush()
    assert(
      onFutureSuccessCounter.get() == expectedOnFutureSuccessCount,
      "Not all onFutureSuccess calls have completed")
  }
}
