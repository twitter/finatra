package com.twitter.finatra.streams.transformer

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafkastreams.processors.FlushingTransformer
import com.twitter.finatra.streams.stores.internal.{
  CachingFinatraKeyValueStoreImpl,
  FinatraKeyValueStoreImpl,
  FinatraStoresGlobalManager
}
import com.twitter.finatra.streams.stores.{CachingFinatraKeyValueStore, FinatraKeyValueStore}
import scala.collection.mutable
import scala.reflect.ClassTag

trait CachingKeyValueStores[K, V, K1, V1] extends FlushingTransformer[K, V, K1, V1] {

  protected def statsReceiver: StatsReceiver

  protected def finatraKeyValueStoresMap: mutable.Map[String, FinatraKeyValueStore[_, _]]

  override def onFlush(): Unit = {
    super.onFlush()
    finatraKeyValueStoresMap.values.foreach(_.flush())
  }

  /**
   * Lookup a caching key value store by name
   * @param name The name of the store
   * @tparam KK Type of keys in the store
   * @tparam VV Type of values in the store
   * @return A caching key value store
   */
  protected def getCachingKeyValueStore[KK: ClassTag, VV](
    name: String
  ): CachingFinatraKeyValueStore[KK, VV] = {
    val store = new CachingFinatraKeyValueStoreImpl[KK, VV](
      statsReceiver,
      new FinatraKeyValueStoreImpl[KK, VV](name, statsReceiver))

    val previousStore = finatraKeyValueStoresMap.put(name, store)
    assert(
      previousStore.isEmpty,
      s"getCachingKeyValueStore was called for store $name more than once")
    FinatraStoresGlobalManager.addStore(store)

    store
  }

}
