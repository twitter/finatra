package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.google.common.collect.{ArrayListMultimap, Multimaps}
import com.twitter.finatra.kafkastreams.transformer.aggregation.TimeWindowed
import com.twitter.finatra.kafkastreams.transformer.domain.CompositeKey
import com.twitter.finatra.kafkastreams.transformer.stores.FinatraKeyValueStore
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
 * This class stores a global list of all Finatra Key Value Stores for use by Finatra's
 * queryable state functionality.
 *
 * Note: We maintain or own global list of state stores so we can retrieve Finatra
 * FinatraKeyValueStore implementations directly for querying (the alternative would be to retrieve
 * the underlying RocksDBStore which is wrapped by the FinatraKeyValueStore implementations).
 */
object FinatraStoresGlobalManager {

  private[stores] val queryableStateStoreNameToStores =
    Multimaps.synchronizedMultimap(ArrayListMultimap.create[String, FinatraKeyValueStore[_, _]]())

  /**
   * @return True if the added store was new
   */
  def addStore[VV, KK: ClassTag](store: FinatraKeyValueStore[KK, VV]): Boolean = {
    queryableStateStoreNameToStores.put(store.name, store)
  }

  def removeStore(store: FinatraKeyValueStore[_, _]): Unit = {
    queryableStateStoreNameToStores.remove(store.name, store)
  }

  def getWindowedCompositeStores[PK, SK, V](
    storeName: String
  ): Iterable[FinatraKeyValueStore[TimeWindowed[CompositeKey[PK, SK]], V]] = {
    queryableStateStoreNameToStores
      .get(storeName)
      .asScala
      .asInstanceOf[Iterable[FinatraKeyValueStore[TimeWindowed[CompositeKey[PK, SK]], V]]]
      .filter(_.isOpen)
  }

  def getWindowedStores[K, V](
    storeName: String
  ): Iterable[FinatraKeyValueStore[TimeWindowed[K], V]] = {
    queryableStateStoreNameToStores
      .get(storeName)
      .asScala
      .asInstanceOf[Iterable[FinatraKeyValueStore[TimeWindowed[K], V]]]
      .filter(_.isOpen)
  }

  def getStores[K, V](storeName: String): Iterable[FinatraKeyValueStore[K, V]] = {
    queryableStateStoreNameToStores
      .get(storeName)
      .asScala
      .asInstanceOf[Iterable[FinatraKeyValueStore[K, V]]]
      .filter(_.isOpen)
  }
}
