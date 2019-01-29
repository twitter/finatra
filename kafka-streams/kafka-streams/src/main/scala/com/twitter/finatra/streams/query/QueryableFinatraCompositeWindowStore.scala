package com.twitter.finatra.streams.query

import com.twitter.conversions.DurationOps._
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

  /**
   * Get a range of composite keys and return them combined in a map. If the primary key for the
   * composite keys is non-local to this Kafka Streams instance, return an exception indicating
   * which instance is hosting this primary key
   *
   * @param primaryKey The primary key for the data being queried (e.g. a UserId)
   * @param startCompositeKey The starting composite key being queried (e.g. UserId-ClickType)
   * @param endCompositeKey The ending composite key being queried (e.g. UserId-ClickType)
   * @param allowStaleReads Allow stale reads when querying a caching key value store. If set to false,
   *                        each query will trigger a flush of the cache.
   * @param startTime The start time of the windows being queried
   * @param endTime The end time of the windows being queried
   * @return A time windowed map of composite keys to their values
   */
  def get(
    primaryKey: PK,
    startCompositeKey: CompositeKey[PK, SK],
    endCompositeKey: CompositeKey[PK, SK],
    allowStaleReads: Boolean,
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
      queryWindow(startCompositeKey, endCompositeKey, windowStartTime, allowStaleReads, resultMap)
      windowStartTime = windowStartTime + windowSizeMillis
    }

    resultMap.toMap
  }

  /* Private */

  private def queryWindow(
    startCompositeKey: CompositeKey[PK, SK],
    endCompositeKey: CompositeKey[PK, SK],
    windowStartTime: DateTimeMillis,
    allowStaleReads: Boolean,
    resultMap: scala.collection.mutable.Map[Long, scala.collection.mutable.Map[SK, V]]
  ): Unit = {
    trace(s"QueryWindow $startCompositeKey to $endCompositeKey ${windowStartTime.asInstanceOf[Long].iso8601}")

    //TODO: Use store.taskId to find exact store where the key is assigned
    for (store <- FinatraStoresGlobalManager.getWindowedCompositeStores[PK, SK, V](storeName)) {
      val iterator = store.range(
        TimeWindowed.forSize(start = Time(windowStartTime), windowSize, startCompositeKey),
        TimeWindowed.forSize(start = Time(windowStartTime), windowSize, endCompositeKey),
        allowStaleReads = allowStaleReads
      )

      while (iterator.hasNext) {
        val entry = iterator.next()
        trace(s"$store\t$entry")
        val innerMap =
          resultMap.getOrElseUpdate(entry.key.start.millis, scala.collection.mutable.Map[SK, V]())
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
      TimeWindowed
        .windowStart(messageTime = Time(DateTimeUtils.currentTimeMillis), size = windowSize)
        .+(windowSizeMillis.millis * defaultWindowMultiplier)
        .millis
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
