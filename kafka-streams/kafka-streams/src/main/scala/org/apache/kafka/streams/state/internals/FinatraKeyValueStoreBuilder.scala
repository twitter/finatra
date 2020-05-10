package org.apache.kafka.streams.state.internals

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafkastreams.transformer.stores.FinatraKeyValueStore
import com.twitter.finatra.kafkastreams.transformer.stores.internal.{
  CachingFinatraKeyValueStoreImpl,
  FinatraKeyValueStoreImpl,
  MetricsFinatraKeyValueStore
}
import java.util
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.state.{KeyValueStore, StoreBuilder}

/**
 * Store builder internally used for creating FinatraKeyValueStore's
 */
class FinatraKeyValueStoreBuilder[K, V](
  storeSupplier: FinatraRocksDbKeyValueBytesStoreSupplier,
  statsReceiver: StatsReceiver,
  keySerde: Serde[K],
  valueSerde: Serde[V])
    extends StoreBuilder[FinatraKeyValueStore[K, V]] {

  @volatile private var _logConfig: util.Map[String, String] = new util.HashMap
  @volatile private var enableCaching = false // Note: Caching is disabled by default

  override def build: FinatraKeyValueStore[K, V] = {
    val rocksDbStore = storeSupplier.get
    val maybeLoggedStore = maybeWrapLogging(rocksDbStore)
    val finatraStore =
      new FinatraKeyValueStoreImpl[K, V](rocksDbStore, maybeLoggedStore, keySerde, valueSerde)
    val metricsFinatraStore = new MetricsFinatraKeyValueStore[K, V](finatraStore, statsReceiver)
    maybeWrapCaching(metricsFinatraStore)
  }

  override def name(): String = storeSupplier.name

  override def withCachingEnabled(): StoreBuilder[FinatraKeyValueStore[K, V]] = {
    enableCaching = true
    this
  }

  override def withCachingDisabled(): StoreBuilder[FinatraKeyValueStore[K, V]] = {
    enableCaching = false
    this
  }

  override def withLoggingDisabled(): StoreBuilder[FinatraKeyValueStore[K, V]] = {
    //Note: We're calling clear for consistency with AbstractStoreBuilder
    _logConfig.clear()
    _logConfig = null
    this
  }

  override def withLoggingEnabled(
    config: util.Map[String, String]
  ): StoreBuilder[FinatraKeyValueStore[K, V]] = {
    _logConfig = config
    this
  }

  override def logConfig(): util.Map[String, String] = _logConfig

  override def loggingEnabled(): Boolean = _logConfig != null

  private def maybeWrapCaching(inner: FinatraKeyValueStore[K, V]): FinatraKeyValueStore[K, V] = {
    if (!enableCaching) {
      inner
    } else {
      new CachingFinatraKeyValueStoreImpl[K, V](inner, statsReceiver)
    }
  }

  private def maybeWrapLogging(
    inner: KeyValueStore[Bytes, Array[Byte]]
  ): KeyValueStore[Bytes, Array[Byte]] = {
    if (!loggingEnabled()) {
      inner
    } else {
      new ChangeLoggingKeyValueBytesStore(inner)
    }
  }
}
