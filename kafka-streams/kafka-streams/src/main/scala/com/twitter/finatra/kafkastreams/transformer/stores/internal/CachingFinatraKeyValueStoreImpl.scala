package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finagle.stats.{Gauge, StatsReceiver}
import com.twitter.finatra.kafkastreams.transformer.stores.{CachingFinatraKeyValueStore, FinatraKeyValueStore}
import com.twitter.inject.Logging
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.util
import java.util.function.BiConsumer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore, TaskId}
import org.apache.kafka.streams.state.KeyValueIterator
import scala.reflect.ClassTag

/**
 * A write-behind caching layer around the FinatraKeyValueStore.
 *
 * We cache Java objects here and then periodically flush entries into RocksDB which involves
 * serializing the objects into byte arrays. As such this cache:
 * 1) Reduces the number of reads/writes to RocksDB
 * 2) Reduces the number of serialization/deserialization operations which can be expensive for some classes
 * 3) Reduces the number of publishes to the Kafka changelog topic backing this key value store
 *
 * This caching does introduce a few odd corner cases :-(
 * 1. Items in the cache have pass-by-reference semantics but items in rocksdb have pass-by-value semantics. Modifying items after a put is a bad idea! Ideally, only
 * immutable objects would be stored in a CachingFinatraKeyValueStore
 * 2. Range queries currently only work against the uncached RocksDB data.
 * This is because sorted Java maps are much less performant than their unsorted counterparts.
 * We typically only use range queries for queryable state where it is ok to read stale data
 * If fresher data is required for range queries, decrease your commit interval.
 *
 * This class is inspired by: https://github.com/apache/samza/blob/1.0.0/samza-kv/src/main/scala/org/apache/samza/storage/kv/CachedStore.scala
 */
