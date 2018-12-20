package com.twitter.finatra.kafka.producers

import com.twitter.finagle.stats.Stat
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter.sanitizeMetricName
import com.twitter.inject.Logging
import com.twitter.util._
import java.util
import java.util.concurrent.TimeUnit.SECONDS
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import scala.collection.JavaConverters._

class FinagleKafkaProducer[K, V](config: FinagleKafkaProducerConfig[K, V])
    extends Closable
    with Logging {

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

  override def close(deadline: Time): Future[Unit] = {
    Future(producer.close(deadline.inSeconds, SECONDS))
  }

  /* Private */

  private def createProducer(): KafkaProducer[K, V] = {
    new KafkaProducer[K, V](config.properties, keySerializer, valueSerializer)
  }

  private def calcTimestampLag(stat: Stat, timestamp: Long): Unit = {
    stat.add(System.currentTimeMillis() - timestamp)
  }
}
