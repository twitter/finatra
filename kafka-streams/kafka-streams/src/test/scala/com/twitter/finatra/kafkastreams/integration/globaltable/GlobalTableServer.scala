package com.twitter.finatra.kafkastreams.integration.globaltable

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.KeyValueStore

object GlobalTableServer {
  final val GlobalTableTopic = "GlobalTableTopic"
}

class GlobalTableServer extends KafkaStreamsTwitterServer {
  override val name = "globaltable"

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder
      .globalTable(
        GlobalTableServer.GlobalTableTopic,
        Materialized
          .as[Int, Int, KeyValueStore[Bytes, Array[Byte]]]("CountsStore")
          .withKeySerde(ScalaSerdes.Int)
          .withValueSerde(ScalaSerdes.Int)
      )
  }
}
