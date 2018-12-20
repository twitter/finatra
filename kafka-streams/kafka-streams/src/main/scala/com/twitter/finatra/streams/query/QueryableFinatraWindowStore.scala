package com.twitter.finatra.streams.query

import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.{
  KafkaPartitioner,
  StaticServiceShardPartitioner
}
import com.twitter.finatra.streams.transformer.FinatraTransformer.WindowStartTime
import com.twitter.finatra.streams.transformer.domain.{Time, TimeWindowed}
import com.twitter.inject.Logging
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.{Serde, Serializer}
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.joda.time.{DateTime, DateTimeUtils}

class QueryableFinatraWindowStore[K, V](
  store: => ReadOnlyKeyValueStore[TimeWindowed[K], V],
  keySerde: Serde[K],
  numShards: Int,
  numQueryablePartitions: Int,
  currentShardId: Int)
    extends Logging {
  private val keySerializer = keySerde.serializer()

  private val currentServiceShardId = ServiceShardId(currentShardId)

  private val partitioner = new KafkaPartitioner(
    StaticServiceShardPartitioner(numShards = numShards),
    numPartitions = numQueryablePartitions
  )

  def get(
    key: K,
    windowSize: Duration,
    startTime: Option[Long],
    endTime: Option[Long]
  ): Map[WindowStartTime, V] = {
    val windowSizeMillis = windowSize.inMillis

    val endWindowRange = endTime.getOrElse(
      TimeWindowed.windowStart(
        messageTime = Time(DateTimeUtils.currentTimeMillis),
        sizeMs = windowSizeMillis
      ) + windowSizeMillis
    )

    val startWindowRange = startTime.getOrElse(endWindowRange - (3 * windowSizeMillis))

    val windowedMap = Map.newBuilder[Long, V]
    val numWindows = (endWindowRange - startWindowRange) / windowSizeMillis
    windowedMap.sizeHint(numWindows.toInt)

    trace(s"Start: ${new DateTime(startWindowRange)}. End: ${new DateTime(endWindowRange)}")

    var currentWindowStart = startWindowRange
    while (currentWindowStart <= endWindowRange) {
      get(key, windowSize, currentWindowStart) match {
        case Some(value) =>
          trace(s"GOT $value")
          windowedMap += (currentWindowStart -> value)
        case None =>
      }

      currentWindowStart = currentWindowStart + windowSizeMillis
    }

    windowedMap.result()
  }

  def get(key: K, windowSize: Duration, timestamp: Long): Option[V] = {
    throwIfNonLocalKey(key, keySerializer)

    val windowedKey = TimeWindowed.forSize(timestamp, windowSize.inMillis, key)
    trace(s"Get $windowedKey")
    Option(store.get(windowedKey))
  }

  private def throwIfNonLocalKey(key: K, keySerializer: Serializer[K]): Unit = {
    val keyBytes = keySerializer.serialize("", key)
    val partitionsToQuery = partitioner.shardIds(keyBytes)
    if (partitionsToQuery.head != currentServiceShardId) {
      throw new Exception(s"Non local key. Query $partitionsToQuery")
    }
  }
}
