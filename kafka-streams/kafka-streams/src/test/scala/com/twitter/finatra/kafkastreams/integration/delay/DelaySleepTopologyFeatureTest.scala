package com.twitter.finatra.kafkastreams.integration.delay

import com.twitter.finatra.kafkastreams.integration.delay.DelaySleepServer.{
  IncomingTopic,
  OutgoingTopic
}
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.apache.kafka.streams.scala.Serdes
import org.joda.time.DateTime

class DelaySleepTopologyFeatureTest extends TopologyFeatureTest {
  override protected val topologyTester: FinatraTopologyTester = FinatraTopologyTester(
    kafkaApplicationId = IncomingTopic,
    server = new DelaySleepServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val incomingTopic = topologyTester.topic(
    name = IncomingTopic,
    keySerde = Serdes.Long,
    valSerde = Serdes.Long
  )

  private val outgoingTopic = topologyTester.topic(
    name = OutgoingTopic,
    keySerde = Serdes.Long,
    valSerde = Serdes.Long
  )

  test("Published event gets published") {
    incomingTopic.pipeInput(3L, 3L)

    outgoingTopic.assertOutput(3L, 3L)
  }
}
