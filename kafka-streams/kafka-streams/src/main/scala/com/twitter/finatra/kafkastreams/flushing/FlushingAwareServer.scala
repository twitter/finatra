package com.twitter.finatra.kafkastreams.flushing

import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.KafkaStreamsConfig
import com.twitter.util.Duration

/**
 * FlushingAwareServer must be mixed in to servers that rely on manually controlling when a flush/commit occurs.
 * As such, this trait will be needed when using the following classes, FlushingProcessor, FlushingTransformer,
 * AsyncProcessor, AsyncTransformer, and FinatraTransformer
 *
 * This trait sets 'kafka.commit.interval' to 'Duration.Top' to disable the normal Kafka Streams commit process.
 * As such the only commits that will occur are triggered manually, thus allowing us to control when flush/commit
 * occurs
 */
trait FlushingAwareServer extends KafkaStreamsTwitterServer {

  override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
    super
      .streamsProperties(config)
      .commitInterval(Duration.Top)
  }
}
