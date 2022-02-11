package com.twitter.finatra.kafkastreams.query

import com.twitter.finatra.kafkastreams.transformer.stores.internal.FinatraStoresGlobalManager
import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.KafkaPartitioner
import com.twitter.finatra.streams.queryable.thrift.partitioning.StaticServiceShardPartitioner
import com.twitter.util.logging.Logging
import java.io.File
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.state.KeyValueIterator

/**
 * A queryable Finatra key value store for use by endpoints exposing queryable state
 */
class QueryableFinatraKeyValueStore[PK, K, V](
  stateDir: File,
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

  /**
   * Get the value corresponding to this key.
   *
   * @param key The key to fetch
   *
   * @return The value or null if no value is found.
   *
   * @throws NullPointerException       If null is used for key.
   * @throws InvalidStateStoreException if the store is not initialized
   */
  @throws[InvalidStateStoreException]
  def get(primaryKey: PK, key: K): Option[V] = {
    Option(
      FinatraStoresGlobalManager
        .getStore[K, V](stateDir, storeName, numQueryablePartitions, getPrimaryKeyBytes(primaryKey))
        .get(key))
  }

  /**
   * Get an iterator over a given range of keys. This iterator must be closed after use.
   * The returned iterator must be safe from {@link java.util.ConcurrentModificationException}s
   * and must not return null values. No ordering guarantees are provided.
   *
   * @param from The first key that could be in the range
   * @param to   The last key that could be in the range
   *
   * @return The iterator for this range.
   *
   * @throws NullPointerException       If null is used for from or to.
   * @throws InvalidStateStoreException if the store is not initialized
   */
  def range(primaryKey: PK, from: K, to: K): KeyValueIterator[K, V] = {
    FinatraStoresGlobalManager
      .getStore[K, V](stateDir, storeName, numQueryablePartitions, getPrimaryKeyBytes(primaryKey))
      .range(from, to)
  }

  /* Private */

  private def getPrimaryKeyBytes(primaryKey: PK): Array[Byte] = {
    FinatraStoresGlobalManager.primaryKeyBytesIfLocalKey(
      partitioner,
      currentServiceShardId,
      primaryKey,
      primaryKeySerializer)
  }
}
