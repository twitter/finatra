package com.twitter.finatra.kafkastreams.transformer.stores

import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.processor.TaskId
import org.apache.kafka.streams.state.{KeyValueIterator, ReadOnlyKeyValueStore}

/**
 * A Finatra key value store that only supports read operations.
 * Implementations should be thread-safe as concurrent reads and writes
 * are expected.
 */
trait FinatraReadOnlyKeyValueStore[K, V] extends ReadOnlyKeyValueStore[K, V] {

  /**
   * Returns the task id of this tore
   *
   * @return the task id of this store
   */
  def taskId: TaskId

  /**
   * Get an iterator over a given range of keys. This iterator must be closed after use.
   * The returned iterator must be safe from {@link java.util.ConcurrentModificationException}s
   * and must not return null values. No ordering guarantees are provided.
   *
   * @param from            The first key that could be in the range
   * @param to              The last key that could be in the range
   * @param allowStaleReads Allow stale reads when querying (Stale reads can occur when querying
   *                        key value stores with caching enabled).
   *
   * @return The iterator for this range.
   *
   * @throws NullPointerException       If null is used for from or to.
   * @throws InvalidStateStoreException if the store is not initialized
   */
  @throws[InvalidStateStoreException]
  def range(from: K, to: K, allowStaleReads: Boolean): KeyValueIterator[K, V]
}
