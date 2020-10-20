package com.twitter.finatra.kafkastreams.integration.tocluster

import com.twitter.finatra.kafka.test.EmbeddedKafka
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.apache.kafka.streams.scala.Serdes
import org.joda.time.DateTime

class ToClusterTopologyFeatureTest extends TopologyFeatureTest with EmbeddedKafka {
  override protected lazy val topologyTester: FinatraTopologyTester = FinatraTopologyTester(
    kafkaApplicationId = ToClusterServer.IncomingTopic,
    server = new ToClusterServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z"),
    flags = Map(
      "outgoing.kafka.dest" -> kafkaCluster.bootstrapServers()
    )
  )

  private lazy val incomingTopic = topologyTester.topic(
    name = ToClusterServer.IncomingTopic,
    keySerde = Serdes.Long,
    valSerde = Serdes.Long
  )

  private val outgoingTopic = kafkaTopic(
    name = ToClusterServer.OutgoingTopic,
    keySerde = Serdes.Long,
    valSerde = Serdes.Long
  )

  test("Incoming event gets published to outgoing topic on specified cluster") {
    incomingTopic.pipeInput(1L, 2L)
    outgoingTopic.consumeMessage() should be(1L -> 2L)
  }
}
