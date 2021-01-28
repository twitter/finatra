package com.twitter.finatra.kafka.producers

import com.twitter.finagle.stats.Stat
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter.sanitizeMetricName
import com.twitter.util._
import java.util
import java.util.concurrent.TimeUnit._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import scala.collection.JavaConverters._

/**
 * A standard implementation of [[KafkaProducerBase]] that forwards
 * events in key/value pairs to [[org.apache.kafka.clients.producer.KafkaProducer]]
 *
 * @param config a configuration of Kafka producer, including the key
 *               serializer and the value serializer
 * @tparam K type of the key in key/value pairs to be published to Kafka
 * @tparam V type of the value in key/value pairs to be published to Kafka
 */
class FinagleKafkaProducer[K, V](config: FinagleKafkaProducerConfig[K, V])
    extends KafkaProducerBase[K, V] {

  private val keySerializer = config.keySerializer.get
  private val valueSerializer = config.valueSerializer.get
  private val producer = createProducer()

  private val clientId =
    config.kafkaProducerConfig.configMap.getOrElse(ProducerConfig.CLIENT_ID_CONFIG, "no_client_id")
  private val scopedStatsReceiver =
    config.statsReceiver.scope("kafka").scope(sanitizeMetricName(clientId))
  private val timestampOnSendLag = scopedStatsReceiver.stat("record_timestamp_on_send_lag")
  private val timestampOnSuccessLag = scopedStatsReceiver.stat("record_timestamp_on_success_lag")
  private val timestampOnFailureLag = scopedStatsReceiver.stat("record_timestamp_on_failure_lag")

  /* Public */

  //Note: Default partitionIdx should be set to null, see: https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=69406838
  //TODO: producer.send will throw exceptions in the transactional API which are not recoverable. As such, we may want to continue to allow these exceptions
  //to be thrown by this method, but this may be unexpected as most Future returning methods will return failed futures rather than throwing
  //exceptions. To be discussed...
  def send(
    topic: String,
    key: K,
    value: V,
    timestamp: Long,
    partitionIdx: Option[Integer] = None
  ): Future[RecordMetadata] = {
    val producerRecord = new ProducerRecord[K, V](
      topic,
      partitionIdx.orNull,
      timestamp,
      key,
      value
    )
    send(producerRecord)
  }

  def send(producerRecord: ProducerRecord[K, V]): Future[RecordMetadata] = {
    val resultPromise = Promise[RecordMetadata]()
    calcTimestampLag(timestampOnSendLag, producerRecord.timestamp)
    producer.send(
      producerRecord,
      new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          if (exception != null) {
            calcTimestampLag(timestampOnFailureLag, producerRecord.timestamp)
            resultPromise.setException(exception)
          } else {
            calcTimestampLag(timestampOnSuccessLag, producerRecord.timestamp)
            resultPromise.setValue(metadata)
          }
        }
      }
    )
    resultPromise
  }

  def initTransactions(): Unit = {
    producer.initTransactions()
  }

  def beginTransaction(): Unit = {
    producer.beginTransaction()
  }

  def sendOffsetsToTransaction(
    offsets: Map[TopicPartition, OffsetAndMetadata],
    consumerGroupId: String
  ): Unit = {
    producer.sendOffsetsToTransaction(offsets.asJava, consumerGroupId)
  }

  def commitTransaction(): Unit = {
    producer.commitTransaction()
  }

  def abortTransaction(): Unit = {
    producer.abortTransaction()
  }

  def flush(): Unit = {
    producer.flush()
  }

  def partitionsFor(topic: String): util.List[PartitionInfo] = {
    producer.partitionsFor(topic)
  }

  def close(deadline: Time): Future[Unit] = {
    // com.twitter.Util.Closable.close() calls com.twitter.Util.Closable.close(Duration) with
    // a duration of Time.Bottom to wait for the resource to be completely relinquished.
    // However, the underlying KafkaProducer will throw an IllegalArgumentException when
    // KafkaProducer.close(Duration) is called with a negative timeout.
    if (deadline == Time.Bottom) {
      Future(producer.close(Long.MaxValue, MILLISECONDS))
    } else {
      Future(producer.close(deadline.inSeconds, SECONDS))
    }
  }

  /* Private */

  private def createProducer(): KafkaProducer[K, V] = {
    TracingKafkaProducer[K, V](config.properties, keySerializer, valueSerializer)
  }

  private def calcTimestampLag(stat: Stat, timestamp: Long): Unit = {
    stat.add(System.currentTimeMillis() - timestamp)
  }
}
