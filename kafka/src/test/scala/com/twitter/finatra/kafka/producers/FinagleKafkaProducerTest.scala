package com.twitter.finatra.kafka.producers

import com.twitter.finatra.kafka.test.EmbeddedKafka
import com.twitter.util.{Duration, Throw}
import com.twitter.util.mock.Mockito
import java.util
import kafka.common.KafkaException
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import org.apache.kafka.common.errors.TimeoutException
import org.apache.kafka.common.serialization.Serdes
import org.mockito.ArgumentCaptor
import scala.concurrent.duration._

class FinagleKafkaProducerTest extends EmbeddedKafka with Mockito {

  // retryable
  val timeoutException = new TimeoutException

  // non-retryable
  val exception = new Exception
  val runtimeException = new RuntimeException
  val kafkaException = new KafkaException("")

  val nonEmptyPartitionInfo: java.util.List[PartitionInfo] = {
    val list = new util.LinkedList[PartitionInfo]()
    list.add(mock[PartitionInfo])
    list
  }

  val producerRecord: ProducerRecord[String, String] =
    new ProducerRecord[String, String]("test-topic", null, System.currentTimeMillis, "Foo", "Bar")

  val recordMetadata = new RecordMetadata(new TopicPartition("", 0), 0, 0, 0, 0, 0, 0)

  var mockProducer: KafkaProducer[String, String] = _
  var producerConfigs: FinagleKafkaProducerConfig[String, String] = _
  var producer: FinagleKafkaProducer[String, String] = _
  var callbackCaptor: ArgumentCaptor[Callback] = _

  override def beforeEach(): Unit = {
    mockProducer = mock[KafkaProducer[String, String]]
    producerConfigs = FinagleKafkaProducerConfig[String, String]()
    producer = new FinagleKafkaProducer(mockProducer, producerConfigs)
    callbackCaptor = ArgumentCaptor.forClass(classOf[Callback])
  }

  override def afterEach(): Unit = {
    producer.close()
  }

  test("retryPolicy - retries only the correct exceptions") {
    val policy = FinagleKafkaProducer.retryPolicy(Duration.fromMilliseconds(1))
    assert(policy(Throw(timeoutException)).nonEmpty)
    assert(policy(Throw(exception)).isEmpty)
    assert(policy(Throw(kafkaException)).isEmpty)
  }

  test("send - retryable exceptions due to missing record metadata get retried") {
    mockProducer.partitionsFor(any[String]) throws timeoutException andThen nonEmptyPartitionInfo

    val sendResult = producer.send(producerRecord)
    // timeout needed because `producer.send` is async
    mockProducer
      .partitionsFor(any[String]) wasCalled (twice within 500.milliseconds)
    mockProducer.send(any[ProducerRecord[String, String]], callbackCaptor.capture()) wasCalled once
    callbackCaptor.getValue.onCompletion(recordMetadata, null)
    assert(await(sendResult) eq recordMetadata)
    mockProducer wasNever calledAgain
  }

  test("send - retryable exceptions during enqueuing get retried") {
    mockProducer.partitionsFor(any[String]) returns nonEmptyPartitionInfo
    mockProducer.send(
      any[ProducerRecord[String, String]],
      any[Callback]) throws timeoutException andThen null

    val sendResult = producer.send(producerRecord)
    mockProducer
      .partitionsFor(any[String]) wasCalled (twice within 500.milliseconds)
    mockProducer.send(any[ProducerRecord[String, String]], callbackCaptor.capture()) wasCalled twice
    callbackCaptor.getValue.onCompletion(recordMetadata, null)
    assert(await(sendResult) eq recordMetadata)
    mockProducer wasNever calledAgain
  }

  test("send - non-retryable exceptions during enqueuing do not get retried") {
    mockProducer.partitionsFor(any[String]) returns nonEmptyPartitionInfo
    mockProducer.send(any[ProducerRecord[String, String]], any[Callback]) throws runtimeException

    val sendResult = producer.send(producerRecord)
    mockProducer
      .partitionsFor(any[String]) wasCalled (once within 500.milliseconds)
    mockProducer.send(any[ProducerRecord[String, String]], any[Callback]) wasCalled once
    assert(intercept[Exception](await(sendResult)) eq runtimeException)
    mockProducer wasNever calledAgain
  }

  test("send - retryable exceptions during sending get retried") {
    mockProducer.partitionsFor(any[String]) returns nonEmptyPartitionInfo

    val sendResult = producer.send(producerRecord)
    mockProducer
      .partitionsFor(any[String]) wasCalled (once within 500.milliseconds)
    mockProducer.send(any[ProducerRecord[String, String]], callbackCaptor.capture()) wasCalled once
    callbackCaptor.getValue.onCompletion(null, timeoutException)

    mockProducer
      .partitionsFor(any[String]) wasCalled (twice within 500.milliseconds)
    mockProducer.send(any[ProducerRecord[String, String]], callbackCaptor.capture()) wasCalled twice
    assert(callbackCaptor.getAllValues.size() == 3)
    callbackCaptor.getValue.onCompletion(recordMetadata, null)

    assert(await(sendResult) eq recordMetadata)
    mockProducer wasNever calledAgain
  }

  test("send - non-retryable exceptions during sending do not get retried") {
    mockProducer.partitionsFor(any[String]) returns nonEmptyPartitionInfo

    val sendResult = producer.send(producerRecord)
    mockProducer
      .partitionsFor(any[String]) wasCalled (once within 500.milliseconds)
    mockProducer.send(any[ProducerRecord[String, String]], callbackCaptor.capture()) wasCalled once
    callbackCaptor.getValue.onCompletion(null, runtimeException)
    assert(intercept[Exception](await(sendResult)) eq runtimeException)
    mockProducer wasNever calledAgain
  }

  test("send - there are no retries when enqueuing and sending succeed") {
    mockProducer.partitionsFor(any[String]) returns nonEmptyPartitionInfo

    val sendResult = producer.send(producerRecord)
    mockProducer
      .partitionsFor(any[String]) wasCalled (once within 500.milliseconds)
    mockProducer.send(any[ProducerRecord[String, String]], callbackCaptor.capture()) wasCalled once
    callbackCaptor.getValue.onCompletion(recordMetadata, null)
    assert(await(sendResult) eq recordMetadata)
    mockProducer wasNever calledAgain
  }

  test("`max.block.ms` is used for the max delay when retrying ") {
    // minimum config necessary + maxBlock
    val producer = FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-producer")
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
      .maxBlock(Duration.fromSeconds(1))
      .build()
    assert(producer.maxDelay == Duration.fromSeconds(1))
  }

  test("`max.block.ms` is defaulted to 1 minute and used for the max delay when retrying") {
    // minimum config necessary
    val producer = FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-producer")
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
      .build()
    assert(producer.maxDelay == Duration.fromMinutes(1))
  }
}
