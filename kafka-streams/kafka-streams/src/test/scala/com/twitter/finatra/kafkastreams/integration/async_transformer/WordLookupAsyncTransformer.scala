package com.twitter.finatra.kafkastreams.integration.async_transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finatra.kafka.serde.UnKeyed
import com.twitter.finatra.kafkastreams.flushing.AsyncTransformer
import com.twitter.util.{Duration, Future}
import org.apache.kafka.streams.processor.internals.ProcessorRecordContext

class WordLookupAsyncTransformer(statsReceiver: StatsReceiver, commitInterval: Duration)
    extends AsyncTransformer[UnKeyed, String, String, Long](
      statsReceiver,
      maxOutstandingFuturesPerTask = 10,
      flushAsyncRecordsInterval = 1.seconds,
      commitInterval = commitInterval,
      flushTimeout = commitInterval
    ) {

  override def transformAsync(
    key: UnKeyed,
    value: String,
    recordContext: ProcessorRecordContext
  ): Future[Iterable[(String, Long, ProcessorRecordContext)]] = {
    info(s"transformAsync $key $value")

    for (length <- lookupWordLength(value)) yield {
      Seq((value, length, recordContext))
    }
  }

  /**
   * Look up the length of a word.
   * @param word a single word without spaces, or a word with a delay built in for the future,
   *             e.g. "hello|3" means process the word "hello" with 3s delay.
   * @return a Future containing the word length that is either immediately resolved or delayed.
   */
  private def lookupWordLength(word: String): Future[Int] = {
    word.split('|').toSeq match {
      case Seq(originalWord, delaySeconds) =>
        Future(originalWord.length).delayed(delaySeconds.toInt.seconds)(DefaultTimer)
      case _ => Future(word.length)
    }
  }
}
