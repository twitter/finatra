package com.twitter.finatra.kafka.producers

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.stats.{NullStatsReceiver, Stat}
import com.twitter.finatra.kafka.producers.FinagleKafkaProducer.retryPolicy
import com.twitter.finatra.kafka.test.EmbeddedKafka
import com.twitter.util.{Await, Duration, Future, Throw, Try}
import java.util
import kafka.common.KafkaException
import org.apache.kafka.clients.producer.{
  BufferExhaustedException,
  Callback,
  Producer,
  ProducerConfig,
  ProducerRecord,
  RecordMetadata
}
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import org.apache.kafka.common.errors.TimeoutException
import org.apache.kafka.common.serialization.Serdes
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

class FinagleKafkaProducerTest extends EmbeddedKafka with MockitoSugar {

  val ignoredStat: Stat = NullStatsReceiver.stat()

  // retryable
  val timeoutException = new TimeoutException()
  val bufferExhaustedException = new BufferExhaustedException("")

  // non-retryable
  val exception = new Exception
  val kafkaException = new KafkaException("")

  val nonEmptyPartitionInfo: java.util.List[PartitionInfo] = {
    val list = new util.LinkedList[PartitionInfo]()
    list.add(mock[PartitionInfo])
    list
  }

  private def enqeueThenSend(
    producer: Producer[Unit, Unit],
    retryPolicy: RetryPolicy[Try[Future[RecordMetadata]]]
  ): Future[Future[RecordMetadata]] =
    FinagleKafkaProducer.enqueueThenSend(
      producer,
      retryPolicy,
      ignoredStat,
      ignoredStat,
      ignoredStat)(
      mock[ProducerRecord[Unit, Unit]]
    )

  test("retryPolicy - retries only the correct exceptions") {
    val policy = retryPolicy(1.milliseconds)
    assert(policy(Throw(timeoutException)).nonEmpty)
    assert(policy(Throw(bufferExhaustedException)).nonEmpty)
    assert(policy(Throw(exception)).isEmpty)
    assert(policy(Throw(kafkaException)).isEmpty)
  }

  test("enqueueThenSend - enqueuing fails due to missing metadata") {
    val mockProducer = mock[Producer[Unit, Unit]]
    val runtimeException = new RuntimeException

    when(mockProducer.partitionsFor(any[String])).thenThrow(runtimeException)
    assert(
      intercept[RuntimeException](
        Await.result(enqeueThenSend(mockProducer, retryPolicy(Duration.Zero)), 1.second)
      ) eq runtimeException
    )
  }

  test("enqueueThenSend - enqueuing fails") {
    val mockProducer = mock[Producer[Unit, Unit]]
    val runtimeException = new RuntimeException

    when(mockProducer.partitionsFor(any[String])).thenReturn(nonEmptyPartitionInfo)
    when(mockProducer.send(any[ProducerRecord[Unit, Unit]], any[Callback]))
      .thenThrow(runtimeException)
    assert(
      intercept[RuntimeException](
        Await.result(enqeueThenSend(mockProducer, retryPolicy(Duration.Zero)), 1.second)
      ) eq runtimeException
    )
  }

  test("enqueueThenSend - enqueue succeeds, sending fails") {
    val mockProducer = mock[Producer[Unit, Unit]]
    val callback: ArgumentCaptor[Callback] = ArgumentCaptor.forClass(classOf[Callback])

    when(mockProducer.partitionsFor(any[String])).thenReturn(nonEmptyPartitionInfo)
    when(mockProducer.send(any[ProducerRecord[Unit, Unit]], callback.capture())).thenReturn(null)

    val sent = Await.result(enqeueThenSend(mockProducer, retryPolicy(Duration.Zero)), 1.second)
    assert(!sent.isDefined)
    callback.getValue.onCompletion(null, exception)
    assert(intercept[Exception](Await.result(sent)) eq exception)
  }

  test("enqueueThenSend - enqueue succeeds, sending succeeds") {
    val mockProducer = mock[Producer[Unit, Unit]]
    val callback: ArgumentCaptor[Callback] = ArgumentCaptor.forClass(classOf[Callback])

    when(mockProducer.partitionsFor(any[String])).thenReturn(nonEmptyPartitionInfo)
    when(mockProducer.send(any[ProducerRecord[Unit, Unit]], callback.capture())).thenReturn(null)

    val metadata = new RecordMetadata(new TopicPartition("", 0), 0, 0, 0, 0, 0, 0)
    val sent = Await.result(enqeueThenSend(mockProducer, retryPolicy(Duration.Zero)), 1.second)
    assert(!sent.isDefined)
    callback.getValue.onCompletion(metadata, null)
    assert(Await.result(sent) eq metadata)
  }

  test(
    "`max.block.ms` is used for the max delay when retrying " +
      "and is set to 0 for the underlying client"
  ) {
    // minimum config necessary + maxBlock
    val producer = FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-producer")
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
      .maxBlock(1.second)
      .build()
    assert(producer.properties.getProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG) == "0")
    assert(producer.maxDelay == 1.second)
  }

  test("`max.block.ms` is defaulted to 1 minute and set to 0 for the underlying client") {
    // minimum config necessary + maxBlock
    val producer = FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .clientId("test-producer")
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
      .build()
    assert(producer.properties.getProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG) == "0")
    assert(producer.maxDelay == 1.minute)
  }
}
