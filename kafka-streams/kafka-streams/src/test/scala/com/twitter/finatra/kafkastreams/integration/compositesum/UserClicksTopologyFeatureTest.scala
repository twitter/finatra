package com.twitter.finatra.kafkastreams.integration.compositesum

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafkastreams.integration.compositesum.UserClicksTypes._
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import com.twitter.finatra.kafkastreams.transformer.aggregation.{
  FixedTimeWindowedSerde,
  TimeWindowed,
  WindowClosed,
  WindowOpen,
  WindowedValue,
  WindowedValueSerde
}
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import org.joda.time.DateTime

class UserClicksTopologyFeatureTest extends TopologyFeatureTest {

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "user-clicks-prod",
    server = new UserClicksServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z"))

  private val userIdToClicksTopic =
    topologyTester.topic("userid-to-clicktype", UserIdSerde, ClickTypeSerde)

  private val hourlyWordAndCountTopic =
    topologyTester.topic(
      "userid-to-hourly-clicks",
      FixedTimeWindowedSerde(UserClicksSerde, duration = 1.hour),
      WindowedValueSerde(NumClicksSerde))

  test("windowed clicks") {
    val userId1 = 1
    val firstHourStartTime = Time.create(new DateTime("2018-01-01T00:00:00Z"))
    val fifthHourStartTime = Time.create(new DateTime("2018-01-01T05:00:00Z"))

    userIdToClicksTopic.pipeInput(userId1, 100)
    userIdToClicksTopic.pipeInput(userId1, 200)
    userIdToClicksTopic.pipeInput(userId1, 300)
    userIdToClicksTopic.pipeInput(userId1, 200)
    userIdToClicksTopic.pipeInput(userId1, 300)
    userIdToClicksTopic.pipeInput(userId1, 300)

    val record1 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 100)),
      WindowedValue(WindowOpen, 1))

    val record2 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 200)),
      WindowedValue(WindowOpen, 2))

    val record3 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 300)),
      WindowedValue(WindowOpen, 3))

    topologyTester.advanceWallClockTime(30.seconds)
    hourlyWordAndCountTopic.assertAllOutput(
      Seq(record1, record3, record2), //kafka 2.2
      Seq(record3, record1, record2) //kafka 2.5
    )

    userIdToClicksTopic.pipeInput(userId1, 100)
    userIdToClicksTopic.pipeInput(userId1, 200)
    userIdToClicksTopic.pipeInput(userId1, 300)

    val record4 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 100)),
      WindowedValue(WindowOpen, 2))

    val record5 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 300)),
      WindowedValue(WindowOpen, 4))

    val record6 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 200)),
      WindowedValue(WindowOpen, 3))

    topologyTester.advanceWallClockTime(5.hours)
    hourlyWordAndCountTopic.assertAllOutput(
      Seq(record4, record5, record6), //kafka2.2
      Seq(record5, record4, record6) //kafka2.5
    )

    userIdToClicksTopic.pipeInput(userId1, 1)
    topologyTester.advanceWallClockTime(30.seconds)

    val record7 = (
      TimeWindowed.hourly(fifthHourStartTime, UserClicks(userId1, clickType = 1)),
      WindowedValue(WindowOpen, 1))
    val record8 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 100)),
      WindowedValue(WindowClosed, 2))
    val record9 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 200)),
      WindowedValue(WindowClosed, 3))
    val record10 = (
      TimeWindowed.hourly(firstHourStartTime, UserClicks(userId1, clickType = 300)),
      WindowedValue(WindowClosed, 4))

    hourlyWordAndCountTopic.assertAllOutput(
      Seq(record7, record8, record9, record10), //kafka2.2
      Seq(record7, record8, record9, record10) //kafka2.5
    )

  }
}
