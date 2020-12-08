package com.twitter.finatra.kafka.test.integration

import com.twitter.finagle.Init
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.filter.PayloadSizeFilter.{ClientRepTraceKey, ClientReqTraceKey}
import com.twitter.finagle.tracing.Annotation.BinaryAnnotation
import com.twitter.finagle.tracing.{Annotation, Flags, NullTracer, Record, Trace, TraceId}
import com.twitter.finatra.kafka.consumers.TracingKafkaConsumer.{
  ConsumerGroupIdAnnotation,
  ConsumerPollAnnotation,
  ConsumerTopicAnnotation,
  ProducerTraceIdAnnotation,
  ClientIdAnnotation => ConsumerClientIdAnnotation
}
import com.twitter.finatra.kafka.consumers.{
  FinagleKafkaConsumer,
  FinagleKafkaConsumerBuilder,
  consumerTracingEnabled
}
import com.twitter.finatra.kafka.domain.{AckMode, KafkaGroupId}
import com.twitter.finatra.kafka.producers.TracingKafkaProducer.{
  ProducerSendAnnotation,
  ProducerTopicAnnotation,
  ClientIdAnnotation => ProducerClientIdAnnotation
}
import com.twitter.finatra.kafka.producers.{
  FinagleKafkaProducer,
  FinagleKafkaProducerBuilder,
  TracingKafkaProducer,
  producerTracingEnabled
}
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.finatra.kafka.test.EmbeddedKafka
import com.twitter.inject.InMemoryStatsReceiverUtility
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.util.{Duration, Time}
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.consumer.{KafkaConsumer, OffsetResetStrategy}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.Serdes
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import scala.collection.JavaConverters._

class FinagleKafkaConsumerIntegrationTest extends EmbeddedKafka {
  private val testTopic = kafkaTopic(Serdes.String, Serdes.String, "test-topic")
  private val tracingTopic = kafkaTopic(Serdes.String, Serdes.String, "tracing-topic")
  private val emptyTestTopic = kafkaTopic(Serdes.String, Serdes.String, "empty-test-topic")

  private def buildTestConsumerBuilder(): FinagleKafkaConsumerBuilder[String, String] = {
    FinagleKafkaConsumerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-consumer")
      .groupId(KafkaGroupId("test-group"))
      .keyDeserializer(Serdes.String.deserializer)
      .valueDeserializer(Serdes.String.deserializer)
      .requestTimeout(Duration.fromSeconds(1))
  }

  private def buildTestConsumer(): FinagleKafkaConsumer[String, String] = {
    buildTestConsumerBuilder().build()
  }

  private def buildTestProducerBuilder(): FinagleKafkaProducerBuilder[String, String] = {
    FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-producer")
      .ackMode(AckMode.ALL)
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
  }

  private def buildTestProducer(): FinagleKafkaProducer[String, String] = {
    buildTestProducerBuilder
      .build()
  }

  private def buildNativeTestProducer(): KafkaProducer[String, String] = {
    buildTestProducerBuilder
      .buildClient()
  }

