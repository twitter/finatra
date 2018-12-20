package com.twitter.unittests.integration.compositesum

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.streams.transformer.domain.{FixedTimeWindowedSerde, WindowedValueSerde}
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}
import org.apache.kafka.streams.scala.Serdes

class CompositeSumServer extends KafkaStreamsTwitterServer {

  private val countStoreName = "CountsStore"
  private val windowSize = 1.hour

  override protected def configureKafkaStreams(streamsBuilder: StreamsBuilder): Unit = {
    streamsBuilder.asScala
      .stream("key-and-type")(Consumed.`with`(ScalaSerdes.Int, ScalaSerdes.Int))
      .map((key, value) => SampleCompositeKey(key, value) -> 1)
      .compositeSum(
        stateStore = countStoreName,
        windowSize = windowSize,
        allowedLateness = 5.minutes,
        queryableAfterClose = 1.hour,
        emitOnClose = true,
        SampleCompositeKey.RangeStart,
        SampleCompositeKeySerde
      ).mapValues(_.map(_.toString()))
      .to("key-to-hourly-counts")(Produced.`with`(
        FixedTimeWindowedSerde(ScalaSerdes.Int, duration = windowSize),
        WindowedValueSerde(Serdes.String)))
  }
}
