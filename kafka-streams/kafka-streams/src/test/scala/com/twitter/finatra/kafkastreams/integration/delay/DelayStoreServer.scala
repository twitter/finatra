package com.twitter.finatra.kafkastreams.integration.delay

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.dsl.FinatraDslDelay
import com.twitter.finatra.kafkastreams.integration.delay.DelayStoreServer.{
  Delay,
  DelayStoreKey,
  IncomingTopic,
  OutgoingTopic
}
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Produced}

object DelayStoreServer {
  val IncomingTopic = "incoming-topic"
  val OutgoingTopic = "outgoing-topic"
  val Delay = 10.seconds
  val DelayStoreKey = "storekey"
}

class DelayStoreServer extends KafkaStreamsTwitterServer with FinatraDslDelay {
  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {

    implicit val produced: Produced[Long, Long] =
      Produced.`with`(Serdes.Long, Serdes.Long)

    builder.asScala
      .stream(IncomingTopic)(Consumed.`with`(Serdes.Long, Serdes.Long))
      .delayWithStore(Delay, DelayStoreKey)
      .to(OutgoingTopic)
  }
}
