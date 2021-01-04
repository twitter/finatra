package com.twitter.finatra.kafkastreams.integration.mapasync

import com.twitter.doeverything.thriftscala.{Answer, Question}
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.apache.kafka.streams.scala.Serdes
import org.joda.time.DateTime

class FinatraDslFlatMapAsyncTopologyFeatureTest extends TopologyFeatureTest {
  import FinatraDslFlatMapAsyncServer._

  override protected val topologyTester: FinatraTopologyTester = FinatraTopologyTester(
    kafkaApplicationId = IncomingTopic,
    server = new FinatraDslFlatMapAsyncServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val incomingTopic = topologyTester.topic(
    name = IncomingTopic,
    keySerde = Serdes.Long,
    valSerde = Serdes.Long
  )

  private val flatMapTopic = topologyTester.topic(
    FlatMapTopic,
    Serdes.Long,
    Serdes.String
  )

  private val flatMapValuesTopic = topologyTester.topic(
    FlatMapValuesTopic,
    Serdes.Long,
    ScalaSerdes.Thrift[Question]
  )

  private val mapTopic = topologyTester.topic(
    MapTopic,
    Serdes.String,
    Serdes.String
  )

  private val mapValuesTopic = topologyTester.topic(
    MapValuesTopic,
    Serdes.String,
    ScalaSerdes.Thrift[Answer]
  )

  test("flatMapAsync works") {
    incomingTopic.pipeInput(1L, 1L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // We only see the results of the 1L -> 1L input event because
    // the 0L -> 0L event(s) haven't gone through to the topic we are
    // looking at yet because enough 'flush' events haven't gone
    // through.
    flatMapTopic.readAllOutput().map(record => record.key -> record.value) shouldBe
      Seq(1 -> "1 = 1", 2 -> "2 = 2")
  }

  test("flatMapValuesAsync works") {
    incomingTopic.pipeInput(1L, 1L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // We only see the results of the 1L -> 1L input event because
    // the 0L -> 0L event(s) haven't gone through to the topic we are
    // looking at yet because enough 'flush' events haven't gone
    // through.
    flatMapValuesTopic.readAllOutput().map(record => record.key -> record.value) shouldBe
      Seq(
        1 -> Question("1 = 1"),
        1 -> Question("1 = 1 and more"),
        2 -> Question("2 = 2"),
        2 -> Question("2 = 2 and more")
      )
  }

  test("mapAsync works") {
    incomingTopic.pipeInput(1L, 1L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // We only see the results of the 1L -> 1L input event because
    // the 0L -> 0L event(s) haven't gone through to the topic we are
    // looking at yet because enough 'flush' events haven't gone
    // through.
    mapTopic.readAllOutput().map(record => record.key -> record.value) shouldBe
      Seq(
        "1" -> "1 = 1",
        "1" -> "1 = 1 and more",
        "2" -> "2 = 2",
        "2" -> "2 = 2 and more"
      )
  }

  test("mapValuesAsync works") {
    incomingTopic.pipeInput(1L, 1L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // force a flush
    topologyTester.advanceWallClockTime(CommitInterval * 2)
    incomingTopic.pipeInput(0L, 0L)

    // We only see the results of the 1L -> 1L input event because
    // the 0L -> 0L event(s) haven't gone through to the topic we are
    // looking at yet because enough 'flush' events haven't gone
    // through.
    mapValuesTopic.readAllOutput().map(record => record.key -> record.value) shouldBe
      Seq(
        "1" -> Answer("1 = 1"),
        "1" -> Answer("1 = 1 and more"),
        "2" -> Answer("2 = 2"),
        "2" -> Answer("2 = 2 and more")
      )
  }
}
