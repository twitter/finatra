package com.twitter.finatra.kafka.test.integration

import com.twitter.finatra.kafka.consumers.FinagleKafkaConsumerBuilder
import com.twitter.finatra.kafka.domain.{AckMode, KafkaGroupId}
import com.twitter.finatra.kafka.producers.FinagleKafkaProducerBuilder
import com.twitter.finatra.kafka.test.EmbeddedKafka
import com.twitter.util.{Duration, Time}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Serdes

class FinagleKafkaConsumerIntegrationTest extends EmbeddedKafka {
  private val testTopic = kafkaTopic(Serdes.String, Serdes.String, "test-topic")
  private val emptyTestTopic = kafkaTopic(Serdes.String, Serdes.String, "empty-test-topic")

  private def getTestConsumer() = {
    FinagleKafkaConsumerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-consumer")
      .groupId(KafkaGroupId("test-group"))
      .keyDeserializer(Serdes.String.deserializer)
      .valueDeserializer(Serdes.String.deserializer)
      .requestTimeout(Duration.fromSeconds(1))
      .build()
  }

  private def getTestProducer() = {
    FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-producer")
      .ackMode(AckMode.ALL)
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
      .build()
  }

  test("endOffset returns 0 for empty topic with no events") {
    val consumer = getTestConsumer()
    val emptyTopicPartition = new TopicPartition(emptyTestTopic.topic, 0)
    try {
      assert(await(consumer.endOffset(emptyTopicPartition)) == 0)
    } finally {
      await(consumer.close())
    }
  }

  test("endOffset increases by 1 after publish") {
    val producer = getTestProducer()
    val consumer = getTestConsumer()
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
    val producer = getTestProducer()
    val consumer = getTestConsumer()
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
    val consumer = getTestConsumer()
    val emptyEndOffsets = await(consumer.endOffsets(Seq.empty[TopicPartition]))
    try {
      assert(emptyEndOffsets.size == 0)
    } finally {
      await(consumer.close())
    }
  }

  test("endOffsets times out for non-existent topic") {
    val consumer = getTestConsumer()
    val notExistTopicPartition = new TopicPartition("topic-does-not-exist", 0)
    try {
      assertThrows[org.apache.kafka.common.errors.TimeoutException](
        await(consumer.endOffsets(Seq(notExistTopicPartition))))
    } finally {
      await(consumer.close())
    }
  }

  test("endOffset times out for non-existent topic") {
    val consumer = getTestConsumer()
    val notExistTopicPartition = new TopicPartition("topic-does-not-exist", 0)
    try {
      assertThrows[org.apache.kafka.common.errors.TimeoutException](
        await(consumer.endOffset(notExistTopicPartition)))
    } finally {
      await(consumer.close())
    }
  }

  test("close with Time.Bottom deadline should not throw exception") {
    val consumer = getTestConsumer()
    await(consumer.close(Time.Bottom))
  }

  test("close with valid deadline should not throw exception") {
    val consumer = getTestConsumer()
    await(consumer.close(Time.now + Duration.fromSeconds(10)))
  }

}
