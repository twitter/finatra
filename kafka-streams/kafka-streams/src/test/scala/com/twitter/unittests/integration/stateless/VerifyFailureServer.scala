package com.twitter.unittests.integration.stateless

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.StatelessKafkaStreamsTwitterServer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Materialized, Produced, Serialized}

class VerifyFailureServer extends StatelessKafkaStreamsTwitterServer {

  override val name = "stateless"
  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder.asScala
      .stream("TextLinesTopic")(Consumed.`with`(Serdes.Bytes, Serdes.String))
      .flatMapValues(_.split(' '))
      .groupBy((_, word) => word)(Serialized.`with`(Serdes.String, Serdes.String))
      .count()(Materialized.as("CountsStore"))
      .toStream
      .to("WordsWithCountsTopic")(Produced.`with`(Serdes.String, ScalaSerdes.Long))
  }
}
