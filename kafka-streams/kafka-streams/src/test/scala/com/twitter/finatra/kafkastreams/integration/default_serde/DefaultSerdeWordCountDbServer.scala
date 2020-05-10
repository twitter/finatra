package com.twitter.finatra.kafkastreams.integration.default_serde

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Grouped, Materialized, Produced}

class DefaultSerdeWordCountDbServer extends KafkaStreamsTwitterServer {

  override val name = "wordcount"
  private val countStoreName = "CountsStore"

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder
      .stream[Bytes, String](
        "TextLinesTopic"
      ) // Uses default serdes since Consumed.with not specified
      .asScala
      .flatMapValues(_.split(' '))
      .groupBy((_, word) => word)(Grouped.`with`(Serdes.String, Serdes.String))
      .count()(Materialized.as(countStoreName))
      .toStream
      .to("WordsWithCountsTopic")(Produced.`with`(Serdes.String, ScalaSerdes.Long))
  }
}
