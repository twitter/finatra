package com.twitter.finatra.kafkastreams.integration.aggregate

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafkastreams.integration.aggregate.ZipkinAggregatorServer._
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import com.twitter.finatra.kafkastreams.transformer.aggregation.{
  TimeWindowed,
  WindowClosed,
  WindowOpen,
  WindowedValue
}
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import org.apache.kafka.clients.producer.ProducerRecord
import org.joda.time.DateTime
import scala.util.Random

class ZipkinAggregatorTopologyFeatureTest extends TopologyFeatureTest {

  val startTime = new DateTime("2018-01-01T00:00:00Z")

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "zipkin-aggregator",
    server = new ZipkinAggregatorServer,
    startingWallClockTime = startTime
  )

  private val zipkinIngestionTopic = topologyTester.topic(
    IngestionTopic,
    TraceIdSerde,
    SpanSerde
  )

  private val zipkinTracesTopic = topologyTester.topic(
    TracesTopic,
    TracesKey,
    TracesValue
  )

  test("Windowed traces single span") {
    val traceId1 = 1L
    val traceId1Span1: Span = Span(
      traceId = traceId1,
      name = "traceId1Span1",
      spanId = 1L,
      parentId = None
    )

    val firstHourStartTime = Time.create(startTime)

    zipkinIngestionTopic.pipeInput(traceId1, traceId1Span1)

    topologyTester.advanceWallClockTime(30.seconds)

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(firstHourStartTime, traceId1),
      WindowedValue(WindowOpen, SpanCollection(traceId1, Seq(traceId1Span1)))
    )
  }

  test("Windowed traces two spans") {
    val traceId1 = 1L
    val traceId1Span1: Span = Span(
      traceId = traceId1,
      name = "traceId1Span1",
      spanId = 1L,
      parentId = None
    )

    val traceId1Span2: Span = Span(
      traceId = traceId1,
      name = s"traceId${traceId1}Span${traceId1Span1.spanId}",
      spanId = 2L,
      parentId = Some(traceId1Span1.spanId)
    )

    val firstHourStartTime = Time.create(startTime)

    zipkinIngestionTopic.pipeInput(traceId1, traceId1Span1)
    zipkinIngestionTopic.pipeInput(traceId1, traceId1Span2)

    topologyTester.advanceWallClockTime(30.seconds)

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(firstHourStartTime, traceId1),
      WindowedValue(WindowOpen, SpanCollection(traceId1, Array(traceId1Span1, traceId1Span2)))
    )
  }

  test("Windowed traces Two traces and each two spans with closed window") {
    val traceId1 = 1L
    val traceId1Span1: Span = Span(
      traceId = traceId1,
      name = "traceId1Span1",
      spanId = 1L,
      parentId = None
    )

    val traceId1Span2: Span = Span(
      traceId = traceId1,
      name = s"traceId1Span2",
      spanId = 2L,
      parentId = Some(traceId1Span1.spanId)
    )

    val traceId2 = 2L
    val traceId2Span1: Span = Span(
      traceId = traceId2,
      name = "traceId2Span1",
      spanId = 1L,
      parentId = None
    )

    val traceId2Span2: Span = Span(
      traceId = traceId2,
      name = s"traceId2Span2",
      spanId = 2L,
      parentId = Some(traceId2Span1.spanId)
    )

    val traceId3 = 3L
    val traceId3Span1: Span = Span(
      traceId = traceId3,
      name = "traceId3Span1",
      spanId = 1L,
      parentId = None
    )

    val firstHourStartTime = Time.create(startTime)
    val fifthHourStartTime = Time.create(startTime.plusHours(5))

    zipkinIngestionTopic.pipeInput(traceId1, traceId1Span1)

    topologyTester.advanceWallClockTime(30.seconds)

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(firstHourStartTime, traceId1),
      WindowedValue(WindowOpen, SpanCollection(traceId1, Seq(traceId1Span1)))
    )

    zipkinIngestionTopic.pipeInput(traceId2, traceId2Span1)

    topologyTester.advanceWallClockTime(30.seconds)

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(firstHourStartTime, traceId2),
      WindowedValue(WindowOpen, SpanCollection(traceId2, Seq(traceId2Span1)))
    )

    zipkinIngestionTopic.pipeInput(traceId1, traceId1Span2)
    zipkinIngestionTopic.pipeInput(traceId2, traceId2Span2)

    topologyTester.advanceWallClockTime(5.hour)

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(firstHourStartTime, traceId2),
      WindowedValue(WindowOpen, SpanCollection(traceId2, Seq(traceId2Span1, traceId2Span2)))
    )

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(firstHourStartTime, traceId1),
      WindowedValue(WindowOpen, SpanCollection(traceId1, Seq(traceId1Span1, traceId1Span2)))
    )

    zipkinIngestionTopic.pipeInput(traceId3, traceId3Span1)
    topologyTester.advanceWallClockTime(30.seconds)

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(fifthHourStartTime, traceId3),
      WindowedValue(WindowOpen, SpanCollection(traceId3, Seq(traceId3Span1)))
    )

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(firstHourStartTime, traceId1),
      WindowedValue(WindowClosed, SpanCollection(traceId1, Seq(traceId1Span1, traceId1Span2)))
    )

    zipkinTracesTopic.assertOutput(
      TimeWindowed.hourly(firstHourStartTime, traceId2),
      WindowedValue(WindowClosed, SpanCollection(traceId2, Seq(traceId2Span1, traceId2Span2)))
    )
  }

  test("Windowed traces with clusters of traces") {
    val tracesTraceId1Head = Span(traceId = 1, name = "trace1span1", spanId = 1, parentId = None)
    val tracesTraceId1Tail = Seq(
      Span(traceId = 1, name = "trace1span1", spanId = 1, parentId = None),
      Span(traceId = 1, name = "trace1span1", spanId = 2, parentId = Some(1)),
      Span(traceId = 1, name = "trace1span3", spanId = 3, parentId = Some(2)),
      Span(traceId = 1, name = "trace1span4", spanId = 4, parentId = Some(3)),
      Span(traceId = 1, name = "trace1span5", spanId = 5, parentId = Some(4)),
      Span(traceId = 1, name = "trace1span6", spanId = 6, parentId = Some(5)),
      Span(traceId = 1, name = "trace1span7", spanId = 7, parentId = Some(6))
    )

    val tracesTraceId2Head = Span(traceId = 2, name = "trace2span1", spanId = 1, parentId = None)
    val tracesTraceId2Tail = Seq(
      Span(traceId = 2, name = "trace2span1", spanId = 2, parentId = Some(1)),
      Span(traceId = 2, name = "trace2span3", spanId = 3, parentId = Some(2)),
      Span(traceId = 2, name = "trace2span4", spanId = 4, parentId = Some(3)),
      Span(traceId = 2, name = "trace2span5", spanId = 5, parentId = Some(4)),
      Span(traceId = 2, name = "trace2span6", spanId = 6, parentId = Some(5)),
      Span(traceId = 2, name = "trace2span7", spanId = 7, parentId = Some(6))
    )

    val tracesTraceId3Head = Span(traceId = 3, name = "trace3span1", spanId = 1, parentId = None)
    val tracesTraceId3Tail = Seq(
      Span(traceId = 3, name = "trace3span2", spanId = 2, parentId = Some(1))
    )

    val traceTraceId4 = Span(traceId = 4, name = "trace4span1", spanId = 1, parentId = None)

    val traces = tracesTraceId1Tail ++ tracesTraceId2Tail ++ tracesTraceId3Tail

    val tracesShuffled = Random.shuffle(traces)

    tracesShuffled.foreach { span =>
      zipkinIngestionTopic.pipeInput(span.traceId, span)
      topologyTester.advanceWallClockTime(1.seconds)
    }

    topologyTester.advanceWallClockTime(30.seconds)

    val allOutputWindowOpened: Seq[
      ProducerRecord[TimeWindowed[TraceId], WindowedValue[SpanCollection]]
    ] =
      zipkinTracesTopic.readAllOutput()
    allOutputWindowOpened.length should be(3)

    val tracesTraceId1Output =
      allOutputWindowOpened.filter(p => p.key().value == tracesTraceId1Tail.head.traceId).head
    val tracesTraceId2Output =
      allOutputWindowOpened.filter(p => p.key().value == tracesTraceId2Tail.head.traceId).head
    val tracesTraceId3Output =
      allOutputWindowOpened.filter(p => p.key().value == tracesTraceId3Tail.head.traceId).head

    tracesTraceId1Output.value().value.spans.length should be(tracesTraceId1Tail.length)
    tracesTraceId2Output.value().value.spans.length should be(tracesTraceId2Tail.length)
    tracesTraceId3Output.value().value.spans.length should be(tracesTraceId3Tail.length)

    zipkinIngestionTopic.pipeInput(tracesTraceId1Head.traceId, tracesTraceId1Head)
    zipkinIngestionTopic.pipeInput(tracesTraceId2Head.traceId, tracesTraceId2Head)
    zipkinIngestionTopic.pipeInput(tracesTraceId3Head.traceId, tracesTraceId3Head)

    topologyTester.advanceWallClockTime(5.hour)

    val allOutputAfterFiveHours: Seq[
      ProducerRecord[TimeWindowed[TraceId], WindowedValue[SpanCollection]]
    ] = zipkinTracesTopic.readAllOutput()
    allOutputAfterFiveHours.length should be(3)

    allOutputAfterFiveHours.count(_.value().windowResultType == WindowClosed) should be(0)
    allOutputAfterFiveHours.count(_.value().windowResultType == WindowOpen) should be(3)

    zipkinIngestionTopic.pipeInput(traceTraceId4.traceId, traceTraceId4)

    topologyTester.advanceWallClockTime(30.seconds)

    val allOutputAfterWindowClosed: Seq[
      ProducerRecord[TimeWindowed[TraceId], WindowedValue[SpanCollection]]
    ] = zipkinTracesTopic.readAllOutput()
    allOutputAfterWindowClosed.length should be(4)

    allOutputAfterWindowClosed.count(_.value().windowResultType == WindowClosed) should be(3)
    allOutputAfterWindowClosed.count(_.value().windowResultType == WindowOpen) should be(1)
  }
}
