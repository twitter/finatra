package com.twitter.finatra.kafkastreams.integration.delay

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafkastreams.integration.delay.DelayStoreServer.{
  Delay,
  IncomingTopic,
  OutgoingTopic
}
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.apache.kafka.streams.scala.Serdes
import org.joda.time.DateTime

class DelayStoreServerTopologyFeatureTest extends TopologyFeatureTest {
  override protected val topologyTester: FinatraTopologyTester = FinatraTopologyTester(
    kafkaApplicationId = IncomingTopic,
    server = new DelayStoreServer,
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

  test("Published event gets published after a delay") {
    incomingTopic.pipeInput(3L, 3L)
    outgoingTopic.readAllOutput() shouldBe empty

    // advance stream time -- have to advance time, publish an event
    // and then advance time again
    topologyTester.advanceWallClockTime(Delay + 1.second)
    incomingTopic.pipeInput(4L, 4L)
    topologyTester.advanceWallClockTime(1.seconds)

    outgoingTopic.assertOutput(3L, 3L)
  }

  test("Two published events within delay window gets published twice") {
    incomingTopic.pipeInput(3L, 3L)
    outgoingTopic.readAllOutput() shouldBe empty

    // advance stream time -- have to advance time, publish an event
    // and then advance time again
    topologyTester.advanceWallClockTime(Delay / 2)
    incomingTopic.pipeInput(4L, 4L)
    topologyTester.advanceWallClockTime(1.second)

    outgoingTopic.readAllOutput() shouldBe empty

    // Publish the duplicate event
    incomingTopic.pipeInput(3L, 3L)

    // advance stream time -- have to advance time, publish an event
    // and then advance time again
    topologyTester.advanceWallClockTime(Delay / 2)
    incomingTopic.pipeInput(5L, 5L)
    topologyTester.advanceWallClockTime(1.second)

    outgoingTopic.readAllOutput().map(record => record.key -> record.value) shouldBe Seq(3L -> 3L)

    // Advance stream time past when the duplicate event would have been published
    // advance stream time -- have to advance time, publish an event
    // and then advance time again
    topologyTester.advanceWallClockTime(Delay / 2)
    incomingTopic.pipeInput(6L, 6L)
    topologyTester.advanceWallClockTime(1.second)

    // We only get the event published that moved time forward, not the duplicate
    outgoingTopic.readAllOutput().map(record => record.key -> record.value) shouldBe Seq(
      4L -> 4L,
      3L -> 3L)

  }

  test("Two published deduplicate events outside delay window gets published twice") {
    incomingTopic.pipeInput(3L, 3L)
    outgoingTopic.readAllOutput() shouldBe empty

    // advance stream time -- have to advance time, publish an event
    // and then advance time again
    topologyTester.advanceWallClockTime(Delay + 1.second)
    incomingTopic.pipeInput(4L, 4L)
    topologyTester.advanceWallClockTime(1.second)

    // Now publish the duplicate event
    incomingTopic.pipeInput(3L, 3L)

    // advance stream time -- have to advance time, publish an event
    // and then advance time again
    topologyTester.advanceWallClockTime(Delay + 1.second)
    incomingTopic.pipeInput(5L, 5L)
    topologyTester.advanceWallClockTime(1.second)

    outgoingTopic.readAllOutput().map(record => record.key -> record.value) shouldBe Seq(
      3L -> 3L,
      4L -> 4L, // The event we published to trigger time forward also got published
      3L -> 3L)
  }
}
