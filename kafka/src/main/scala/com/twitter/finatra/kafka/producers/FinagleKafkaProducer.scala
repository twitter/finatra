package com.twitter.finatra.kafka.producers

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Backoff
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.stats.Stat
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter.sanitizeMetricName
import com.twitter.inject.utils.RetryUtils
import com.twitter.util._
import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.errors.TimeoutException
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
class FinagleKafkaProducer[K, V] private[kafka] (
  producer: KafkaProducer[K, V],
  config: FinagleKafkaProducerConfig[K, V])
    extends KafkaProducerBase[K, V] {

  private val clientId =
    config.kafkaProducerConfig.configMap.getOrElse(ProducerConfig.CLIENT_ID_CONFIG, "no_client_id")
  private val scopedStatsReceiver =
    config.statsReceiver.scope("kafka").scope(sanitizeMetricName(clientId))
  private val timestampOnSendLag = scopedStatsReceiver.stat("record_timestamp_on_send_lag")
  private val timestampOnSuccessLag = scopedStatsReceiver.stat("record_timestamp_on_success_lag")
  private val timestampOnFailureLag = scopedStatsReceiver.stat("record_timestamp_on_failure_lag")

  // use the existing max block ms as the max delay introduced by backing off
  private[producers] val maxDelay =
    Option(config.properties.getProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG)) match {
      case None => 1.minute // Kafka's default value
      case Some(s) =>
        // this is guaranteed to be a valid string representation of a long
        // since the producer validates this and it's called first
        s.toLong.milliseconds
    }
  private val retryPolicy = FinagleKafkaProducer.retryPolicy(maxDelay)

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
    RetryUtils.retryFuture(
      retryPolicy, // retry only the specific failures we care about
      suppress = true, // no need to log failures here since the returned future has that info
    ) {
      // `retryFuture` will catch thrown exceptions

      // create a new record for every retry, otherwise we hit exceptions
      // when the producer interceptor tries to add the publish time to the
      // same record header multiple times
      val record = new ProducerRecord[K, V](
        producerRecord.topic,
        producerRecord.partition,
        producerRecord.timestamp,
        producerRecord.key,
        producerRecord.value,
        producerRecord.headers
      )

      // ensure that we have topic metadata before attempting to publish
      // `partitionsFor` will throw on failures we will retry
      if (producer.partitionsFor(record.topic()).isEmpty) {
        // ensure that there are actually partitions
        // this is the same exception that Kafka throws if metadata is missing
        throw new TimeoutException(s"Metadata not available for topic: ${producerRecord.topic()}")
      }

      val resultPromise = Promise[RecordMetadata]()
      producer.send(
        record,
        new Callback {
          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
            if (exception != null) {
              calcTimestampLag(timestampOnFailureLag, record.timestamp)
              resultPromise.setException(exception)
            } else {
              calcTimestampLag(timestampOnSuccessLag, record.timestamp)
              resultPromise.setValue(metadata)
            }
          }
        }
      )
      calcTimestampLag(timestampOnSendLag, record.timestamp)
      resultPromise
    }
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

  private def calcTimestampLag(stat: Stat, timestamp: Long): Unit = {
    stat.add(System.currentTimeMillis() - timestamp)
  }
}

object FinagleKafkaProducer {

  def apply[K, V](config: FinagleKafkaProducerConfig[K, V]): FinagleKafkaProducer[K, V] = {

    val properties = new Properties()
    val keySerializer = config.keySerializer.get
    val valueSerializer = config.valueSerializer.get

    // copy so we don't change the existing properties
    properties.putAll(config.properties)
    // override and set this to 0 since we do backoffs in the wrapper and no longer need to block
    properties.setProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG, "0")

    val producer = TracingKafkaProducer[K, V](properties, keySerializer, valueSerializer)
    new FinagleKafkaProducer[K, V](producer, config)
  }

  // instance of a retry policy with the configured `maxDelay` which will retry TimeoutExceptions
  private[producers] def retryPolicy(maxDelay: Duration): RetryPolicy[Try[RecordMetadata]] = {
    val backoff = Backoff
      .exponential(1.millisecond, 2)
    RetryPolicy.backoff(backoff.takeUntil(maxDelay)) {
      case Throw(_: TimeoutException) => true
      case _ => false
    }
  }
}
