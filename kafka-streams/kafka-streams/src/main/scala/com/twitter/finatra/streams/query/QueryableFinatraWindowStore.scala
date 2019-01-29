package com.twitter.finatra.streams.query

import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.{
  KafkaPartitioner,
  StaticServiceShardPartitioner
}
import com.twitter.finatra.streams.stores.FinatraKeyValueStore
import com.twitter.finatra.streams.stores.internal.FinatraStoresGlobalManager
import com.twitter.finatra.streams.transformer.FinatraTransformer.{DateTimeMillis, WindowStartTime}
import com.twitter.finatra.streams.transformer.domain.{Time, TimeWindowed}
import com.twitter.inject.Logging
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.{Serde, Serializer}
import org.joda.time.DateTimeUtils
import scala.collection.JavaConverters._

class QueryableFinatraWindowStore[K, V](
  storeName: String,
  windowSize: Duration,
  keySerde: Serde[K],
  numShards: Int,
  numQueryablePartitions: Int,
  currentShardId: Int)
    extends Logging {

  // The number of windows to query before and/or after specified start and end times
  private val defaultWindowMultiplier = 3

  private val keySerializer = keySerde.serializer()

  private val currentServiceShardId = ServiceShardId(currentShardId)

  private val queryWindowSize = windowSize * defaultWindowMultiplier

  private val partitioner = new KafkaPartitioner(
    StaticServiceShardPartitioner(numShards = numShards),
    numPartitions = numQueryablePartitions
  )

  def get(
    key: K,
    startTime: Option[Long] = None,
    endTime: Option[Long] = None
  ): Map[WindowStartTime, V] = {
    throwIfNonLocalKey(key, keySerializer)

    val endWindowRange = endTime.getOrElse(
      TimeWindowed
        .windowStart(messageTime = Time(DateTimeUtils.currentTimeMillis), size = windowSize)
        .+(queryWindowSize)
        .millis)

    val startWindowRange =
      startTime.getOrElse(endWindowRange - queryWindowSize.inMillis)

    val windowedMap = new java.util.TreeMap[DateTimeMillis, V]

    var currentWindowStart = Time(startWindowRange)
    while (currentWindowStart.millis <= endWindowRange) {
      val windowedKey = TimeWindowed.forSize(currentWindowStart, windowSize, key)

      //TODO: Use store.taskId to find exact store where the key is assigned
      for (store <- stores) {
        val result = store.get(windowedKey)
        if (result != null) {
          windowedMap.put(currentWindowStart.millis, result)
        }
      }

      currentWindowStart = currentWindowStart + windowSize
    }

    windowedMap.asScala.toMap
  }

  private def throwIfNonLocalKey(key: K, keySerializer: Serializer[K]): Unit = {
    val keyBytes = keySerializer.serialize("", key)
    val partitionsToQuery = partitioner.shardIds(keyBytes)
    if (partitionsToQuery.head != currentServiceShardId) {
      throw new Exception(s"Non local key. Query $partitionsToQuery")
    }
  }

  private def stores: Iterable[FinatraKeyValueStore[TimeWindowed[K], V]] = {
    FinatraStoresGlobalManager.getWindowedStores(storeName)
  }
}
