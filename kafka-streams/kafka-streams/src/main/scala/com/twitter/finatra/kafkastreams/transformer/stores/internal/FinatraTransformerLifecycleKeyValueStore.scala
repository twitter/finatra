package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finatra.kafkastreams.transformer.stores.FinatraKeyValueStore
import com.twitter.util.logging.Logging
import java.util
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.processor.TaskId
import org.apache.kafka.streams.state.KeyValueIterator
import com.twitter.finatra.kafkastreams.internal.utils.CompatibleUtils._

/**
 * A FinatraKeyValueStore which allows FinatraTransformer#getKeyValueStore to retrieve a key value
 * store in it's static constructor and then have that returned store participate in the
 * Kafka Streams init and close lifecycle
 *
 * @param name          State store name
 * @param flushListener Callback that will be called every time a cached store entry is flushed
 *                      into the underlying RocksDB store
 */
case class FinatraTransformerLifecycleKeyValueStore[K, V](
  name: String,
  flushListener: Option[(String, K, V) => Unit] = None)
    extends FinatraKeyValueStore[K, V]
    with Logging {

  @volatile private var keyValueStore: FinatraKeyValueStore[K, V] = _
  @volatile private var _processorContext: ProcessorContext = _

  /* Public */

  override def init(processorContext: ProcessorContext, root: StateStore): Unit = {
    debug(s"init ${processorContext.taskId}")
    this._processorContext = processorContext

    val unwrappedStateStore = processorContext.unwrap[StateStore, K, V](name)

    this.keyValueStore = unwrappedStateStore match {
      case cachingStore: CachingFinatraKeyValueStoreImpl[K, V] =>
        flushListener.foreach { listener =>
          cachingStore.registerFlushListener {
            case (k, v) =>
              listener(name, k, v)
          }
        }

        cachingStore

      case finatraKeyValueStore: FinatraKeyValueStore[K, V] =>
        assert(
          flushListener.isEmpty,
          s"Store $name was not configured to enable caching but a " +
            s"flushListener was specified when calling FinatraTransformer#getKeyValueStore. To " +
            s"enable caching, call keyValueStoreBuilder#withCachingEnabled() when configuring your store."
        )

        finatraKeyValueStore

      case store =>
        throw new Exception(
          s"FinatraTransformer cannot be used with $name $store since it isn't " +
            "a FinatraKeyValueStore. To fix this error, configure your state store with the " +
            "FinatraStores class instead of the Stores class. " +
            "Note: FinatraTransformer#getKeyValueStore does not currently work with global stores" +
            s"so if $name is a global store, you must use processorContext.getStateStore instead")
    }
  }

  override def close(): Unit = {
    debug(s"close $name")

    // We simply null out the underlying keyValueStore because it's retrieved using
    // processorContext.getStateStore and it's lifecycle (init and close) is managed by Kafka Streams
    keyValueStore = null

    _processorContext = null
  }

  override def taskId: TaskId = {
    if (keyValueStore == null) {
      throw new InvalidStateStoreException(name)
    }
    _processorContext.taskId()
  }

  override def range(from: K, to: K, allowStaleReads: Boolean): KeyValueIterator[K, V] =
    keyValueStore.range(from, to, allowStaleReads)

  override def deleteRange(from: K, to: K): Unit = keyValueStore.deleteRange(from, to)

  override def deleteWithoutGettingPriorValue(key: K): Unit =
    keyValueStore
      .deleteWithoutGettingPriorValue(key)

  override def getOrDefault(key: K, default: => V): V = keyValueStore.getOrDefault(key, default)

  override def range(fromBytes: Array[Byte]): KeyValueIterator[K, V] =
    keyValueStore
      .range(fromBytes)

  override def range(
    fromBytesInclusive: Array[Byte],
    toBytesExclusive: Array[Byte]
  ): KeyValueIterator[K, V] = keyValueStore.range(fromBytesInclusive, toBytesExclusive)

  override def deleteRangeExperimentalWithNoChangelogUpdates(
    beginKeyInclusive: Array[Byte],
    endKeyExclusive: Array[Byte]
  ): Unit =
    keyValueStore
      .deleteRangeExperimentalWithNoChangelogUpdates(beginKeyInclusive, endKeyExclusive)

  override def put(k: K, v: V): Unit = keyValueStore.put(k, v)

  override def putIfAbsent(k: K, v: V): V = keyValueStore.putIfAbsent(k, v)

  override def putAll(list: util.List[KeyValue[K, V]]): Unit = keyValueStore.putAll(list)

  override def delete(k: K): V = keyValueStore.delete(k)

  override def flush(): Unit = keyValueStore.flush()

  override def persistent(): Boolean = keyValueStore.persistent()

  override def isOpen: Boolean = keyValueStore.isOpen

  override def get(k: K): V = keyValueStore.get(k)

  override def range(k: K, k1: K): KeyValueIterator[K, V] = keyValueStore.range(k, k1)

  override def all(): KeyValueIterator[K, V] = keyValueStore.all()

  override def approximateNumEntries(): Long = keyValueStore.approximateNumEntries()
}