  private def buildNativeTestConsumer(
    offsetResetStrategy: OffsetResetStrategy = OffsetResetStrategy.NONE
  ): KafkaConsumer[String, String] = {
    buildTestConsumerBuilder()
      .autoOffsetReset(offsetResetStrategy)
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

  test("endOffset returns 0 for empty topic with no events") {
    val consumer = buildTestConsumer()
    val emptyTopicPartition = new TopicPartition(emptyTestTopic.topic, 0)
    try {
      assert(await(consumer.endOffset(emptyTopicPartition)) == 0)
    } finally {
      await(consumer.close())
    }
  }

  test("endOffset increases by 1 after publish with native consumer") {
    val producer = buildTestProducer()
    val consumer = buildNativeTestConsumer()
    val topicPartition = new TopicPartition(testTopic.topic, 0)
    val initEndOffset = consumer.endOffsets(List(topicPartition).asJava).get(topicPartition)

    try {
      await(producer.send(testTopic.topic, "Foo", "Bar", System.currentTimeMillis))
      val finishEndOffset = consumer.endOffsets(List(topicPartition).asJava).get(topicPartition)
      assert(finishEndOffset == initEndOffset + 1)
    } finally {
      await(producer.close())
    }
  }

  test("endOffset increases by 1 after publish") {
    val producer = buildTestProducer()
    val consumer = buildTestConsumer()
    val topicPartition = new TopicPartition(testTopic.topic, 0)
    val initEndOffset = await(consumer.endOffset(topicPartition))

    try {
      await(producer.send(testTopic.topic, "Foo", "Bar", System.currentTimeMillis))
      assert(await(consumer.endOffset(topicPartition)) == initEndOffset + 1)
    } finally {
      await(producer.close())
      await(consumer.close())
    }
  }

  test("endOffset increases by 3 after 3 publishes") {
    val producer = buildTestProducer()
    val consumer = buildTestConsumer()
    val topicPartition = new TopicPartition(testTopic.topic, 0)
    val initEndOffset = await(consumer.endOffset(topicPartition))

    try {
      await(producer.send(testTopic.topic, "Fee", "Bee", System.currentTimeMillis))
      await(producer.send(testTopic.topic, "Fi", "Bye", System.currentTimeMillis))
      await(producer.send(testTopic.topic, "Foo", "Boo", System.currentTimeMillis))
      assert(await(consumer.endOffset(topicPartition)) == initEndOffset + 3)
    } finally {
      await(producer.close())
      await(consumer.close())
    }
  }

  test("endOffsets returns empty map for empty sequence of partitions") {
    val consumer = buildTestConsumer()
    val emptyEndOffsets = await(consumer.endOffsets(Seq.empty[TopicPartition]))
    try {
      assert(emptyEndOffsets.size == 0)
    } finally {
      await(consumer.close())
    }
  }

  test("endOffsets times out for non-existent topic") {
    val consumer = buildTestConsumer()
    val notExistTopicPartition = new TopicPartition("topic-does-not-exist", 0)
    try {
      assertThrows[org.apache.kafka.common.errors.TimeoutException](
        await(consumer.endOffsets(Seq(notExistTopicPartition))))
    } finally {
      await(consumer.close())
    }
  }

  test("endOffset times out for non-existent topic") {
    val consumer = buildTestConsumer()
    val notExistTopicPartition = new TopicPartition("topic-does-not-exist", 0)
    try {
      assertThrows[org.apache.kafka.common.errors.TimeoutException](
        await(consumer.endOffset(notExistTopicPartition)))
    } finally {
      await(consumer.close())
    }
  }

  test("partition stats recorded by default") {
    val injector = TestInjector(InMemoryStatsReceiverModule).create
    KafkaFinagleMetricsReporter.init(injector)

    val producer = buildNativeTestProducer()
    val consumer = buildTestConsumerBuilder().buildClient()

    try {
      producer
        .send(new ProducerRecord(testTopic.topic, null, "Foo", "Bar"))
        .get(5, TimeUnit.SECONDS)
      consumer.subscribe(Seq(testTopic.topic).asJava)
      consumer.poll(java.time.Duration.ofSeconds(5))

      val statsUtils = InMemoryStatsReceiverUtility(injector)
      statsUtils.gauges.assert("kafka/test_consumer/test_topic/0/records_lag_avg", 0.0f)
    } finally {
      producer.close(java.time.Duration.ofSeconds(5))
      consumer.close(java.time.Duration.ofSeconds(5))
    }
  }

  test("partition stats not recorded when includePartitionMetrics false") {
    val injector = TestInjector(InMemoryStatsReceiverModule).create
    KafkaFinagleMetricsReporter.init(injector)

    val producer = buildNativeTestProducer()
    val consumer = buildTestConsumerBuilder()
      .includePartitionMetrics(false)
      .buildClient()

    try {
      producer
        .send(new ProducerRecord(testTopic.topic, null, "Foo", "Bar"))
        .get(5, TimeUnit.SECONDS)
      consumer.subscribe(Seq(testTopic.topic).asJava)
      consumer.poll(java.time.Duration.ofSeconds(5))

      val statsUtils = InMemoryStatsReceiverUtility(injector)
      statsUtils.gauges.get("kafka/test_consumer/test_topic/0/records_lag_avg") shouldBe None
    } finally {
      producer.close(java.time.Duration.ofSeconds(5))
      consumer.close(java.time.Duration.ofSeconds(5))
    }
  }

  test("close with Time.Bottom deadline should not throw exception") {
    val consumer = buildTestConsumer()
    await(consumer.close(Time.Bottom))
  }

  test("close with valid deadline should not throw exception") {
    val consumer = buildTestConsumer()
    await(consumer.close(Time.now + Duration.fromSeconds(10)))
  }

  test(
    "consumer should record topic and producer information in the trace when actively tracing and producer trace id is debug") {
    val producerTraceId = {
      val id = Trace.nextId
      id.copy(flags = id.flags.setDebug)
    }
    testConsumerTracing(producerTraceId)
  }

  test(
    "consumer should record topic and producer information in the trace when actively tracing and producer trace id is sampled") {
    val producerTraceId = {
      val id = Trace.nextId
      id.copy(flags = id.flags.setFlags(Seq(Flags.SamplingKnown, Flags.Sampled)))
    }
    testConsumerTracing(producerTraceId)
  }

  test(
    "consumer should not record topic and producer information in the trace when not actively tracing") {
    val producerTraceId = Trace.nextId
    val nullTracer = spy(new NullTracer())

    producerTracingEnabled.let(true) {
      consumerTracingEnabled.let(true) {
        Trace.letTracer(nullTracer) {
          val producer = buildNativeTestProducer()
          val consumer = buildNativeTestConsumer(OffsetResetStrategy.EARLIEST)
          consumer.subscribe(Seq(tracingTopic.topic).asJava)
          try {
            Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, producerTraceId) {
              producer
                .send(new ProducerRecord(tracingTopic.topic, null, "Foo", "Bar"))
                .get(5, TimeUnit.SECONDS)
            }
            val records = consumer.poll(java.time.Duration.ofSeconds(5))
            records.asScala.flatMap(_.headers().asScala) should contain noElementsOf Seq(
              new RecordHeader(
                TracingKafkaProducer.TraceIdHeader,
                TraceId.serialize(producerTraceId.copy(_sampled = Some(false)))))
          } finally {
            producer.close(java.time.Duration.ofSeconds(5))
            consumer.close(java.time.Duration.ofSeconds(5))
          }
        }
      }
    }

    verify(nullTracer, times(0)).record(any[Record])
  }

