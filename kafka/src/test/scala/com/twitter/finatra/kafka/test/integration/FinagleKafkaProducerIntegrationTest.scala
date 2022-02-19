package com.twitter.finatra.kafka.test.integration

import com.twitter.finagle.Init
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.filter.PayloadSizeFilter.ClientReqTraceKey
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.tracing.Annotation.BinaryAnnotation
import com.twitter.finagle.tracing.Annotation
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.tracing.Record
import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.tracing.TraceId
import com.twitter.finatra.kafka.domain.AckMode
import com.twitter.finatra.kafka.producers.TracingKafkaProducer.TraceIdHeader
import com.twitter.finatra.kafka.producers.KafkaProducerTraceAnnotatorImpl.ClientIdAnnotation
import com.twitter.finatra.kafka.producers.KafkaProducerTraceAnnotatorImpl.ProducerSendAnnotation
import com.twitter.finatra.kafka.producers.KafkaProducerTraceAnnotatorImpl.ProducerTopicAnnotation
import com.twitter.finatra.kafka.producers.FinagleKafkaProducerBuilder
import com.twitter.finatra.kafka.producers.TracingKafkaProducer
import com.twitter.finatra.kafka.producers.producerTracingEnabled
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.finatra.kafka.test.EmbeddedKafka
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.inject.InMemoryStatsReceiverUtility
import com.twitter.util.Duration.fromMilliseconds
import com.twitter.util.Duration
import com.twitter.util.Time
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import scala.collection.JavaConverters._

class FinagleKafkaProducerIntegrationTest extends EmbeddedKafka {

  private val testTopic = kafkaTopic(Serdes.String, Serdes.String, "test-topic")

  private def getTestProducerBuilder(statsReceiver: StatsReceiver) = {
    FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .statsReceiver(statsReceiver)
      .clientId("test-producer")
      .ackMode(AckMode.ALL)
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
      .transactionalId(null) // test null string config causing non impact
  }

  private def getTestProducer(statsReceiver: StatsReceiver) = {
    getTestProducerBuilder(statsReceiver).build()
  }

  private def getNativeTestProducer(statsReceiver: StatsReceiver) = {
    getTestProducerBuilder(statsReceiver).buildClient()
  }

  test("close with Time.Bottom deadline should not throw exception") {
    val producer = getTestProducer(NullStatsReceiver)
    await(producer.close(Time.Bottom))
  }

  test("close with valid deadline should not throw exception") {
    val producer = getTestProducer(NullStatsReceiver)
    await(producer.close(Time.now + Duration.fromSeconds(10)))
  }

  test("native producer should record topic in the trace when actively tracing") {
    val producerRecord = new ProducerRecord[String, String](
      testTopic.topic,
      null,
      System.currentTimeMillis,
      "Foo",
      "Bar")
    val recordsCaptor: ArgumentCaptor[Record] = ArgumentCaptor.forClass(classOf[Record])
    val nullTracer = spy(new NullTracer())
    when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(true)
    val testTraceId = Trace.nextId.copy(_sampled = Some(true))

    producerTracingEnabled.let(true) {
      Trace.letTracer(nullTracer) {
        Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, testTraceId) {
          val producer = getNativeTestProducer(NullStatsReceiver)
          try {
            val resultFuture = producer.send(producerRecord)
            resultFuture.get(5, TimeUnit.SECONDS)
          } finally {
            producer.close(java.time.Duration.ofSeconds(5))
          }
        }
      }
    }

    val traceIdHeader = producerRecord.headers.asScala.find(_.key() == TraceIdHeader)
    traceIdHeader should not be None
    traceIdHeader.value.value() shouldEqual TraceId.serialize(testTraceId)

