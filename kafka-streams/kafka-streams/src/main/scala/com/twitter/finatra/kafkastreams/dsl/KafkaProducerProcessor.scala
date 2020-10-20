package com.twitter.finatra.kafkastreams.dsl

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.producers.FinagleKafkaProducer
import com.twitter.finatra.kafkastreams.flushing.AsyncProcessor
import com.twitter.finatra.kafkastreams.utils.MessageTimestamp
import com.twitter.util.{Duration, Future}

private[dsl] class KafkaProducerProcessor[K, V](
  topic: String,
  statsReceiver: StatsReceiver,
  producer: FinagleKafkaProducer[K, V],
  maxOutstandingFutures: Int,
  commitInterval: Duration,
  flushTimeout: Duration)
    extends AsyncProcessor[K, V](
      statsReceiver = statsReceiver,
      maxOutstandingFuturesPerTask = maxOutstandingFutures,
      commitInterval = commitInterval,
      flushTimeout = flushTimeout) {

  override protected def processAsync(
    key: K,
    value: V,
    timestamp: MessageTimestamp
  ): Future[Unit] = {
    producer.send(topic, key, value, timestamp).unit
  }
}
