package com.twitter.finatra.kafkastreams.internal.listeners

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.logging.Logging
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.streams.processor.StateRestoreListener
import org.joda.time.DateTimeUtils

/**
 * A [[StateRestoreListener]] that emits logs and metrics relating to state restoration.
 *
 * @param statsReceiver A StatsReceiver used for metric tracking.
 */
private[kafkastreams] class FinatraStateRestoreListener(statsReceiver: StatsReceiver)
    extends StateRestoreListener
    with Logging {

  private val scopedStatReceiver = statsReceiver.scope("finatra_state_restore_listener")
  private val totalRestoreTime =
    scopedStatReceiver.addGauge("restore_time_elapsed_ms")(restoreTimeElapsedMs)

  private var restoreTimestampStartMs: Option[Long] = None
  private var restoreTimestampEndMs: Option[Long] = None

  override def onRestoreStart(
    topicPartition: TopicPartition,
    storeName: String,
    startingOffset: Long,
    endingOffset: Long
  ): Unit = {
    restoreTimestampStartMs = Some(DateTimeUtils.currentTimeMillis)
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
    restoreTimestampEndMs = Some(DateTimeUtils.currentTimeMillis)
    info(
      s"${storeAndPartition(storeName, topicPartition)} finished restoring $totalRestored records in $restoreTimeElapsedMs ms"
    )
  }

  private def storeAndPartition(storeName: String, topicPartition: TopicPartition): String = {
    s"$storeName topic ${topicPartition.topic}_${topicPartition.partition}"
  }

  private def restoreTimeElapsedMs: Long = {
    val currentTimestampMs = DateTimeUtils.currentTimeMillis
    restoreTimestampEndMs.getOrElse(currentTimestampMs) - restoreTimestampStartMs.getOrElse(
      currentTimestampMs
    )
  }
}
