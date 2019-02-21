package com.twitter.finatra.kafkastreams.integration.finatratransformer

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.apache.kafka.common.serialization.Serdes
import org.joda.time.DateTime

class WordLengthServerTopologyFeatureTest extends TopologyFeatureTest {

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "test-transformer-prod-alice",
    server = new WordLengthServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val wordAndCountTopic =
    topologyTester.topic(WordLengthServer.stringsAndInputsTopic, Serdes.String(), Serdes.String())

  private val stringAndCountTopic =
    topologyTester.topic(WordLengthServer.StringsAndOutputsTopic, Serdes.String(), Serdes.String())

  test("test inputs get transformed and timers fire") {
    wordAndCountTopic.pipeInput("key", "")
    stringAndCountTopic.assertOutput("key", "onMessage key " + "key".length)
    // advance time
    topologyTester.advanceWallClockTime(6.seconds)
    // send a message to advance the watermark
    wordAndCountTopic.pipeInput("key2", "")
    // advance time again to cause the new watermark to get passed through onWatermark
    topologyTester.advanceWallClockTime(1.seconds)

    stringAndCountTopic.assertOutput("key2", "onMessage key2 " + "key2".length)
    stringAndCountTopic.assertOutput("key", "onEventTimer key " + "key".length)
  }
}
