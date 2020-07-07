package com.twitter.finatra.kafkastreams.integration.tracing

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Materialized, Produced}
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped}
import org.apache.kafka.streams.state.Stores

class TracingServer extends KafkaStreamsTwitterServer {

  override val name = "tracing"

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder.asScala
      .stream[Bytes, String]("tracing")(Consumed.`with`(Serdes.Bytes, Serdes.String))
      .flatMapValues(_.split(" "))
      .groupBy((_, word) => word)(Grouped.`with`(Serdes.String, Serdes.String))
      .count()(
        Materialized
          .as(Stores.inMemoryKeyValueStore("tracing-store"))
          .withKeySerde(Serdes.String())
          .withValueSerde(ScalaSerdes.Long)
          .withCachingDisabled())
      .toStream
      .to("tracing-word-count")(Produced.`with`(Serdes.String, ScalaSerdes.Long))
  }

}
