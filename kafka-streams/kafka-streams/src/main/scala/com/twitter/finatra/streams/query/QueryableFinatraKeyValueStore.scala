package com.twitter.finatra.streams.query

import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.{
  KafkaPartitioner,
  StaticServiceShardPartitioner
}
import com.twitter.finatra.streams.stores.FinatraKeyValueStore
import com.twitter.finatra.streams.stores.internal.FinatraStoresGlobalManager
import com.twitter.inject.Logging
import java.util.NoSuchElementException
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.state.KeyValueIterator

//TODO: DRY with window store
class QueryableFinatraKeyValueStore[PK, K, V](
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
  def get(primaryKey: PK, key: K): Option[V] = {
    throwIfNonLocalKey(primaryKey)

    trace(s"Get $key")

    //TODO: Use store.taskId to find exact store where the key is assigned
    for (store <- stores) {
      val result = store.get(key)
      if (result != null) {
        return Some(result)
      }
    }

    None
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
    throwIfNonLocalKey(primaryKey)

    //TODO: Use store.taskId to find exact store where the key is assigned
    for (store <- stores) {
      val result = store.range(from, to)
      if (result.hasNext) {
        return result
      }
    }

    EmptyKeyValueIterator
  }

  private def throwIfNonLocalKey(primaryKey: PK): Unit = {
    val keyBytes = primaryKeySerializer.serialize("", primaryKey)
    val partitionsToQuery = partitioner.shardIds(keyBytes)
    if (partitionsToQuery.head != currentServiceShardId) {
      throw new Exception(s"Non local key. Query $partitionsToQuery")
    }
  }

  private def stores: Iterable[FinatraKeyValueStore[K, V]] = {
    FinatraStoresGlobalManager.getStores[K, V](storeName)
  }

  private object EmptyKeyValueIterator extends KeyValueIterator[K, V] {
    override def hasNext = false

    override def close(): Unit = {}

    override def peekNextKey = throw new NoSuchElementException

    override def next = throw new NoSuchElementException

    override def remove(): Unit = {}
  }
}