    verify(nullTracer, times(13)).record(recordsCaptor.capture())
    val records = recordsCaptor.getAllValues.asScala
    val expectedAnnotations = Seq(
      BinaryAnnotation("clnt/finagle.label", "kafka.producer"),
      BinaryAnnotation("clnt/finagle.version", Init.finagleVersion),
      BinaryAnnotation("clnt/namer.path", brokers.map(_.brokerList()).mkString(",")),
      BinaryAnnotation(ClientReqTraceKey, 6),
      BinaryAnnotation(ProducerTopicAnnotation, testTopic.topic),
      BinaryAnnotation(ClientIdAnnotation, "test-producer"),
      Annotation.ServiceName("kafka.producer"),
      Annotation.Message(ProducerSendAnnotation),
      Annotation.ClientSend,
      Annotation.WireSend,
      Annotation.ClientRecv,
      Annotation.WireRecv,
      Annotation.Rpc("send")
    )
    records.map(_.annotation) should contain theSameElementsAs expectedAnnotations
  }

  test("native producer should not record topic in the trace when not actively tracing") {
    val producerRecord = new ProducerRecord[String, String](
      testTopic.topic,
      null,
      System.currentTimeMillis,
      "Foo",
      "Bar")
    val nullTracer = spy(new NullTracer())
    when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(false)
    val testTraceId = Trace.nextId

    producerTracingEnabled.let(true) {
      Trace.letTracer(nullTracer) {
        Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, testTraceId) {
          val producer = getNativeTestProducer(NullStatsReceiver)
          try {
            val resultFuture = producer.send(producerRecord)
            resultFuture.get(5, TimeUnit.SECONDS)
          } finally {
            producer.close(java.time.Duration.ofSeconds(5))
          }
        }
      }
    }

    val traceIdHeader = producerRecord.headers.asScala.find(_.key() == TraceIdHeader)
    traceIdHeader shouldBe None

    verify(nullTracer, times(0)).record(any[Record])
  }

  test("native producer should not record topic in the trace when tracingEnabled is false") {
    val producerRecord = new ProducerRecord[String, String](
      testTopic.topic,
      null,
      System.currentTimeMillis,
      "Foo",
      "Bar")
    val nullTracer = spy(new NullTracer())
    when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(true)
    val testTraceId = Trace.nextId

    producerTracingEnabled.let(false) {
      Trace.letTracer(nullTracer) {
        Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, testTraceId) {
          val producer = getNativeTestProducer(NullStatsReceiver)
          try {
            val resultFuture = producer.send(producerRecord)
            resultFuture.get(5, TimeUnit.SECONDS)
          } finally {
            producer.close(java.time.Duration.ofSeconds(5))
          }
        }
      }
    }

    val traceIdHeader = producerRecord.headers.asScala.find(_.key() == TraceIdHeader)
    traceIdHeader shouldBe None

    verify(nullTracer, times(0)).record(any[Record])
  }

  test("producer should record topic in the trace when actively tracing") {
    val producerRecord = new ProducerRecord[String, String](
      testTopic.topic,
      null,
      System.currentTimeMillis,
      "Foo",
      "Bar")
    val recordsCaptor: ArgumentCaptor[Record] = ArgumentCaptor.forClass(classOf[Record])
    val nullTracer = spy(new NullTracer())
    when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(true)
    val testTraceId = Trace.nextId.copy(_sampled = Some(true))

    producerTracingEnabled.let(true) {
      Trace.letTracer(nullTracer) {
        Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, testTraceId) {
          val producer = getTestProducer(NullStatsReceiver)
          try {
            await(producer.send(producerRecord))
          } finally {
            await(producer.close())
          }
        }
      }
    }

    val traceIdHeader = producerRecord.headers.asScala.find(_.key() == TraceIdHeader)
    traceIdHeader should not be None
    traceIdHeader.value.value() shouldEqual TraceId.serialize(testTraceId)

    verify(nullTracer, times(13)).record(recordsCaptor.capture())
    val records = recordsCaptor.getAllValues.asScala
    val expectedAnnotations = Seq(
      BinaryAnnotation("clnt/finagle.label", "kafka.producer"),
      BinaryAnnotation("clnt/finagle.version", Init.finagleVersion),
      BinaryAnnotation("clnt/namer.path", brokers.map(_.brokerList()).mkString(",")),
      BinaryAnnotation(ClientReqTraceKey, 6),
      BinaryAnnotation(ProducerTopicAnnotation, testTopic.topic),
      BinaryAnnotation(ClientIdAnnotation, "test-producer"),
      Annotation.ServiceName("kafka.producer"),
      Annotation.Message(ProducerSendAnnotation),
      Annotation.ClientSend,
      Annotation.WireSend,
      Annotation.ClientRecv,
      Annotation.WireRecv,
      Annotation.Rpc("send")
    )
    records.map(_.annotation) should contain theSameElementsAs expectedAnnotations
  }

  test("producer should not record topic in the trace when not actively tracing") {
    val producerRecord = new ProducerRecord[String, String](
      testTopic.topic,
      null,
      System.currentTimeMillis,
      "Foo",
      "Bar")
    val nullTracer = spy(new NullTracer())
    when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(false)
    val testTraceId = Trace.nextId

    producerTracingEnabled.let(true) {
      Trace.letTracer(nullTracer) {
        Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, testTraceId) {
          val producer = getTestProducer(NullStatsReceiver)
          try {
            await(producer.send(producerRecord))
          } finally {
            await(producer.close())
          }
        }
      }
    }

    val traceIdHeader = producerRecord.headers.asScala.find(_.key() == TraceIdHeader)
    traceIdHeader shouldBe None

    verify(nullTracer, times(0)).record(any[Record])
  }

  test("producer should not record topic in the trace when tracingEnabled is false") {
    val producerRecord = new ProducerRecord[String, String](
      testTopic.topic,
      null,
      System.currentTimeMillis,
      "Foo",
      "Bar")
    val nullTracer = spy(new NullTracer())
    when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(false)
    val testTraceId = Trace.nextId

    producerTracingEnabled.let(false) {
      Trace.letTracer(nullTracer) {
        Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, testTraceId) {
          val producer = getTestProducer(NullStatsReceiver)
          try {
            await(producer.send(producerRecord))
          } finally {
            await(producer.close())
          }
        }
      }
    }

    val traceIdHeader = producerRecord.headers.asScala.find(_.key() == TraceIdHeader)
    traceIdHeader shouldBe None

    verify(nullTracer, times(0)).record(any[Record])
  }

  // NOTE: this test shuts down the embedded kafka cluster so we need to make sure
  //       it's run last or else it clobbers other tests.
  test("success then failure publish") {
    val injector = TestInjector(InMemoryStatsReceiverModule).create
    KafkaFinagleMetricsReporter.init(injector)

    val producer = getTestProducerBuilder(injector.instance[StatsReceiver])
      .requestTimeout(fromMilliseconds(500))
      .deliveryTimeout(fromMilliseconds(1000))
      .build()

    try {
      await(producer.send("test-topic", "Foo", "Bar", System.currentTimeMillis))

      val statsUtils = InMemoryStatsReceiverUtility(injector)

      statsUtils.gauges.assert("kafka/test_producer/record_send_total", 1.0f)
      val onSendLag = statsUtils.stats("kafka/test_producer/record_timestamp_on_send_lag")
      assert(onSendLag.size == 1)
      assert(onSendLag.head >= 0)

      val onSuccessLag = statsUtils.stats("kafka/test_producer/record_timestamp_on_success_lag")
      assert(onSuccessLag.size == 1)
      assert(onSuccessLag.head >= onSendLag.head)

      /* Stop the brokers so that the next publish attempt results in publish error */
      closeEmbeddedKafka()

      intercept[org.apache.kafka.common.errors.TimeoutException] {
        await(producer.send("test-topic", "Hello", "World", System.currentTimeMillis))
      }

      statsUtils.gauges.assert("kafka/test_producer/record_error_total", 1.0f)
      val onFailureLag = statsUtils.stats("kafka/test_producer/record_timestamp_on_failure_lag")
      assert(onFailureLag.size == 1)
      assert(onFailureLag.head >= 0)
    } finally {
      producer.close()
    }
  }
}
