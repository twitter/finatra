package com.twitter.finatra.kafkastreams.integration.window

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.query.QueryableFinatraWindowStore
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import com.twitter.finatra.kafkastreams.transformer.aggregation.WindowedValueSerde
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
    topologyTester.topic(
      "word-to-hourly-counts",
      Serdes.String,
      WindowedValueSerde(ScalaSerdes.Int))

  test("windowed word count test 1") {
    val countStore =
      topologyTester.queryableFinatraWindowStore[String, Int](
        storeName = "CountsStore",
        windowSize = 1.hour,
        allowedLateness = 1.minute,
        queryableAfterClose = 10.minutes,
        keySerde = Serdes.String())

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
    expectedValue: Int
  ): Unit = {
    val currentHourOpt = Some(topologyTester.currentHour.getMillis)
    countStore.get(key, startTime = currentHourOpt, endTime = currentHourOpt) should
      equal(Map(topologyTester.currentHour.getMillis -> expectedValue))
  }
}
