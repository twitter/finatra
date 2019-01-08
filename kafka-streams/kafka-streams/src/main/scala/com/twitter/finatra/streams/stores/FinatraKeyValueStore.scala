package com.twitter.finatra.streams.stores
import com.twitter.finatra.streams.transformer.domain.TimerResult
import org.apache.kafka.streams.processor.TaskId
import org.apache.kafka.streams.state.{KeyValueIterator, KeyValueStore}

trait FinatraKeyValueStore[K, V] extends KeyValueStore[K, V] {

  /**
   * The task id associated with this store
   */
  def taskId: TaskId

  /**
   * Get an iterator over a given range of keys. This iterator must be closed after use.
   * The returned iterator must be safe from {@link java.util.ConcurrentModificationException}s
   * and must not return null values. No ordering guarantees are provided.
   *
   * @param from            The first key that could be in the range
   * @param to              The last key that could be in the range
   * @param allowStaleReads Allow stale reads when querying a caching key value store. If set to false,
   *                        each query will trigger a flush of the cache.
   *
   * @return The iterator for this range.
   *
   * @throws NullPointerException       If null is used for from or to.
   * @throws InvalidStateStoreException if the store is not initialized
   */
  def range(from: K, to: K, allowStaleReads: Boolean): KeyValueIterator[K, V]

  @deprecated("no longer supported", "1/7/2019")
  def deleteRange(from: K, to: K, maxDeletes: Int = 25000): TimerResult[K]

  /**
   * Delete the value from the store (if there is one)
   * Note: This version of delete avoids getting the prior value which keyValueStore.delete does
   *
   * @param key The key
   *
   * @return The old value or null if there is no such key.
   *
   * @throws NullPointerException If null is used for key.
   */
  def deleteWithoutGettingPriorValue(key: K): Unit

  /**
   * Get the value corresponding to this key or return the specified default value if no key is found
   *
   * @param key The key to fetch
   * @param default The default value to return if key is not found in the store
   *
   * @return The value associated with the key or the default value if the key is not found
   *
   * @throws NullPointerException       If null is used for key.
   * @throws InvalidStateStoreException if the store is not initialized
   */
  def getOrDefault(key: K, default: => V): V

  /**
   * A range scan starting from bytes.
   *
   * Note 1: This is an API for Advanced users only
   *
   * Note 2: If this RocksDB instance is configured in "prefix seek mode", than fromBytes will be used as a "prefix" and the iteration will end when the prefix is no longer part of the next element.
   * Enabling "prefix seek mode" can be done by calling options.useFixedLengthPrefixExtractor. When enabled, prefix scans can take advantage of a prefix based bloom filter for better seek performance
   * See: https://github.com/facebook/rocksdb/wiki/Prefix-Seek-API-Changes
   */
  def range(fromBytes: Array[Byte]): KeyValueIterator[K, V]

  /**
   * Get an iterator over a given range of keys. This iterator must be closed after use.
   * The returned iterator must be safe from {@link java.util.ConcurrentModificationException}s
   * and must not return null values. No ordering guarantees are provided.
   *
   * @param fromBytesInclusive Inclusive bytes to start the range scan
   * @param toBytesExclusive Exclusive bytes to end the range scan
   *
   * @return The iterator for this range.
   *
   * @throws NullPointerException       If null is used for from or to.
   * @throws InvalidStateStoreException if the store is not initialized
   */
  def range(fromBytesInclusive: Array[Byte], toBytesExclusive: Array[Byte]): KeyValueIterator[K, V]

  /**
     Removes the database entries in the range ["begin_key", "end_key"), i.e.,
     including "begin_key" and excluding "end_key". Returns OK on success, and
     a non-OK status on error. It is not an error if no keys exist in the range
     ["begin_key", "end_key").

     This feature is currently an experimental performance optimization for
     deleting very large ranges of contiguous keys. Invoking it many times or on
     small ranges may severely degrade read performance; in particular, the
     resulting performance can be worse than calling Delete() for each key in
     the range. Note also the degraded read performance affects keys outside the
     deleted ranges, and affects database operations involving scans, like flush
     and compaction.

     Consider setting ReadOptions::ignore_range_deletions = true to speed
     up reads for key(s) that are known to be unaffected by range deletions.

     Note: Changelog entries will not be deleted, so this method is best used
     when relying on retention.ms to delete entries from the changelog
   */
  def deleteRangeExperimentalWithNoChangelogUpdates(
    beginKeyInclusive: Array[Byte],
    endKeyExclusive: Array[Byte]
  ): Unit

  /*
     Note: We define equals and hashcode so we can store Finatra Key Value stores in maps for
     retrieval when implementing queryable state
   */

  override def equals(other: Any): Boolean = other match {
    case that: FinatraKeyValueStore[_, _] =>
      taskId == that.taskId &&
        name == that.name()
    case _ => false
  }

  override def hashCode(): Int = {
    31 * taskId.hashCode() + name.hashCode
  }
}
