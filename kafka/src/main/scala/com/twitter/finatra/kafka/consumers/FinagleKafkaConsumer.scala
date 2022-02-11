package com.twitter.finatra.kafka.consumers

import com.twitter.finatra.kafka.domain.KafkaTopic
import com.twitter.finatra.kafka.domain.SeekStrategy
import com.twitter.finatra.utils.FuturePools
import com.twitter.util._
import com.twitter.util.logging.Logging
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import scala.collection.JavaConverters._

/*
 * Note: The current implementation relies on a future pool with a single thread since Kafka
 * requires poll to always be called from the same thread. However, we can likely optimize the
 * conversion to avoid the need for a future pool
 */
@deprecated(
  "Use the FinagleKafkaConsumerBuilder.buildClient to create a native Kafka Consumer instead.",
  "2019-05-30")
class FinagleKafkaConsumer[K, V](config: FinagleKafkaConsumerConfig[K, V])
    extends Closable
    with Logging {

  private val groupId = config.kafkaConsumerConfig.configMap(ConsumerConfig.GROUP_ID_CONFIG)
  private val keyDeserializer = config.keyDeserializer.get
  private val valueDeserializer = config.valueDeserializer.get
  private val seekStrategy = config.seekStrategy
  private val rewindDuration = config.rewindDuration
  private val singleThreadFuturePool = FuturePools.fixedPool(s"kafka-consumer-$groupId", 1)
  private val consumer = createConsumer()
  private var subscribed = false
  private var assigned = false
  private val initialSeekCompleted = new AtomicBoolean()

  /**
   * This class will handle seek strategy when partitions are first assigned to this consumer.
   * And it also takes an inner listener which can be passed in by user to handle user defined
   * listening actions.
   */
  private class SeekRebalanceListener(innerListener: Option[ConsumerRebalanceListener] = None)
      extends ConsumerRebalanceListener {
    override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit = {
      if (initialSeekCompleted.compareAndSet(false, true)) {
        info(s"Applying seek strategy $seekStrategy")
        seekStrategy match {
          case SeekStrategy.BEGINNING => consumer.seekToBeginning(partitions)
          case SeekStrategy.END => consumer.seekToEnd(partitions)
          case SeekStrategy.REWIND =>
            require(rewindDuration.isDefined)
            val seekTime = rewindDuration.get.ago
            seekToTime(seekTime)
          case _ =>
        }
        // We don't need to commit offsets when resuming, only when we're seeking to a designated position
        if (seekStrategy != SeekStrategy.RESUME) {
          info(s"Committing offsets after seek is complete")
          consumer.commitSync()
        }
      }
      if (innerListener.isDefined) {
        innerListener.get.onPartitionsAssigned(partitions)
      }
    }

    override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit = {
      if (innerListener.isDefined) {
        innerListener.get.onPartitionsRevoked(partitions)
      }
    }

    /**
     * Positions the consumer the a specific time for each partition assigned to this consumer.
     */
    private def seekToTime(seekTime: Time): Unit = {
      val partitionTimestamps = (consumer.assignment.asScala map { topicPartition =>
        topicPartition -> java.lang.Long.valueOf(seekTime.inMillis)
      }).toMap.asJava
      consumer.offsetsForTimes(partitionTimestamps).asScala foreach {
        partitionOffsetPair: (TopicPartition, OffsetAndTimestamp) =>
          val partition = partitionOffsetPair._1
          val offset = partitionOffsetPair._2.offset()
          consumer.seek(partition, offset)
      }
    }
  }

  /**
   * Subscribe to the given list of topics to get dynamically assigned partitions.
   *
   * We block start until the consumer is subscribed to the topic. We're using a future pool here
   * since Kafka requires all interactions with the consumer to come from a single thread.
   */
  def subscribe(
    topics: Set[KafkaTopic],
    listener: Option[ConsumerRebalanceListener] = None
  ): Unit = {
    Await.result(singleThreadFuturePool({
      assert(!subscribed, "subscribe() has already been called")
      val topicNames = topics.map(_.name)
      consumer.subscribe(topicNames.asJava, new SeekRebalanceListener(listener))
      info(s"Subscribed to topics ${consumer.subscription()}")
      subscribed = true
    }))
  }

  def assignment(): Future[util.Set[TopicPartition]] = {
    singleThreadFuturePool(consumer.assignment())
  }

  /**
   * Manually assign partitions to the consumer. This assignment is not incremental but replaces
   * the previous assignment.
   */
  def assign(partitions: Seq[TopicPartition]): Unit = {
    Await.result(singleThreadFuturePool({
      consumer.assign(partitions.asJavaCollection)
      info(s"Assigned to topics-partitions ${consumer.assignment()}")
      assigned = true
    }))
  }

  def seekToOffset(partition: TopicPartition, offset: Long): Future[Unit] = {
    singleThreadFuturePool(consumer.seek(partition, offset))
  }

  def seekToBeginning(partitions: util.Collection[TopicPartition]): Future[Unit] = {
    singleThreadFuturePool(consumer.seekToBeginning(partitions))
  }

  def seekToEnd(partitions: util.Collection[TopicPartition]): Future[Unit] = {
    singleThreadFuturePool(consumer.seekToEnd(partitions))
  }

  def offsetsForTimes(
    timestampsToSearch: java.util.Map[TopicPartition, java.lang.Long]
  ): Future[util.Map[TopicPartition, OffsetAndTimestamp]] = {
    singleThreadFuturePool(consumer.offsetsForTimes(timestampsToSearch))
  }

  /**
   * Get the end offsets for the given partitions. In the default {@code read_uncommitted} isolation level, the end
   * offset is the high watermark (that is, the offset of the last successfully replicated message plus one). For
   * {@code read_committed} consumers, the end offset is the last stable offset (LSO), which is the minimum of
   * the high watermark and the smallest offset of any open transaction. Finally, if the partition has never been
   * written to, the end offset is 0.
   */
  def endOffsets(
    partitions: Seq[TopicPartition]
  ): Future[util.Map[TopicPartition, java.lang.Long]] = {
    singleThreadFuturePool(consumer.endOffsets(partitions.asJavaCollection))
  }

  /**
   * Get the end offset for a given partition.
   */
  def endOffset(partition: TopicPartition): Future[Long] = {
    singleThreadFuturePool(consumer.endOffsets(List(partition).asJava).get(partition))
  }

  /**
   * @param timeout The time, in milliseconds, spent waiting in poll if data is not available in the buffer.
   *                If 0, returns immediately with any records that are available currently in the buffer, else returns empty.
   *                Must not be negative.
   * @return map of topic to records since the last fetch for the subscribed list of topics and partitions
   */
  def poll(timeout: Duration = config.pollTimeout): Future[ConsumerRecords[K, V]] = {
    singleThreadFuturePool({
      assert(subscribed || assigned, "either subscribe() or assign() has not been called")
      // We should only seek after the first poll and getting partition assignment successfully
      consumer.poll(timeout.inMilliseconds)
    })
  }

  /**
   * Commit offsets returned on the last poll() for all the subscribed list of topics and partition.
   */
  def commit(): Future[Unit] = {
    singleThreadFuturePool({
      consumer.commitSync()
    })
  }

  /**
   * Get the offset of the next record that will be fetched (if a record with that offset exists).
   */
  def position(partition: TopicPartition): Future[Long] = {
    singleThreadFuturePool({
      consumer.position(partition)
    })
  }

  /**
   * Commit the specified offsets for the specified list of topics and partitions.
   */
  def commit(offsets: util.Map[TopicPartition, OffsetAndMetadata]): Future[Unit] = {
    singleThreadFuturePool({
      consumer.commitSync(offsets)
    })
  }

  /**
   * Wakeup the consumer. This method is thread-safe and is useful in particular to abort a long poll.
   *
   * The thread which is blocking in an operation will throw WakeupException
   * If no thread is blocking, the next blocking call will raise it instead.
   */
  def wakeup(): Unit = {
    consumer.wakeup()
  }

  def close(deadline: Time): Future[Unit] = {
    try {
      singleThreadFuturePool({
        info(s"Closing consumer for topics: ${consumer.subscription()}")
        // com.twitter.Util.Closable.close() calls com.twitter.Util.Closable.close(Duration) with
        // a duration of Time.Bottom to wait for the resource to be completely relinquished.
        // However, the underlying KafkaConsumer will throw an IllegalArgumentException when
        // KafkaConsumer.close(Duration) is called with a negative timeout.
        if (deadline == Time.Bottom) {
          consumer.close(java.time.Duration.ofMillis(Long.MaxValue))
        } else {
          consumer.close(java.time.Duration.ofMillis(deadline.inMillis))
        }
      }).ensure {
        singleThreadFuturePool.executor.shutdown()
      }
    } catch {
      case e: Exception =>
        error(s"Error closing consumer ${groupId}", e)
        Future.exception(e)
    }
  }

  /* Private */
  private def createConsumer(): KafkaConsumer[K, V] = {
    new KafkaConsumer[K, V](config.properties, keyDeserializer, valueDeserializer)
  }
}
