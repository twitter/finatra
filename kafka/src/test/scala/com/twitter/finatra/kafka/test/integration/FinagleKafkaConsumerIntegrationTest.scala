package com.twitter.finatra.kafka.test.integration

import com.twitter.finatra.kafka.consumers.{FinagleKafkaConsumer, FinagleKafkaConsumerBuilder}
import com.twitter.finatra.kafka.domain.{AckMode, KafkaGroupId}
import com.twitter.finatra.kafka.producers.{FinagleKafkaProducer, FinagleKafkaProducerBuilder}
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
import org.apache.kafka.common.serialization.Serdes
import scala.collection.JavaConverters._

class FinagleKafkaConsumerIntegrationTest extends EmbeddedKafka {
  private val testTopic = kafkaTopic(Serdes.String, Serdes.String, "test-topic")
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
    buildTestProducerBuilder().build()
  }

  private def buildNativeTestProducer(): KafkaProducer[String, String] = {
    buildTestProducerBuilder().buildClient()
  }

  private def buildNativeTestConsumer(
    offsetResetStrategy: OffsetResetStrategy = OffsetResetStrategy.NONE
  ): KafkaConsumer[String, String] = {
    buildTestConsumerBuilder()
      .autoOffsetReset(offsetResetStrategy)
      .buildClient()
  }

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
}
