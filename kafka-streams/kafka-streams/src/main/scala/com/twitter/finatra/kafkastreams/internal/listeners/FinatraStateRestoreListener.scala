package com.twitter.finatra.kafkastreams.internal.listeners

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.logging.Logging
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.streams.processor.StateRestoreListener

class FinatraStateRestoreListener(
  statsReceiver: StatsReceiver) //TODO: Add stats for restoration (e.g. total time)
    extends StateRestoreListener
    with Logging {

  override def onRestoreStart(
    topicPartition: TopicPartition,
    storeName: String,
    startingOffset: Long,
    endingOffset: Long
  ): Unit = {
    val upToRecords = endingOffset - startingOffset
    info(
      s"${storeAndPartition(storeName, topicPartition)} start restoring up to $upToRecords records from $startingOffset to $endingOffset"
    )
  }

  override def onBatchRestored(
    topicPartition: TopicPartition,
    storeName: String,
    batchEndOffset: Long,
    numRestored: Long
  ): Unit = {
    trace(s"Restored $numRestored records for ${storeAndPartition(storeName, topicPartition)}")
  }

  override def onRestoreEnd(
    topicPartition: TopicPartition,
    storeName: String,
    totalRestored: Long
  ): Unit = {
    info(
      s"${storeAndPartition(storeName, topicPartition)} finished restoring $totalRestored records"
    )
  }

  private def storeAndPartition(storeName: String, topicPartition: TopicPartition) = {
    s"$storeName topic ${topicPartition.topic}_${topicPartition.partition}"
  }
}
