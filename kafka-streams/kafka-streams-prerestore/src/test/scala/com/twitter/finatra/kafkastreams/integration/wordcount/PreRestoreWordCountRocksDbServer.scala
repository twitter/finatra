package com.twitter.finatra.kafkastreams.integration.wordcount

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.prerestore.PreRestoreState
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Materialized, Produced}
import org.apache.kafka.streams.scala.kstream.Grouped

class PreRestoreWordCountRocksDbServer extends KafkaStreamsTwitterServer with PreRestoreState {

  override val name = "wordcount"
  private val countStoreName = "CountsStore"
  flag("hack_to_allow_explicit_http_port_below", "hack", "hack")

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder.asScala
      .stream("TextLinesTopic")(Consumed.`with`(Serdes.Bytes, Serdes.String))
      .flatMapValues(_.split(' '))
      .groupBy((_, word) => word)(Grouped.`with`(Serdes.String, Serdes.String))
      .count()(Materialized.as(countStoreName))
      .toStream
      .to("WordsWithCountsTopic")(Produced.`with`(Serdes.String, ScalaSerdes.Long))
  }
}
