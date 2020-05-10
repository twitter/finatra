package com.twitter.finatra.kafkastreams.integration.punctuator

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.joda.time.DateTime

class HeartBeatServerTopologyFeatureTest extends TopologyFeatureTest {
  private val startingWallClockTime = new DateTime("1970-01-01T00:00:00Z")

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "wordcount-prod-bob",
    server = new HeartBeatServer,
    startingWallClockTime = startingWallClockTime
  )

  private val inputTopic =
    topologyTester.topic("input-topic", ScalaSerdes.Long, ScalaSerdes.Long)

  private val outputTopic =
    topologyTester.topic("output-topic", ScalaSerdes.Long, ScalaSerdes.Long)

  private val transformStatName = "kafka/stream/transform"
  private val punctuateStatName = "kafka/stream/punctuate"

  test("Publish single value from input to output") {
    topologyTester.stats.assertCounter(transformStatName, 0)
    topologyTester.stats.assertCounter(punctuateStatName, 0)
    inputTopic.pipeInput(1, 1)
    outputTopic.assertOutput(1, 1)
    outputTopic.readAllOutput().isEmpty should be(true)
    topologyTester.stats.assertCounter(transformStatName, 1)
    topologyTester.stats.assertCounter(punctuateStatName, 0)
  }

  test("Publish single value and single heartbeat from input to output") {
    topologyTester.stats.assertCounter(transformStatName, 0)
    topologyTester.stats.assertCounter(punctuateStatName, 0)
    inputTopic.pipeInput(1, 1)
    outputTopic.assertOutput(1, 1)
    topologyTester.advanceWallClockTime(1.seconds)
    val punctuatedTime = startingWallClockTime.getMillis + 1.second.inMillis
    outputTopic.assertOutput(punctuatedTime, punctuatedTime)
    outputTopic.readAllOutput().isEmpty should be(true)
    topologyTester.stats.assertCounter(transformStatName, 1)
    topologyTester.stats.assertCounter(punctuateStatName, 1)
  }

  test("Publish heartbeat from advanced wall clock time to output") {
    topologyTester.stats.assertCounter(transformStatName, 0)
    topologyTester.stats.assertCounter(punctuateStatName, 0)
    topologyTester.advanceWallClockTime(1.seconds)
    val punctuatedTime = startingWallClockTime.getMillis + 1.second.inMillis
    outputTopic.assertOutput(punctuatedTime, punctuatedTime)
    outputTopic.readAllOutput().isEmpty should be(true)
    topologyTester.stats.assertCounter(transformStatName, 0)
    topologyTester.stats.assertCounter(punctuateStatName, 1)
  }

  test("Publish multiple values from input to output") {
    topologyTester.stats.assertCounter(transformStatName, 0)
    topologyTester.stats.assertCounter(punctuateStatName, 0)

    val messages = 1 to 10
    for {
      message <- messages
    } {
      inputTopic.pipeInput(message, message)
    }

    for {
      message <- messages
    } {
      outputTopic.assertOutput(message, message)
    }

    outputTopic.readAllOutput().isEmpty should be(true)
    topologyTester.stats.assertCounter(transformStatName, 10)
    topologyTester.stats.assertCounter(punctuateStatName, 0)
  }

  test("Publish multiple heartbeat from advanced wall clock time to output") {
    topologyTester.stats.assertCounter(transformStatName, 0)
    topologyTester.stats.assertCounter(punctuateStatName, 0)

    val secondsRange = 1 to 10
    for {
      _ <- secondsRange
    } {
      topologyTester.advanceWallClockTime(1.seconds)
    }

    for {
      s <- secondsRange
    } {
      val punctuatedTime = startingWallClockTime.getMillis + s.seconds.inMillis
      outputTopic.assertOutput(punctuatedTime, punctuatedTime)
    }

    outputTopic.readAllOutput().isEmpty should be(true)
    topologyTester.stats.assertCounter(transformStatName, 0)
    topologyTester.stats.assertCounter(punctuateStatName, 10)
  }

  test("Publish multiple values and multiple heartbeats from input to output") {
    topologyTester.stats.assertCounter(transformStatName, 0)
    topologyTester.stats.assertCounter(punctuateStatName, 0)
    inputTopic.pipeInput(1, 1)
    topologyTester.advanceWallClockTime(1.seconds)
    inputTopic.pipeInput(2, 2)
    topologyTester.advanceWallClockTime(1.seconds)
    inputTopic.pipeInput(3, 3)
    topologyTester.advanceWallClockTime(1.seconds)

    outputTopic.assertOutput(1, 1)
    val punctuatedTime1 = startingWallClockTime.getMillis + 1.second.inMillis
    outputTopic.assertOutput(punctuatedTime1, punctuatedTime1)

    outputTopic.assertOutput(2, 2)
    val punctuatedTime2 = punctuatedTime1 + 1.second.inMillis
    outputTopic.assertOutput(punctuatedTime2, punctuatedTime2)

    outputTopic.assertOutput(3, 3)
    val punctuatedTime3 = punctuatedTime2 + 1.second.inMillis
    outputTopic.assertOutput(punctuatedTime3, punctuatedTime3)

    outputTopic.readAllOutput().isEmpty should be(true)
    topologyTester.stats.assertCounter(transformStatName, 3)
    topologyTester.stats.assertCounter(punctuateStatName, 3)
  }
}
