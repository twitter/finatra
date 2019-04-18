package com.twitter.finatra.kafkastreams.transformer.stores

import com.twitter.finatra.kafkastreams.flushing.FlushingTransformer
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import scala.reflect.ClassTag

/**
 * Trait to mix into a FinatraTransformer which is used
 * to access a caching enabled Finatra Store
 *
 * @tparam K Transformer key input type
 * @tparam V Transformer value input type
 * @tparam K1 Transformer key return type
 * @tparam V1 Transformer value return type
 */
trait CachingKeyValueStores[K, V, K1, V1]
    extends FinatraTransformer[K, V, K1, V1]
    with FlushingTransformer[K, V, K1, V1] {

  override def onFlush(): Unit = {
    super.onFlush()
    finatraKeyValueStoresMap.values.foreach(_.flush())
  }

  /**
   * Lookup a caching key value store by name
   * @param name The name of the store
   * @param flushListener Callback that will be called every time a cached store entry is flushed
   *                      into the underlying RocksDB store
   * @tparam KK Type of keys in the store
   * @tparam VV Type of values in the store
   * @return A caching key value store
   */
  protected def getCachingKeyValueStore[KK: ClassTag, VV](
    name: String,
    flushListener: (String, KK, VV) => Unit
  ): FinatraKeyValueStore[KK, VV] = {
    getKeyValueStore(name, Some(flushListener))
  }
}
