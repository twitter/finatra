package com.twitter.unittests.integration.window

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.streams.query.QueryableFinatraWindowStore
import com.twitter.finatra.streams.tests.{FinatraTopologyTester, TopologyFeatureTest}
import com.twitter.finatra.streams.transformer.domain.WindowedValueSerde
import org.apache.kafka.common.serialization.Serdes
import org.joda.time.DateTime

class WindowedTweetWordCountServerTopologyFeatureTest extends TopologyFeatureTest {

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "windowed-wordcount-prod-alice",
    server = new WindowedTweetWordCountServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val wordAndCountTopic =
    topologyTester.topic("word-and-count", Serdes.String(), ScalaSerdes.Int)

  private val hourlyWordAndCountTopic =
    topologyTester.topic("word-to-hourly-counts", Serdes.String,  WindowedValueSerde(ScalaSerdes.Int))


  test("windowed word count test 1") {
    val countStore = topologyTester.queryableFinatraWindowStore[String,  Int]("CountsStore", Serdes.String())

    wordAndCountTopic.pipeInput("bob", 1)
    assertCurrentHourContains(countStore, "bob", 1)
    wordAndCountTopic.pipeInput("bob", 1)
    assertCurrentHourContains(countStore, "bob", 2)
    wordAndCountTopic.pipeInput("alice", 1)
    assertCurrentHourContains(countStore, "bob", 2)
    assertCurrentHourContains(countStore, "alice", 1)
  }

  private def assertCurrentHourContains(
    countStore: QueryableFinatraWindowStore[String, Int],
    key: String,
    expectedValue: Int): Unit = {
    countStore.get(key, 1.hour, topologyTester.currentHour.getMillis) should equal(Some(expectedValue))
  }
}
