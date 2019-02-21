package com.twitter.finatra.kafkastreams.integration.async_transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.serde.UnKeyed
import com.twitter.finatra.kafkastreams.flushing.AsyncTransformer
import com.twitter.finatra.kafkastreams.utils.MessageTimestamp
import com.twitter.util.{Duration, Future}

class WordLookupAsyncTransformer(statsReceiver: StatsReceiver, commitInterval: Duration)
    extends AsyncTransformer[UnKeyed, String, String, Long](
      statsReceiver,
      maxOutstandingFuturesPerTask = 10,
      flushAsyncRecordsInterval = 1.second,
      commitInterval = commitInterval,
      flushTimeout = commitInterval
    ) {

  override def transformAsync(
    key: UnKeyed,
    value: String,
    timestamp: MessageTimestamp
  ): Future[Iterable[(String, Long, MessageTimestamp)]] = {
    info(s"transformAsync $key $value")

    for (length <- lookupWordLength(value)) yield {
      Seq((value, length, timestamp))
    }
  }

  private def lookupWordLength(word: String): Future[Int] = {
    Future(word.length)
  }
}
