package com.twitter.finatra.kafkastreams.integration.wordcount

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Grouped, Materialized, Produced}

class WordCountRocksDbServer extends KafkaStreamsTwitterServer {

  override val name = "wordcount"
  private val countStoreName = "CountsStore"

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder.asScala
      .stream[Bytes, String]("TextLinesTopic")(Consumed.`with`(Serdes.Bytes, Serdes.String))
      .flatMapValues(_.split(' '))
      .groupBy((_, word) => word)(Grouped.`with`(Serdes.String, Serdes.String))
      .count()(Materialized.as(countStoreName))
      .toStream
      .to("WordsWithCountsTopic")(Produced.`with`(Serdes.String, ScalaSerdes.Long))
  }
}