class CachingFinatraKeyValueStoreImpl[K: ClassTag, V](
  statsReceiver: StatsReceiver,
  keyValueStore: FinatraKeyValueStore[K, V])
    extends CachingFinatraKeyValueStore[K, V]
    with Logging {

  private var numCacheEntriesGauge: Gauge = _

  private var _taskId: TaskId = _

  /* Regarding concurrency, Kafka Stream's Transformer interface assures us that only 1 thread will ever modify this map.
   * However, when using QueryableState, there may be concurrent readers of this map. FastUtil documentation says the following:
   * ** All classes are not synchronized.
   * ** If multiple threads access one of these classes concurrently, and at least one of the threads modifies it, it must be synchronized externally.
   * ** Iterators will behave unpredictably in the presence of concurrent modifications.
   * ** Reads, however, can be carried out concurrently.
   *
   * Since we only ever execute concurrent gets from queryable state and don't access iterators, we are safe performing these "gets" concurrently along with Transformer read and writes
   */
  private val objectCache = new Object2ObjectOpenHashMap[K, V]

  private var flushListener: (K, V) => Unit = _

  /*
      TODO: Consider making this a "batch consumer" so we could use keyValueStore.putAll which uses RocksDB WriteBatch
      which may lead to better performance...
      See: https://github.com/facebook/rocksdb/wiki/RocksDB-FAQ "Q: What's the fastest way to load data into RocksDB?"
      See: https://github.com/apache/samza/blob/1.0.0/samza-kv-rocksdb/src/main/scala/org/apache/samza/storage/kv/RocksDbKeyValueStore.scala#L175
   */
  private val flushListenerBiConsumer = new BiConsumer[K, V] {
    override def accept(key: K, value: V): Unit = {
      debug(s"flush_put($key -> $value")
      keyValueStore.put(key, value)
      if (flushListener != null) {
        flushListener(key, value)
      }
    }
  }

  /* Public */

  /**
   * Register a flush listener callback that will be called every time a cached key value store
   * entry is flushed into the underlying RocksDB store
   * @param listener Flush callback for cached entries
   */
  def registerFlushListener(listener: (K, V) => Unit): Unit = {
    assert(flushListener == null, "Can only currently call registerFlushListener once")
    flushListener = listener
  }

  override def taskId: TaskId = _taskId

  override def name(): String = keyValueStore.name

  override def init(processorContext: ProcessorContext, stateStore: StateStore): Unit = {
    _taskId = processorContext.taskId()

    numCacheEntriesGauge = statsReceiver
      .scope("stores")
      .scope(name)
      .addGauge(s"numCacheEntries")(objectCache.size())

    keyValueStore.init(processorContext, stateStore)
  }

  override def flush(): Unit = {
    trace("flush")
    flushObjectCache()
    keyValueStore.flush()
  }

  override def close(): Unit = {
    flushListener = null
    if (numCacheEntriesGauge != null) {
      numCacheEntriesGauge.remove()
      numCacheEntriesGauge = null
    }
    keyValueStore.close()
  }

  override def put(key: K, value: V): Unit = {
    trace(s"put($key -> $value")
    objectCache.put(key, value)
  }

  override def putIfAbsent(k: K, v: V): V = {
    objectCache.putIfAbsent(k, v)
  }

  override def putAll(list: util.List[KeyValue[K, V]]): Unit = {
    val iterator = list.iterator()
    while (iterator.hasNext) {
      val entry = iterator.next()
      objectCache.put(entry.key, entry.value)
    }
  }

  override def delete(k: K): V = {
    objectCache.remove(k)
    keyValueStore.delete(k)
  }

  override def get(k: K): V = {
    trace(s"get($k)")
    val cacheResult = objectCache.get(k)
    if (cacheResult != null) {
      cacheResult
    } else {
      keyValueStore.get(k)
    }
  }

  override def getOrDefault(k: K, default: => V): V = {
    trace(s"getOrDefault($k)")
    val result = get(k)
    if (result != null) {
      result
    } else {
      default
    }
  }

  override def deleteWithoutGettingPriorValue(key: K): Unit = {
    objectCache.remove(key)
    keyValueStore.put(key, null.asInstanceOf[V])
  }

  override def all(): KeyValueIterator[K, V] = {
    flushObjectCache()
    keyValueStore.all()
  }

  override def range(fromInclusive: K, toInclusive: K): KeyValueIterator[K, V] = {
    flushObjectCache()
    keyValueStore.range(fromInclusive, toInclusive)
  }

  override def range(fromBytesInclusive: Array[Byte]): KeyValueIterator[K, V] = {
    flushObjectCache()
    keyValueStore.range(fromBytesInclusive)
  }

  override def range(
    fromBytesInclusive: Array[Byte],
    toBytesExclusive: Array[Byte]
  ): KeyValueIterator[K, V] = {
    flushObjectCache()
    keyValueStore.range(fromBytesInclusive, toBytesExclusive)
  }

  override def range(
    fromInclusive: K,
    toInclusive: K,
    allowStaleReads: Boolean
  ): KeyValueIterator[K, V] = {
    trace(s"range($fromInclusive to $toInclusive)")
    if (allowStaleReads) {
      staleRange(fromInclusive, toInclusive)
    } else {
      flushObjectCache()
      keyValueStore.range(fromInclusive, toInclusive)
    }
  }

  override def deleteRangeExperimentalWithNoChangelogUpdates(
    beginKeyInclusive: Array[Byte],
    endKeyExclusive: Array[Byte]
  ): Unit = {
    flushObjectCache()
    keyValueStore.deleteRangeExperimentalWithNoChangelogUpdates(beginKeyInclusive, endKeyExclusive)
  }

  override def deleteRange(from: K, to: K): Unit = {
    flushObjectCache()
    keyValueStore.deleteRange(from, to)
  }

  override def approximateNumEntries(): Long = {
    keyValueStore.approximateNumEntries()
  }

  override def persistent(): Boolean = keyValueStore.persistent()

  override def isOpen: Boolean = keyValueStore.isOpen

  /* Private */

  private def flushObjectCache(): Unit = {
    if (!objectCache.isEmpty) {
      objectCache.forEach(flushListenerBiConsumer)
      objectCache.clear()
    }
  }

  /* A stale range read will occur for new keys (meaning that new keys will not be returned by this
   * method until a flush/commit. Existing keys with stale values in rocksdb will be
   * updated by checking the cache on the way out. In this way, we use RocksDB for efficient sorting
   * but can still leverage the most recent values in the cache... */
  private def staleRange(fromInclusive: K, toInclusive: K) = {
    new KeyValueIterator[K, V] {
      private val iterator = keyValueStore.range(fromInclusive, toInclusive)

      override def hasNext: Boolean = iterator.hasNext

      override def peekNextKey(): K = {
        iterator.peekNextKey()
      }

      override def next(): KeyValue[K, V] = {
        val result = iterator.next()
        val newerResultValue = objectCache.get(result.key)
        if (newerResultValue != null) {
          new KeyValue(result.key, newerResultValue)
        } else {
          result
        }
      }

      override def close(): Unit = {
        iterator.close()
      }
    }
  }

}
