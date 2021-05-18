package com.twitter.finatra.kafkastreams.integration.tracing

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Init
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.filter.PayloadSizeFilter.ClientReqTraceKey
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.tracing.Annotation.BinaryAnnotation
import com.twitter.finagle.tracing.{Annotation, Record, Trace, TraceId, Tracer}
import com.twitter.finatra.kafka.domain.AckMode
import com.twitter.finatra.kafka.producers.KafkaProducerTraceAnnotatorImpl.{
  ClientIdAnnotation,
  ProducerSendAnnotation,
  ProducerTopicAnnotation
}
import com.twitter.finatra.kafka.producers.TracingKafkaProducer.TraceIdHeader
import com.twitter.finatra.kafka.producers.{
  FinagleKafkaProducerBuilder,
  TracingKafkaProducer,
  producerTracingEnabled
}
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.KafkaStreamsMultiServerFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import scala.collection.JavaConverters._
import scala.collection.mutable

class TracingServerTest extends KafkaStreamsMultiServerFeatureTest {

  private class RecordingTracer(isActivelyTracing: Boolean, sampleTrace: Option[Boolean])
      extends Tracer {
    val records: mutable.Buffer[Record] = new mutable.ArrayBuffer[Record]()

    override def record(record: Record): Unit = records += record

    override def isActivelyTracing(traceId: TraceId): Boolean = isActivelyTracing

    override def sampleTrace(traceId: TraceId): Option[Boolean] = sampleTrace
  }

  private def createServer: EmbeddedTwitterServer =
    new EmbeddedTwitterServer(
      new TracingServer,
      flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "tracing-test"),
      disableTestLogging = true
    )

  private def getTestProducer(statsReceiver: StatsReceiver) = FinagleKafkaProducerBuilder()
    .dest(brokers.map(_.brokerList()).mkString(","))
    .statsReceiver(statsReceiver)
    .clientId("tracing-test-producer")
    .ackMode(AckMode.ALL)
    .keySerializer(ScalaSerdes.Long.serializer)
    .valueSerializer(Serdes.String.serializer)
    .buildClient()

  private val tracingTopic = kafkaTopic(ScalaSerdes.Long, Serdes.String, "tracing")
  private val wordCountTopic = kafkaTopic(Serdes.String, ScalaSerdes.Long, "tracing-word-count")

  test("producer tracing should send trace id when actively tracing") {
    val tracer = new RecordingTracer(true, Some(true))
    val producerTraceId = Trace.nextId.copy(_sampled = Some(true))

    producerTracingEnabled.let(true) {
      Trace.letTracer(tracer) {
        val server = createServer
        server.start()
        Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, producerTraceId) {
          val producer = getTestProducer(server.inMemoryStatsReceiver)
          try {
            producer
              .send(new ProducerRecord(tracingTopic.topic, 1L, "hello world hello"))
              .get(5, TimeUnit.SECONDS)
          } finally {
            producer.close(java.time.Duration.ofSeconds(5))
          }
        }

        val records = wordCountTopic.consumeRecords(3)
        val messages = records.map(r =>
          wordCountTopic.keySerde.deserializer.deserialize(wordCountTopic.topic, r.key()) ->
            wordCountTopic.valSerde.deserializer.deserialize(wordCountTopic.topic, r.value()))
        messages should contain theSameElementsAs Seq(
          "world" -> 1,
          "hello" -> 1,
          "hello" -> 2
        )
        val traceIdHeaders = records.flatMap(_.headers().asScala).flatMap {
          case header if header.key() == TraceIdHeader =>
            TraceId
              .deserialize(header.value())
              .toOption
          case _ => None
        }
        traceIdHeaders should contain oneElementOf Seq(producerTraceId)

        tracer.records.map(_.annotation) should contain theSameElementsAs Seq(
          BinaryAnnotation("clnt/finagle.label", "kafka.producer"),
          BinaryAnnotation("clnt/finagle.version", Init.finagleVersion),
          BinaryAnnotation("clnt/namer.path", brokers.map(_.brokerList()).mkString(",")),
          BinaryAnnotation(ClientReqTraceKey, 25),
          BinaryAnnotation(ProducerTopicAnnotation, tracingTopic.topic),
          BinaryAnnotation(ClientIdAnnotation, "tracing-test-producer"),
          Annotation.ServiceName("kafka.producer"),
          Annotation.Message(ProducerSendAnnotation),
          Annotation.ClientSend,
          Annotation.WireSend,
          Annotation.ClientRecv,
          Annotation.WireRecv,
          Annotation.Rpc("send")
        )

        server.close(5.seconds)
      }
    }
  }
}
