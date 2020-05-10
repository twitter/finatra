package com.twitter.finatra.kafkastreams.integration.compositesum

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.dsl.FinatraDslWindowedAggregations
import com.twitter.finatra.kafkastreams.integration.compositesum.UserClicksTypes.{
  NumClicksSerde,
  UserIdSerde
}
import com.twitter.finatra.kafkastreams.transformer.aggregation.{
  FixedTimeWindowedSerde,
  WindowedValueSerde
}
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}

class UserClicksServer extends KafkaStreamsTwitterServer with FinatraDslWindowedAggregations {

  override def configureKafkaStreams(streamsBuilder: StreamsBuilder): Unit = {
    streamsBuilder.asScala
      .stream("userid-to-clicktype")(Consumed.`with`(UserIdSerde, ScalaSerdes.Int))
      .map((userId, clickType) => UserClicks(userId, clickType) -> 1)
      .sum(
        stateStore = "user-clicks-store",
        windowSize = 1.hour,
        allowedLateness = 5.minutes,
        queryableAfterClose = 1.hour,
        emitUpdatedEntriesOnCommit = true,
        keySerde = UserClicksSerde
      )
      .to("userid-to-hourly-clicks")(Produced.`with`(
        FixedTimeWindowedSerde(UserClicksSerde, duration = 1.hour),
        WindowedValueSerde(NumClicksSerde)))
  }
}
