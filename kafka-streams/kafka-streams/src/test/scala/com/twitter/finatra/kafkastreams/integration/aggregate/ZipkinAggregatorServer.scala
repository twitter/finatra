package com.twitter.finatra.kafkastreams.integration.aggregate

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.{AbstractSerde, ScalaSerdes}
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.dsl.FinatraDslWindowedAggregations
import com.twitter.finatra.kafkastreams.integration.aggregate.ZipkinAggregatorServer._
import com.twitter.finatra.kafkastreams.transformer.aggregation.{
  FixedTimeWindowedSerde,
  TimeWindowed,
  WindowedValue,
  WindowedValueSerde
}
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}
import scala.util.Try

object ZipkinAggregatorServer {
  type TraceId = Long
  type SpanId = Long
  type ParentId = Long
  case class Span(traceId: TraceId, name: String, spanId: SpanId, parentId: Option[ParentId])
  case class SpanCollection(traceId: TraceId, spans: Seq[Span])

  object SpanSerde extends AbstractSerde[Span] {
    override def deserialize(bytes: Array[Byte]): Span = {
      val spanParts = new String(bytes).split(',')
      Span(
        traceId = spanParts(0).toLong,
        name = spanParts(1),
        spanId = spanParts(2).toLong,
        parentId = Try.apply(spanParts(3).toLong).toOption
      )
    }

    override def serialize(span: Span): Array[Byte] = {
      s"${span.traceId},${span.name},${span.spanId},${span.parentId.getOrElse(None)}".getBytes
    }
  }

  object SpanCollectionSerde extends AbstractSerde[SpanCollection] {
    override def deserialize(bytes: Array[Byte]): SpanCollection = {
      val spanCollectionParts = new String(bytes).split('|')
      val traceId = spanCollectionParts(0).toLong
      val spansParts = spanCollectionParts(1).split(':')

      val spans = spansParts.map { span =>
        val spanParts = span.split(',')
        Span(
          traceId = spanParts(0).toLong,
          name = spanParts(1),
          spanId = spanParts(2).toLong,
          parentId = Try.apply(spanParts(3).toLong).toOption
        )
      }

      SpanCollection(traceId, spans)
    }

    override def serialize(spanCollection: SpanCollection): Array[Byte] = {
      val spans = spanCollection.spans
        .map { span =>
          s"${span.traceId},${span.name},${span.spanId},${span.parentId.getOrElse(None)}"
        }.mkString(":")
      s"${spanCollection.traceId}|$spans".getBytes
    }
  }

  val TraceIdSerde: Serde[TraceId] = ScalaSerdes.Long
  val SpanIdSerde: Serde[SpanId] = ScalaSerdes.Long
  val ParentIdSerde: Serde[SpanId] = ScalaSerdes.Long

  val IngestionTopic: String = "zipkin-ingestion"
  val IngestionKey: Serde[TraceId] = TraceIdSerde
  val IngestionValue: Serde[Span] = SpanSerde
  val IngestionConsumed: Consumed[TraceId, Span] = Consumed.`with`(IngestionKey, IngestionValue)

  val TracesTopic: String = "zipkin-traces"
  val TracesKey: Serde[TimeWindowed[TraceId]] =
    FixedTimeWindowedSerde(TraceIdSerde, duration = 1.hour)
  val TracesValue: WindowedValueSerde[SpanCollection] = WindowedValueSerde(SpanCollectionSerde)
  val TracesProduced: Produced[TimeWindowed[TraceId], WindowedValue[SpanCollection]] =
    Produced.`with`(TracesKey, TracesValue)
}

class ZipkinAggregatorServer extends KafkaStreamsTwitterServer with FinatraDslWindowedAggregations {
  val TraceIdAggregatorStore = "traceid-aggregator-store"

  override def configureKafkaStreams(streamsBuilder: StreamsBuilder): Unit = {
    streamsBuilder.asScala
      .stream(topic = IngestionTopic)(IngestionConsumed)
      .aggregate[SpanCollection](
        stateStore = TraceIdAggregatorStore,
        windowSize = 1.hour,
        allowedLateness = 5.minutes,
        queryableAfterClose = 1.hour,
        keySerde = TraceIdSerde,
        aggregateSerde = SpanCollectionSerde,
        initializer = () => SpanCollection(0, Seq[Span]()),
        aggregator = (traceIdAndSpan: (TraceId, Span), spanCollection: SpanCollection) => {
          val (traceId, span) = traceIdAndSpan
          spanCollection.copy(traceId = traceId, spans = spanCollection.spans :+ span)
        },
        emitOnClose = true,
        emitUpdatedEntriesOnCommit = true,
        windowSizeRetentionMultiplier = 2
      )
      .to(topic = TracesTopic)(TracesProduced)
  }
}