  test(
    "consumer should not record topic and producer information in the trace when producer and consumer tracing is disabled") {
    val producerTraceId = Trace.nextId
    val nullTracer = spy(new NullTracer())
    when(nullTracer.isActivelyTracing(any[TraceId])).thenReturn(true)

    producerTracingEnabled.let(false) {
      consumerTracingEnabled.let(false) {
        Trace.letTracer(nullTracer) {
          val producer = buildNativeTestProducer()
          val consumer = buildNativeTestConsumer(OffsetResetStrategy.EARLIEST)
          consumer.subscribe(Seq(tracingTopic.topic).asJava)
          try {
            Contexts.local.let(TracingKafkaProducer.TestTraceIdKey, producerTraceId) {
              producer
                .send(new ProducerRecord(tracingTopic.topic, null, "Foo", "Bar"))
                .get(5, TimeUnit.SECONDS)
            }
            val records = consumer.poll(java.time.Duration.ofSeconds(5))
            records.asScala.flatMap(_.headers().asScala) should contain noElementsOf Seq(
              new RecordHeader(
                TracingKafkaProducer.TraceIdHeader,
                TraceId.serialize(producerTraceId.copy(_sampled = Some(false)))))
          } finally {
            producer.close(java.time.Duration.ofSeconds(5))
            consumer.close(java.time.Duration.ofSeconds(5))
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
          val consumer = buildNativeTestConsumer(OffsetResetStrategy.EARLIEST)
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
            val records = consumer.poll(java.time.Duration.ofSeconds(5))
            records.asScala.flatMap(_.headers().asScala) should contain oneElementOf Seq(
              new RecordHeader(
                TracingKafkaProducer.TraceIdHeader,
                TraceId.serialize(producerTraceId)))
          } finally {
            producer.close(java.time.Duration.ofSeconds(5))
            consumer.close(java.time.Duration.ofSeconds(5))
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
