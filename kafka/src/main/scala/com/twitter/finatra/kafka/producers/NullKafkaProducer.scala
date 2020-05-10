package com.twitter.finatra.kafka.producers
import com.twitter.util.{Future, Time}
import java.util
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.{PartitionInfo, TopicPartition}

/**
 * A no-op [[KafkaProducerBase]]. No network connection is created and
 * events are discarded, making this producer useful in unit tests
 * and as defaults in situations where event publishing is not needed.
 *
 * @tparam K type of the key in key/value pairs to be published to Kafka
 * @tparam V type of the value in key/value pairs to be published to Kafka
 */
class NullKafkaProducer[K, V] extends KafkaProducerBase[K, V] {

  val DataRecord = new RecordMetadata(
    new TopicPartition("", 0),
    0L,
    0L,
    0L,
    0L,
    0,
    0
  )

  val EmptyList = new util.ArrayList[PartitionInfo]()

  def send(
    topic: String,
    key: K,
    value: V,
    timestamp: Long,
    partitionIdx: Option[Integer]
  ): Future[RecordMetadata] = Future.value(DataRecord)

  def send(producerRecord: ProducerRecord[K, V]): Future[RecordMetadata] =
    Future.value(DataRecord)

  def initTransactions(): Unit = ()

  def beginTransaction(): Unit = ()

  def sendOffsetsToTransaction(
    offsets: Map[TopicPartition, OffsetAndMetadata],
    consumerGroupId: String
  ): Unit = ()

  def commitTransaction(): Unit = ()

  def abortTransaction(): Unit = ()

  def flush(): Unit = ()

  def partitionsFor(topic: String): util.List[PartitionInfo] =
    EmptyList

  def close(deadline: Time): Future[Unit] = Future.Unit
}
