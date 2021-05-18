package com.twitter.finatra.kafka.test.integration

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Init
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.filter.PayloadSizeFilter.{ClientRepTraceKey, ClientReqTraceKey}
import com.twitter.finagle.tracing.Annotation.BinaryAnnotation
import com.twitter.finagle.tracing.{Annotation, Flags, NullTracer, Record, Trace, TraceId}
import com.twitter.finatra.kafka.consumers.KafkaConsumerTraceAnnotatorImpl.{
  ConsumerGroupIdAnnotation,
  ConsumerPollAnnotation,
  ConsumerTopicAnnotation,
  ClientIdAnnotation => ConsumerClientIdAnnotation
}
import com.twitter.finatra.kafka.consumers.KafkaConsumerTracer.ProducerTraceIdAnnotation
import com.twitter.finatra.kafka.consumers.{
  FinagleKafkaConsumerBuilder,
  KafkaConsumerTracer,
  consumerTracingEnabled
}
import com.twitter.finatra.kafka.domain.{AckMode, KafkaGroupId}
import com.twitter.finatra.kafka.producers.KafkaProducerTraceAnnotatorImpl.{
  ProducerSendAnnotation,
  ProducerTopicAnnotation,
  ClientIdAnnotation => ProducerClientIdAnnotation
}
import com.twitter.finatra.kafka.producers.{
  FinagleKafkaProducerBuilder,
  TracingKafkaProducer,
  producerTracingEnabled
}
import com.twitter.finatra.kafka.test.EmbeddedKafka
import com.twitter.util.Duration
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.consumer.{KafkaConsumer, OffsetResetStrategy}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.Serdes
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import scala.collection.JavaConverters._

class KafkaConsumerTracerTest extends EmbeddedKafka {
  private val tracingTopic = kafkaTopic(Serdes.String, Serdes.String, "tracing-topic")

  private def buildTestConsumerBuilder(): FinagleKafkaConsumerBuilder[String, String] = {
    FinagleKafkaConsumerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-consumer")
      .groupId(KafkaGroupId("test-group"))
      .keyDeserializer(Serdes.String.deserializer)
      .valueDeserializer(Serdes.String.deserializer)
      .requestTimeout(Duration.fromSeconds(1))
  }

  private def buildTestProducerBuilder(): FinagleKafkaProducerBuilder[String, String] = {
    FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-producer")
      .ackMode(AckMode.ALL)
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
  }

  private def buildNativeTestProducer(): KafkaProducer[String, String] = {
    buildTestProducerBuilder().buildClient()
  }

  private def buildNativeTestConsumer(): KafkaConsumer[String, String] = {
    buildTestConsumerBuilder()
      .autoOffsetReset(OffsetResetStrategy.EARLIEST)
      .buildClient()
  }

  private def expectedProducerAnnotations: Seq[Annotation] = Seq(
    BinaryAnnotation("clnt/finagle.label", "kafka.producer"),
    BinaryAnnotation("clnt/finagle.version", Init.finagleVersion),
    BinaryAnnotation("clnt/namer.path", brokers.map(_.brokerList()).mkString(",")),
    BinaryAnnotation(ClientReqTraceKey, 6),
    BinaryAnnotation(ProducerTopicAnnotation, tracingTopic.topic),
    BinaryAnnotation(ProducerClientIdAnnotation, "test-producer"),
    Annotation.ServiceName("kafka.producer"),
    Annotation.Message(ProducerSendAnnotation),
    Annotation.ClientSend,
    Annotation.WireSend,
    Annotation.ClientRecv,
    Annotation.WireRecv,
    Annotation.Rpc("send")
  )

  private def expectedConsumerAnnotations(traceId: TraceId): Seq[Annotation] = Seq(
    BinaryAnnotation("clnt/finagle.label", "kafka.consumer"),
    BinaryAnnotation("clnt/finagle.version", Init.finagleVersion),
    BinaryAnnotation("clnt/namer.path", brokers.map(_.brokerList()).mkString(",")),
    BinaryAnnotation(ClientRepTraceKey, 6),
    BinaryAnnotation(ConsumerTopicAnnotation, tracingTopic.topic),
    BinaryAnnotation(ConsumerClientIdAnnotation, "test-consumer"),
    BinaryAnnotation(ConsumerGroupIdAnnotation, "test-group"),
    BinaryAnnotation(ProducerTraceIdAnnotation, traceId.toString),
    Annotation.ServiceName("kafka.consumer"),
    Annotation.Message(ConsumerPollAnnotation),
    Annotation.ClientRecv,
    Annotation.WireRecv,
    Annotation.Rpc("poll")
  )

  test(
    "consumer should record topic and producer information in the trace when actively tracing and producer trace id is debug"
  ) {
    val producerTraceId = {
      val id = Trace.nextId
      id.copy(flags = id.flags.setDebug)
    }
    testConsumerTracing(producerTraceId)
  }

