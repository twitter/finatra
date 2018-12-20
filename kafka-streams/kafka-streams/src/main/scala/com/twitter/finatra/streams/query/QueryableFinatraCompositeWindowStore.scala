package com.twitter.finatra.streams.query

import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.{
  KafkaPartitioner,
  StaticServiceShardPartitioner
}
import com.twitter.finatra.streams.transformer.FinatraTransformer.{DateTimeMillis, WindowStartTime}
import com.twitter.finatra.streams.transformer.domain.{CompositeKey, Time, TimeWindowed}
import com.twitter.inject.Logging
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.{Serde, Serializer}
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.state.{KeyValueIterator, QueryableStoreTypes, ReadOnlyKeyValueStore}
import org.joda.time.{DateTime, DateTimeUtils}
import scala.collection.JavaConverters._
import scala.collection.mutable

//TODO: DRY with other queryable finatra stores
class QueryableFinatraCompositeWindowStore[K, PK, SK, V](
  kafkaStreams: => KafkaStreams,
  storeName: String,
  primaryKeySerde: Serde[PK],
  numShards: Int,
  numQueryablePartitions: Int,
  currentShardId: Int)
    extends Logging {
  private val primaryKeySerializer = primaryKeySerde.serializer()

  private val currentServiceShardId = ServiceShardId(currentShardId)

  private val partitioner = new KafkaPartitioner(
    StaticServiceShardPartitioner(numShards = numShards),
    numPartitions = numQueryablePartitions
  )

  private val queryableStoreType = {
    QueryableStoreTypes.keyValueStore[TimeWindowed[CompositeKey[PK, SK]], V]
  }

  //TODO: DRY with other queryable stores
  def get(
    primaryKey: PK,
    windowSize: Duration,
    startTime: Option[Long],
    endTime: Option[Long],
    queryLocalWindow: (PK, DateTime,
      DateTime) => KeyValueIterator[TimeWindowed[CompositeKey[PK, SK]], V]
  ): Map[WindowStartTime, Map[SK, V]] = {
    throwIfNonLocalKey(primaryKey, primaryKeySerializer)

    val windowSizeMillis = windowSize.inMillis

    val (startWindowRange, endWindowRange) = startAndEndRange(
      startTime = startTime,
      endTime = endTime,
      windowSizeMillis = windowSizeMillis
    )

    val windowedCountsMapBuilder = Map.newBuilder[WindowStartTime, Map[SK, V]]
    val numWindows = (endWindowRange - startWindowRange) / windowSizeMillis
    windowedCountsMapBuilder.sizeHint(numWindows.toInt)

    var currentWindowStart = startWindowRange
    var currentWindowEnd = currentWindowStart + windowSizeMillis
    while (currentWindowStart <= endWindowRange) {
      callQueryLocal(
        primaryKey,
        queryLocalWindow,
        currentWindowStart,
        currentWindowEnd,
        windowedCountsMapBuilder
      )

      currentWindowStart = currentWindowStart + windowSizeMillis
      currentWindowEnd = currentWindowEnd + windowSizeMillis
    }

    windowedCountsMapBuilder.result()
  }

  def store: ReadOnlyKeyValueStore[TimeWindowed[CompositeKey[PK, SK]], V] = {
    if (kafkaStreams == null) {
      throw new InvalidStateStoreException(s"Store $storeName not started")
    }

    kafkaStreams.store(storeName, queryableStoreType)
  }

  private def callQueryLocal(
    primaryKey: PK,
    queryLocalWindow: (PK, DateTime,
      DateTime) => KeyValueIterator[TimeWindowed[CompositeKey[PK, SK]], V],
    currentWindowStart: DateTimeMillis,
    currentWindowEnd: DateTimeMillis,
    windowedCountsMapBuilder: mutable.Builder[
      (WindowStartTime, Map[SK, V]),
      Map[
        WindowStartTime,
        Map[SK, V]
      ]]
  ) = {
    val localEntriesIterator =
      queryLocalWindow(primaryKey, new DateTime(currentWindowStart), new DateTime(currentWindowEnd))

    try {
      val localEntries = localEntriesIterator.asScala
      val attributeMapBuilder = Map.newBuilder[SK, V]
      attributeMapBuilder.sizeHint(8)
      for (entry <- localEntries) {
        attributeMapBuilder += (entry.key.value.secondary -> entry.value)
      }

      val attributeMap = attributeMapBuilder.result()
      if (attributeMap.nonEmpty) {
        windowedCountsMapBuilder += (currentWindowStart -> attributeMap)
      }
    } finally {
      localEntriesIterator.close()
    }
  }

  //TODO: Reuse in other queryable stores
  private def startAndEndRange(
    startTime: Option[DateTimeMillis],
    endTime: Option[DateTimeMillis],
    windowSizeMillis: DateTimeMillis
  ) = {
    val endWindowRange = endTime.getOrElse(
      TimeWindowed.windowStart(
        messageTime = Time(DateTimeUtils.currentTimeMillis),
        sizeMs = windowSizeMillis
      ) + windowSizeMillis
    )

    val startWindowRange = startTime.getOrElse(endWindowRange - (3 * windowSizeMillis))

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
