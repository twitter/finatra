package com.twitter.finatra.kafkastreams.processors

import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.KafkaStreamsConfig
import com.twitter.util.Duration

trait FlushingAwareServer extends KafkaStreamsTwitterServer {

  override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
    super
      .streamsProperties(config)
      .commitInterval(Duration.Top) //FlushingProcessor (used by EventBusProducerProcessor) requires the ability to manage its own commits. As such, 'kafka.commit.interval' must be set to 'duration.top' to disable the normal Kafka Streams commit process
  }
}
