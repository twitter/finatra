package com.twitter.unittests.integration.compositesum

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.streams.query.QueryableFinatraWindowStore
import com.twitter.finatra.streams.tests.{FinatraTopologyTester, TopologyFeatureTest}
import com.twitter.finatra.streams.transformer.domain.{
  FixedTimeWindowedSerde,
  TimeWindowed,
  WindowClosed,
  WindowedValue,
  WindowedValueSerde
}
import org.apache.kafka.common.serialization.Serdes
import org.joda.time.DateTime

class CompositeSumServerTopologyFeatureTest extends TopologyFeatureTest {

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "windowed-wordcount-prod-alice",
    server = new CompositeSumServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val wordAndCountTopic =
    topologyTester.topic("key-and-type", ScalaSerdes.Int, ScalaSerdes.Int)

  private val hourlyWordAndCountTopic =
    topologyTester.topic(
      "key-to-hourly-counts",
      FixedTimeWindowedSerde(ScalaSerdes.Int, duration = 1.hour),
      WindowedValueSerde(Serdes.String()))

  test("windowed word count test 1") {
    val countStore = topologyTester
      .queryableFinatraWindowStore[SampleCompositeKey, Int]("CountsStore", SampleCompositeKeySerde)

    wordAndCountTopic.pipeInput(1, 1)
    assertCurrentHourContains(countStore, SampleCompositeKey(1, 1), 1)
    wordAndCountTopic.pipeInput(1, 1)
    assertCurrentHourContains(countStore, SampleCompositeKey(1, 1), 2)
    wordAndCountTopic.pipeInput(2, 1)
    assertCurrentHourContains(countStore, SampleCompositeKey(1, 1), 2)
    assertCurrentHourContains(countStore, SampleCompositeKey(2, 1), 1)
    wordAndCountTopic.pipeInput(2, 2)
    assertCurrentHourContains(countStore, SampleCompositeKey(1, 1), 2)
    assertCurrentHourContains(countStore, SampleCompositeKey(2, 1), 1)
    assertCurrentHourContains(countStore, SampleCompositeKey(2, 2), 1)

    val startTime = topologyTester.currentHour

    topologyTester.advanceWallClockTime(2.hours)
    wordAndCountTopic.pipeInput(3, 3)
    assertClosedHourlyCountsOutput(startTime, 1, Map(1 -> 2))
    assertClosedHourlyCountsOutput(startTime, 2, Map(2 -> 1, 1 -> 1))
  }

  private def assertClosedHourlyCountsOutput(hour: DateTime, key: Int, expected: Map[Int, Int]) = {
    val input = hourlyWordAndCountTopic.readOutput()
    input.key() should equal(TimeWindowed.hourly(hour.getMillis, key))
    input.value() should equal(WindowedValue(WindowClosed, expected.toString()))
  }

  private def assertCurrentHourContains(
    countStore: QueryableFinatraWindowStore[SampleCompositeKey, Int],
    key: SampleCompositeKey,
    expectedValue: Int
  ): Unit = {
    countStore.get(key, 1.hour, topologyTester.currentHour.getMillis) should equal(
      Some(expectedValue))
  }
}
