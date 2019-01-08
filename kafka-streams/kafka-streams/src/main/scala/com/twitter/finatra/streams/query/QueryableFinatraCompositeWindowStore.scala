package com.twitter.finatra.streams.query

import com.twitter.finatra.streams.converters.time._
import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.{
  KafkaPartitioner,
  StaticServiceShardPartitioner
}
import com.twitter.finatra.streams.stores.internal.FinatraStoresGlobalManager
import com.twitter.finatra.streams.transformer.FinatraTransformer.{DateTimeMillis, WindowStartTime}
import com.twitter.finatra.streams.transformer.domain.{CompositeKey, Time, TimeWindowed}
import com.twitter.inject.Logging
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.{Serde, Serializer}
import org.joda.time.DateTimeUtils
import scala.collection.JavaConverters._

//TODO: DRY with other queryable finatra stores
class QueryableFinatraCompositeWindowStore[PK, SK, V](
  storeName: String,
  windowSize: Duration,
  primaryKeySerde: Serde[PK],
  numShards: Int,
  numQueryablePartitions: Int,
  currentShardId: Int)
    extends Logging {

  // The number of windows to query before and/or after specified start and end times
  private val defaultWindowMultiplier = 3

  private val primaryKeySerializer = primaryKeySerde.serializer()

  private val currentServiceShardId = ServiceShardId(currentShardId)

  private val windowSizeMillis = windowSize.inMillis

  private val partitioner = new KafkaPartitioner(
    StaticServiceShardPartitioner(numShards = numShards),
    numPartitions = numQueryablePartitions
  )

  /* Public */

  def get(
    primaryKey: PK,
    startCompositeKey: CompositeKey[PK, SK],
    endCompositeKey: CompositeKey[PK, SK],
    startTime: Option[DateTimeMillis] = None,
    endTime: Option[DateTimeMillis] = None
  ): Map[WindowStartTime, scala.collection.Map[SK, V]] = {
    throwIfNonLocalKey(primaryKey, primaryKeySerializer)

    val (startWindowRange, endWindowRange) = startAndEndRange(
      startTime = startTime,
      endTime = endTime,
      windowSizeMillis = windowSizeMillis)

    val resultMap = new java.util.TreeMap[Long, scala.collection.mutable.Map[SK, V]]().asScala

    var windowStartTime = startWindowRange
    while (windowStartTime <= endWindowRange) {
      queryWindow(startCompositeKey, endCompositeKey, windowStartTime, resultMap)
      windowStartTime = windowStartTime + windowSizeMillis
    }

    resultMap.toMap
  }

  /* Private */

  private def queryWindow(
    startCompositeKey: CompositeKey[PK, SK],
    endCompositeKey: CompositeKey[PK, SK],
    windowStartTime: DateTimeMillis,
    resultMap: scala.collection.mutable.Map[Long, scala.collection.mutable.Map[SK, V]]
  ): Unit = {
    trace(s"QueryWindow $startCompositeKey to $endCompositeKey ${windowStartTime.iso8601}")

    //TODO: Use store.taskId to find exact store where the key is assigned
    for (store <- FinatraStoresGlobalManager.getWindowedCompositeStores[PK, SK, V](storeName)) {
      val iterator = store.range(
        TimeWindowed.forSize(startMs = windowStartTime, windowSizeMillis, startCompositeKey),
        TimeWindowed.forSize(startMs = windowStartTime, windowSizeMillis, endCompositeKey)
      )

      while (iterator.hasNext) {
        val entry = iterator.next()
        info(s"$store\t$entry")
        val innerMap =
          resultMap.getOrElseUpdate(entry.key.startMs, scala.collection.mutable.Map[SK, V]())
        innerMap += (entry.key.value.secondary -> entry.value)
      }
    }
  }

  private def startAndEndRange(
    startTime: Option[DateTimeMillis],
    endTime: Option[DateTimeMillis],
    windowSizeMillis: DateTimeMillis
  ): (DateTimeMillis, DateTimeMillis) = {
    val endWindowRange = endTime.getOrElse {
      TimeWindowed.windowStart(
        messageTime = Time(DateTimeUtils.currentTimeMillis),
        sizeMs = windowSizeMillis) + defaultWindowMultiplier * windowSizeMillis
    }

    val startWindowRange =
      startTime.getOrElse(endWindowRange - (defaultWindowMultiplier * windowSizeMillis))

    (startWindowRange, endWindowRange)
  }

  private def throwIfNonLocalKey(key: PK, keySerializer: Serializer[PK]): Unit = {
    val keyBytes = keySerializer.serialize("", key)
    val partitionsToQuery = partitioner.shardIds(keyBytes)
    if (partitionsToQuery.head != currentServiceShardId) {
      throw new Exception(s"Non local key. Query $partitionsToQuery")
    }
  }
}
