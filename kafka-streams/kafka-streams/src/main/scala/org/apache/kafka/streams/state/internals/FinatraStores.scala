package org.apache.kafka.streams.state.internals

import com.twitter.finagle.stats.StatsReceiver
import org.apache.kafka.common.serialization.Serde

/**
 * Factory for creating Finatra state stores
 * (used instead of [[org.apache.kafka.streams.state.Stores]]) for use
 * with [[com.twitter.finatra.kafkastreams.transformer.FinatraTransformer]]
 */
object FinatraStores {

  /**
   * Creates a [[FinatraKeyValueStoreBuilder]] than can be used to build a [[com.twitter.finatra.kafkastreams.transformer.stores.FinatraKeyValueStore]]
   *
   * @param statsReceiver StatsReceiver for use in the created [[com.twitter.finatra.kafkastreams.transformer.stores.FinatraKeyValueStore]]
   * @param supplier      a [[FinatraRocksDbKeyValueBytesStoreSupplier]]
   * @param keySerde      the key serde to use
   * @param valueSerde    the value serde to use
   * @tparam K key type
   * @tparam V value type
   *
   * @return an instance of a { @link StoreBuilder} that can build a { @link KeyValueStore}
   */
  def keyValueStoreBuilder[K, V](
    statsReceiver: StatsReceiver,
    supplier: FinatraRocksDbKeyValueBytesStoreSupplier,
    keySerde: Serde[K],
    valueSerde: Serde[V]
  ): FinatraKeyValueStoreBuilder[K, V] = {
    new FinatraKeyValueStoreBuilder[K, V](
      storeSupplier = supplier,
      statsReceiver = statsReceiver,
      keySerde = keySerde,
      valueSerde = valueSerde)
  }

  /**
   * Create a persistent [[FinatraRocksDbKeyValueBytesStoreSupplier]]
   *
   * @param name name of the store
   *
   * @return an instance of a [[FinatraRocksDbKeyValueBytesStoreSupplier]] that can be used
   *         to build a persistent store
   */
  def persistentKeyValueStore(
    name: String
  ): FinatraRocksDbKeyValueBytesStoreSupplier = {
    new FinatraRocksDbKeyValueBytesStoreSupplier(name)
  }
}
