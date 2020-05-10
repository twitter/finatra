package com.twitter.finatra.kafkastreams.integration.window

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.dsl.FinatraDslWindowedAggregations
import com.twitter.finatra.kafkastreams.transformer.aggregation.{
  FixedTimeWindowedSerde,
  WindowedValueSerde
}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}

class WindowedTweetWordCountServer
    extends KafkaStreamsTwitterServer
    with FinatraDslWindowedAggregations {

  private val countStoreName = "CountsStore"

  override protected def configureKafkaStreams(streamsBuilder: StreamsBuilder): Unit = {
    streamsBuilder.asScala
      .stream("word-and-count")(Consumed.`with`(Serdes.String(), ScalaSerdes.Int))
      .sum(
        stateStore = countStoreName,
        windowSize = windowSize(),
        allowedLateness = 5.minutes,
        queryableAfterClose = 1.hour,
        keySerde = Serdes.String())
      .to("word-to-hourly-counts")(
        Produced.`with`(
          FixedTimeWindowedSerde(Serdes.String, duration = windowSize()),
          WindowedValueSerde(ScalaSerdes.Int)))
  }
}
