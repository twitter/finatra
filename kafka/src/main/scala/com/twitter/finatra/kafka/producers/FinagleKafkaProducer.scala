package com.twitter.finatra.kafka.producers

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.stats.Stat
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter.sanitizeMetricName
import com.twitter.finatra.kafka.utils.MaxDelayExponentialRetryPolicy
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
class FinagleKafkaProducer[K, V](config: FinagleKafkaProducerConfig[K, V])
    extends KafkaProducerBase[K, V] {

  private[producers] val properties = new Properties()
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

  // use the existing max block ms as the max delay introduced by backing off
  private[producers] val maxDelay =
    Option(config.properties.getProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG)) match {
      case None => 1.minute // Kafka's default value
      case Some(s) =>
        // this is guaranteed to be a valid string representation of a long
        // since the producer validates this and it's called first
        s.toLong.milliseconds
    }

  private val enqueueThenSendVal =
    FinagleKafkaProducer.enqueueThenSend[K, V](
      producer,
      FinagleKafkaProducer.retryPolicy(maxDelay),
      timestampOnSendLag,
      timestampOnSuccessLag,
      timestampOnFailureLag
    )(_)

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

  def send(producerRecord: ProducerRecord[K, V]): Future[RecordMetadata] =
    enqueueThenSend(producerRecord).flatten

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

  /** enqueues a [[ProducerRecord]] in to the Kafka client's internal buffer then sends it to Kafka
   *
   * if metadata isn't available for a topic or if the buffer is full at the time enqueueThenSend
   * is called then this will retry for up to the configured `max.block.ms` using Futures. It won't
   * block the caller.
   *
   * TODO: add to KafkaProducerBase and make this public when the name is solidified
   *
   * @param producerRecord the record to enqueue and send
   * @return a `Future[Future[RecordMetadata]]` where the outer Future completes when the event is
   *         enqueued and the inner Future completes when the event has been sent to Kafka
   */
  private def enqueueThenSend(
    producerRecord: ProducerRecord[K, V]
  ): Future[Future[RecordMetadata]] =
    enqueueThenSendVal(producerRecord)

  private def createProducer(): KafkaProducer[K, V] = {
    // copy so we don't change the existing properties
    properties.putAll(config.properties)
    // override and set this to 0 since we do backoffs in the wrapper and no longer need to block
    properties.setProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG, "0")
    TracingKafkaProducer[K, V](properties, keySerializer, valueSerializer)
  }
}

private[producers] object FinagleKafkaProducer {

  def calcTimestampLag(stat: Stat, timestamp: Long): Unit = {
    stat.add(System.currentTimeMillis() - timestamp)
  }

  // instance of a retry policy with the configured `maxDelay` which will retry Buffer and Timeout exceptions
  def retryPolicy(maxDelay: Duration): RetryPolicy[Try[Future[RecordMetadata]]] =
    new MaxDelayExponentialRetryPolicy(
      maxDelay,
      {
        case _: BufferExhaustedException => true // buffer is full
        case _: TimeoutException => true // metadata isn't ready
      })

  def enqueueThenSend[K, V](
    producer: Producer[K, V],
    retryPolicy: RetryPolicy[Try[Future[RecordMetadata]]],
    timestampOnSendLag: Stat,
    timestampOnSuccessLag: Stat,
    timestampOnFailureLag: Stat
  )(
    producerRecord: ProducerRecord[K, V]
  ): Future[Future[RecordMetadata]] = {
    val sendResult = Promise[RecordMetadata]()

    RetryUtils
      .retryFuture(
        retryPolicy, // retry only the specific failures we care about
        suppress = true // no need to log failures here since the returned future has that info
      ) {
        // `retryFuture` will catch thrown exceptions

        // ensure that we have topic metadata Before attempting to publish
        // `partitionsFor` will throw on failures we will retry
        if (producer.partitionsFor(producerRecord.topic()).isEmpty) {
          // ensure that there are actually partitions
          // this is the same exception that Kafka throws if metadata is missing
          throw new TimeoutException(s"Metadata not available for topic: ${producerRecord.topic()}")
        }

        // `send` will throw on failure types we will retry
        producer.send(
          producerRecord,
          new Callback {
            override def onCompletion(
              metadata: RecordMetadata,
              exception: Exception
            ): Unit = {
              if (exception != null) {
                calcTimestampLag(timestampOnFailureLag, producerRecord.timestamp)
                sendResult.setException(exception)
              } else {
                calcTimestampLag(timestampOnSuccessLag, producerRecord.timestamp)
                sendResult.setValue(metadata)
              }
            }
          }
        )

        // `send` returned and no exceptions were thrown so event was successfully enqueued
        // only add to the stat once we've invoked `send` to avoid duplicate stats from retries
        calcTimestampLag(timestampOnSendLag, producerRecord.timestamp)
        // if we get here then we have successfully enqueued
        Future.value(sendResult)
      }
  }
}