  test(
    "consumer should record topic and producer information in the trace when actively tracing and producer trace id is sampled"
  ) {
    val producerTraceId = {
      val id = Trace.nextId
      id.copy(flags = id.flags.setFlags(Seq(Flags.SamplingKnown, Flags.Sampled)))
    }
    testConsumerTracing(producerTraceId)
  }

  test(
    "consumer should not record topic and producer information in the trace when not actively tracing"
  ) {
    val producerTraceId = Trace.nextId
    val nullTracer = spy(new NullTracer())

    producerTracingEnabled.let(true) {
      consumerTracingEnabled.let(true) {
        Trace.letTracer(nullTracer) {
          val producer = buildNativeTestProducer()
          val consumer = buildNativeTestConsumer()
          consumer.subscribe(Seq(tracingTopic.topic).asJava)
          try {
            Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, producerTraceId) {
              producer
                .send(new ProducerRecord(tracingTopic.topic, null, "Foo", "Bar"))
                .get(5, TimeUnit.SECONDS)
            }
            val records = consumer.poll(5.seconds.asJava)
            KafkaConsumerTracer.trace(records) {}
            records.asScala.flatMap(_.headers().asScala) should contain noElementsOf Seq(
              new RecordHeader(
                TracingKafkaProducer.TraceIdHeader,
                TraceId.serialize(producerTraceId.copy(_sampled = Some(false)))
              )
            )
          } finally {
            producer.close(5.seconds.asJava)
            consumer.close(5.seconds.asJava)
          }
        }
      }
    }

    verify(nullTracer, times(0)).record(any[Record])
  }

  test(
    "consumer should not record topic and producer information in the trace when producer and consumer tracing is disabled"
  ) {
    val producerTraceId = Trace.nextId
    val nullTracer = spy(new NullTracer())
    when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(true)

    producerTracingEnabled.let(false) {
      consumerTracingEnabled.let(false) {
        Trace.letTracer(nullTracer) {
          val producer = buildNativeTestProducer()
          val consumer = buildNativeTestConsumer()
          consumer.subscribe(Seq(tracingTopic.topic).asJava)
          try {
            Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, producerTraceId) {
              producer
                .send(new ProducerRecord(tracingTopic.topic, null, "Foo", "Bar"))
                .get(5, TimeUnit.SECONDS)
            }
            val records = consumer.poll(5.seconds.asJava)
            KafkaConsumerTracer.trace(records) {}
            records.asScala.flatMap(_.headers().asScala) should contain noElementsOf Seq(
              new RecordHeader(
                TracingKafkaProducer.TraceIdHeader,
                TraceId.serialize(producerTraceId.copy(_sampled = Some(false)))
              )
            )
          } finally {
            producer.close(5.seconds.asJava)
            consumer.close(5.seconds.asJava)
          }
        }
      }
    }

    verify(nullTracer, times(0)).record(any[Record])
  }

  private def testConsumerTracing(producerTraceId: TraceId): Unit = {
    val traceRecordsCaptor: ArgumentCaptor[Record] = ArgumentCaptor.forClass(classOf[Record])
    val nullTracer = spy(new NullTracer())

    producerTracingEnabled.let(true) {
      consumerTracingEnabled.let(true) {
        Trace.letTracer(nullTracer) {
          val producer = buildNativeTestProducer()
          val consumerConfigMap = buildTestConsumerBuilder().config.kafkaConsumerConfig.configMap
          val consumer = buildNativeTestConsumer()
          consumer.subscribe(Seq(tracingTopic.topic).asJava)
          try {
            // Always trace for producer
            when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(true)
            Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, producerTraceId) {
              producer
                .send(new ProducerRecord(tracingTopic.topic, null, "Foo", "Bar"))
                .get(5, TimeUnit.SECONDS)
            }
            // Answer based on sampling for consumer
            when(nullTracer.isActivelyTracing(any[TraceId]))
              .thenAnswer(new Answer[Boolean] { // kept for sbt compatibility
                override def answer(invocation: InvocationOnMock): Boolean = {
                  // compatibility with older mockito versions
                  invocation.getArguments()(0).asInstanceOf[TraceId].sampled match {
                    case Some(sampled) => sampled
                    case None => false
                  }
                }
              })
            val records = consumer.poll(5.seconds.asJava)
            KafkaConsumerTracer.trace(records, consumerConfigMap) {}
            records.asScala.flatMap(_.headers().asScala) should contain oneElementOf Seq(
              new RecordHeader(
                TracingKafkaProducer.TraceIdHeader,
                TraceId.serialize(producerTraceId)
              )
            )
          } finally {
            producer.close(5.seconds.asJava)
            consumer.close(5.seconds.asJava)
          }
        }
      }
    }

    verify(nullTracer, times(26)).record(traceRecordsCaptor.capture())
    val traceRecords = traceRecordsCaptor.getAllValues.asScala
    val expectedAnnotations =
      expectedProducerAnnotations ++ expectedConsumerAnnotations(producerTraceId)
    traceRecords.map(_.annotation) should contain theSameElementsAs expectedAnnotations
  }
}
