package com.twitter.finatra.kafkastreams.integration.delay

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.dsl.FinatraDslDelay
import com.twitter.finatra.kafkastreams.integration.delay.DelaySleepServer.{
  Delay,
  IncomingTopic,
  OutgoingTopic
}
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Produced}

object DelaySleepServer {
  val IncomingTopic = "incoming-topic"
  val OutgoingTopic = "outgoing-topic"
  val Delay = 100.milliseconds

}

class DelaySleepServer extends KafkaStreamsTwitterServer with FinatraDslDelay {
  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {

    implicit val produced: Produced[Long, Long] =
      Produced.`with`(Serdes.Long, Serdes.Long)

    builder.asScala
      .stream(IncomingTopic)(Consumed.`with`(Serdes.Long, Serdes.Long))
      .delayBySleep(Delay)
      .to(OutgoingTopic)
  }
}
