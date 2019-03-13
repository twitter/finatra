package com.twitter.finatra.streams.queryable.thrift

import com.twitter.app.Flag
import com.twitter.finatra.kafkastreams.partitioning.StaticPartitioning
import com.twitter.finatra.kafkastreams.query.{QueryableFinatraCompositeWindowStore, QueryableFinatraKeyValueStore, QueryableFinatraWindowStore}
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.Serde

/**
 * Trait to mix into a Kafka Streams Server exposing queryable state
 */
trait QueryableState extends StaticPartitioning {
  protected val currentShard: Flag[Int] = flag[Int]("kafka.current.shard", "")
  protected val numQueryablePartitions: Flag[Int] = flag[Int]("kafka.num.queryable.partitions", "")

  /**
   * Returns a queryable Finatra key value store
   * @param storeName Name of the queryable store
   * @param primaryKeySerde Serde of the primary key being queried which is used to determine
   *                        which queryable store is responsible for they key being queried
   * @tparam PK Type of the primary key
   * @tparam K  Type of the key being queried
   * @tparam V  Type of the value associated with the key being queried
   * @return A QueryableFinatraKeyValueStore
   */
  protected def queryableFinatraKeyValueStore[PK, K, V](
    storeName: String,
    primaryKeySerde: Serde[PK]
  ): QueryableFinatraKeyValueStore[PK, K, V] = {
    new QueryableFinatraKeyValueStore[PK, K, V](
      storeName,
      primaryKeySerde,
      numApplicationInstances(),
      numQueryablePartitions(),
      currentShard())
  }

  /**
   * Returns a queryable Finatra window store
   * @param storeName Name of the queryable store
   * @param windowSize Size of the windows being queried
   * @param allowedLateness Allowed lateness for the windows being queried
   * @param queryableAfterClose Time the window being queried will exist after closing
   * @param primaryKeySerde Serde of the primary key being queried which is used to determine
   *                        which queryable store is responsible for they key being queried
   * @tparam K  Type of the key being queried
   * @tparam V  Type of the value associated with the key being queried
   * @return A QueryableFinatraWindowStore
   */
  protected def queryableFinatraWindowStore[K, V](
    storeName: String,
    windowSize: Duration,
    allowedLateness: Duration,
    queryableAfterClose: Duration,
    primaryKeySerde: Serde[K]
  ): QueryableFinatraWindowStore[K, V] = {
    new QueryableFinatraWindowStore[K, V](
      storeName,
      windowSize = windowSize,
      allowedLateness = allowedLateness,
      queryableAfterClose = queryableAfterClose,
      keySerde = primaryKeySerde,
      numShards = numApplicationInstances(),
      numQueryablePartitions = numQueryablePartitions(),
      currentShardId = currentShard())
  }

  /**
   * Returns a queryable Finatra composite window store (composite windows contain composite keys
   * which contain a primary and secondary key)
   *
   * @param storeName Name of the queryable store
   * @param windowSize Size of the windows being queried
   * @param allowedLateness Allowed lateness for the windows being queried
   * @param queryableAfterClose Time the window being queried will exist after closing
   * @param primaryKeySerde Serde of the primary key being queried which is used to determine
   *                        which queryable store is responsible for they key being queried
   * @tparam PK Type of the primary key
   * @tparam K  Type of the key being queried
   * @tparam V  Type of the value associated with the key being queried
   * @return A QueryableFinatraCompositeWindowStore
   */
  protected def queryableFinatraCompositeWindowStore[PK, SK, V](
    storeName: String,
    windowSize: Duration,
    allowedLateness: Duration,
    queryableAfterClose: Duration,
    primaryKeySerde: Serde[PK]
  ): QueryableFinatraCompositeWindowStore[PK, SK, V] = {
    new QueryableFinatraCompositeWindowStore[PK, SK, V](
      storeName,
      windowSize = windowSize,
      allowedLateness = allowedLateness,
      queryableAfterClose = queryableAfterClose,
      primaryKeySerde = primaryKeySerde,
      numShards = numApplicationInstances(),
      numQueryablePartitions = numQueryablePartitions(),
      currentShardId = currentShard())
  }
}
