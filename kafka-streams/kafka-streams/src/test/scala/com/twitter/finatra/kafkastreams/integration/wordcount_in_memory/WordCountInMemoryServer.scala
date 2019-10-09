package com.twitter.finatra.kafkastreams.integration.wordcount_in_memory

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Grouped, Materialized, Produced}
import org.apache.kafka.streams.state.Stores

object WordCountInMemoryServerMain extends WordCountInMemoryServer

class WordCountInMemoryServer extends KafkaStreamsTwitterServer {

  override val name = "wordcount"
  private val countStoreName = "CountsStore"

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder.asScala
      .stream("text-lines-topic")(Consumed.`with`(Serdes.Bytes, Serdes.String))
      .flatMapValues(_.split(' '))
      .groupBy((_, word) => word)(Grouped.`with`(Serdes.String, Serdes.String))
      .count()(
        Materialized
          .as(
            Stores
              .inMemoryKeyValueStore(countStoreName)
          )
          .withKeySerde(Serdes.String())
          .withValueSerde(ScalaSerdes.Long)
          .withCachingDisabled()
      )
      .toStream
      .to("WordsWithCountsTopic")(Produced.`with`(Serdes.String, ScalaSerdes.Long))
  }
}
