package com.twitter.finatra.kafkastreams.query

import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer.DateTimeMillis
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer.WindowStartTime
import com.twitter.finatra.kafkastreams.transformer.aggregation.TimeWindowed
import com.twitter.finatra.kafkastreams.transformer.domain.CompositeKey
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import com.twitter.finatra.kafkastreams.transformer.stores.FinatraReadOnlyKeyValueStore
import com.twitter.finatra.kafkastreams.transformer.stores.internal.FinatraStoresGlobalManager
import com.twitter.finatra.kafkastreams.utils.time._
import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.KafkaPartitioner
import com.twitter.finatra.streams.queryable.thrift.partitioning.StaticServiceShardPartitioner
import com.twitter.util.Duration
import com.twitter.util.logging.Logging
import java.io.File
import org.apache.kafka.common.serialization.Serde
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * A queryable Finatra composite window store for use by endpoints exposing queryable
 * state (composite stores utilize composite keys which are comprised of a primary
 * and secondary key.
 */
class QueryableFinatraCompositeWindowStore[PK, SK, V](
  stateDir: File,
  storeName: String,
  windowSize: Duration,
  allowedLateness: Duration,
  queryableAfterClose: Duration,
  primaryKeySerde: Serde[PK],
  numShards: Int,
  numQueryablePartitions: Int,
  currentShardId: Int)
    extends Logging {

  private val primaryKeySerializer = primaryKeySerde.serializer()

  private val currentServiceShardId = ServiceShardId(currentShardId)

  private val windowSizeMillis = windowSize.inMillis

  private val partitioner = new KafkaPartitioner(
    StaticServiceShardPartitioner(numShards = numShards),
    numPartitions = numQueryablePartitions
  )

  private val defaultQueryRange = QueryableFinatraWindowStore
    .defaultQueryRange(windowSize, allowedLateness, queryableAfterClose)

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
    val primaryKeyBytes = FinatraStoresGlobalManager.primaryKeyBytesIfLocalKey(
      partitioner,
      currentServiceShardId,
      primaryKey,
      primaryKeySerializer)

    val (startWindowRange, endWindowRange) = QueryableFinatraWindowStore.queryStartAndEndTime(
      windowSize,
      defaultQueryRange,
      startTime,
      endTime)

    val resultMap = new java.util.TreeMap[DateTimeMillis, mutable.Map[SK, V]]().asScala

    val store = FinatraStoresGlobalManager.getWindowedCompositeStore[PK, SK, V](
      stateDirFlag = stateDir,
      storeName = storeName,
      numPartitions = numQueryablePartitions,
      keyBytes = primaryKeyBytes)

    trace(
      s"Start Query $storeName ${store.taskId} $primaryKey $startCompositeKey $endCompositeKey ${startWindowRange.iso8601Millis} to ${endWindowRange.iso8601Millis} = $resultMap")

    var windowStartTime = startWindowRange
    while (windowStartTime <= endWindowRange) {
      queryWindow(
        store = store,
        startCompositeKey = startCompositeKey,
        endCompositeKey = endCompositeKey,
        windowStartTime = windowStartTime,
        allowStaleReads = allowStaleReads,
        windowedResultsMap = resultMap
      )

      windowStartTime = windowStartTime + windowSizeMillis
    }

    info(
      s"Query $storeName ${store.taskId} $primaryKey $startCompositeKey $endCompositeKey ${startWindowRange.iso8601Millis} to ${endWindowRange.iso8601Millis} = $resultMap")
    resultMap.toMap
  }

  /* Private */

  private def queryWindow(
    store: FinatraReadOnlyKeyValueStore[TimeWindowed[CompositeKey[PK, SK]], V],
    startCompositeKey: CompositeKey[PK, SK],
    endCompositeKey: CompositeKey[PK, SK],
    windowStartTime: DateTimeMillis,
    allowStaleReads: Boolean,
    windowedResultsMap: scala.collection.mutable.Map[
      WindowStartTime,
      scala.collection.mutable.Map[SK, V]
    ]
  ): Unit = {
    val iterator = store.range(
      TimeWindowed.forSize(start = Time(windowStartTime), windowSize, startCompositeKey),
      TimeWindowed.forSize(start = Time(windowStartTime), windowSize, endCompositeKey),
      allowStaleReads = allowStaleReads
    )

    while (iterator.hasNext) {
      val entry = iterator.next()
      trace(s"$store\t$entry")
      val windowedResult = windowedResultsMap.getOrElseUpdate(
        entry.key.start.millis,
        scala.collection.mutable.Map[SK, V]())

      windowedResult += (entry.key.value.secondary -> entry.value)
    }
  }
}
