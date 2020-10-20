package com.twitter.finatra.kafkastreams.integration.tocluster

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.domain.KafkaTopic
import com.twitter.finatra.kafkastreams.dsl.FinatraDslToCluster
import com.twitter.finatra.kafkastreams.flushing.FlushingAwareServer
import com.twitter.finatra.kafkastreams.integration.tocluster.ToClusterServer.{
  IncomingTopic,
  OutgoingTopic
}
import com.twitter.inject.annotations.Flags
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Produced}

object ToClusterServer {
  val IncomingTopic = "incoming-topic"
  val OutgoingTopic = "outgoing-topic"
}

class ToClusterServer extends FlushingAwareServer with FinatraDslToCluster {
  flag[String]("outgoing.kafka.dest", "cluster to publish events to")

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    val cluster = injector.instance[String](Flags.named("outgoing.kafka.dest"))

    implicit val produced: Produced[Long, Long] =
      Produced.`with`(Serdes.Long, Serdes.Long)

    builder.asScala
      .stream(IncomingTopic)(Consumed.`with`(Serdes.Long, Serdes.Long))
      .toCluster(
        cluster = cluster,
        topic = KafkaTopic(OutgoingTopic),
        clientId = name,
        statsReceiver = injector.instance[StatsReceiver])
  }
}
