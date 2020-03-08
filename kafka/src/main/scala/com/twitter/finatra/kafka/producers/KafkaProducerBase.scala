package com.twitter.finatra.kafka.producers

import com.twitter.inject.Logging
import com.twitter.util.{Closable, Future, Time}
import java.util
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.{PartitionInfo, TopicPartition}

/**
 * An interface for publishing events in key/value pairs to Kafka and
 * returning a [[com.twitter.util.Future]]
 *
 * @tparam K type of the key in key/value pairs to be published to Kafka
 * @tparam V type of the value in key/value pairs to be published to Kafka
 */
trait KafkaProducerBase[K, V] extends Closable with Logging {
  def send(
    topic: String,
    key: K,
    value: V,
    timestamp: Long,
    partitionIdx: Option[Integer] = None
  ): Future[RecordMetadata]

  def send(producerRecord: ProducerRecord[K, V]): Future[RecordMetadata]

  def initTransactions(): Unit

  def beginTransaction(): Unit

  def sendOffsetsToTransaction(
    offsets: Map[TopicPartition, OffsetAndMetadata],
    consumerGroupId: String
  ): Unit

  def commitTransaction(): Unit

  def abortTransaction(): Unit

  def flush(): Unit

  def partitionsFor(topic: String): util.List[PartitionInfo]

  def close(deadline: Time): Future[Unit]
}
