package com.twitter.finatra.kafkastreams.transformer.stores

/**
 * A FinatraKeyValueStore with a callback that fires when an entry is flushed into the underlying store
 */
trait CachingFinatraKeyValueStore[K, V] extends FinatraKeyValueStore[K, V] {

  /**
   * Register a flush listener callback that will be called every time a cached key value store
   * entry is flushed into the underlying RocksDB store
   * @param listener Flush callback for cached entries
   */
  def registerFlushListener(listener: (K, V) => Unit): Unit
}
