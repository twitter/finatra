package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finatra.kafkastreams.internal.utils.ReflectionUtils
import com.twitter.finatra.kafkastreams.transformer.stores.FinatraKeyValueStore
import com.twitter.finatra.kafkastreams.utils.RocksKeyValueIterator
import com.twitter.util.logging.Logging
import java.util
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.processor.internals.ProcessorStateManager
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.processor.TaskId
import org.apache.kafka.streams.state.internals.RocksDBStore
import org.apache.kafka.streams.state.KeyValueIterator
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.StateSerdes
import org.rocksdb.RocksDB

class FinatraKeyValueStoreImpl[K, V](
  _rocksDBStore: RocksDBStore,
  inner: KeyValueStore[Bytes, Array[Byte]],
  keySerde: Serde[K],
  valueSerde: Serde[V])
    extends FinatraKeyValueStore[K, V]
    with Logging {

  /* Private Mutable */

  @transient private var _context: ProcessorContext = _
  @transient private var serdes: StateSerdes[K, V] = _
  @transient private var _rocksDB: RocksDB = _

  /* Public */

  override def init(processorContext: ProcessorContext, root: StateStore): Unit = {
    debug(s"init ${inner.name} ${processorContext.taskId()}")
    _context = processorContext

    serdes = new StateSerdes[K, V](
      ProcessorStateManager.storeChangelogTopic(processorContext.applicationId, name),
      keySerde,
      valueSerde)

    assert(!inner.isOpen)
    inner.init(processorContext, root)
  }

  override def close(): Unit = {
    info(s"close ${inner.name} ${_context.taskId()}")

    //Note: _rocksDB is obtained from the "inner" store via reflection. As such,
    //we null out the retrieved reference here and rely on calling inner.close() below to
    // properly close the resources associated with _rocksDB
    _rocksDB = null

    inner.close()
  }

  override def taskId: TaskId = _context.taskId()

  override def name(): String = inner.name()

  override def flush(): Unit = inner.flush()

  override def persistent(): Boolean = inner.persistent()

  override def isOpen: Boolean = inner.isOpen

  override def get(key: K): V = {
    valueFrom(inner.get(rawKey(key)))
  }

  override def put(key: K, value: V): Unit = {
    inner.put(rawKey(key), rawValue(value))
  }

  override def putIfAbsent(key: K, value: V): V = {
    valueFrom(inner.putIfAbsent(rawKey(key), rawValue(value)))
  }

  override def putAll(entries: util.List[KeyValue[K, V]]): Unit = {
    val byteEntries = new util.ArrayList[KeyValue[Bytes, Array[Byte]]]
    val iterator = entries.iterator
    while (iterator.hasNext) {
      val entry = iterator.next
      byteEntries.add(KeyValue.pair(rawKey(entry.key), rawValue(entry.value)))
    }

    inner.putAll(byteEntries)
  }

  override def range(from: K, to: K, allowStaleReads: Boolean): KeyValueIterator[K, V] = {
    // If this method is being handled here (as oppose to a caching store) then
    // we can ignore allowStaleReads and directly call range which will never serve stale reads
    range(from = from, to = to)
  }

  override def delete(key: K): V = {
    valueFrom(inner.delete(rawKey(key)))
  }

  override def approximateNumEntries(): Long = inner.approximateNumEntries()

  override def all(): KeyValueIterator[K, V] = {
    validateStoreOpen()
    val iterator = rocksDB.newIterator()
    iterator.seekToFirst()

    new RocksKeyValueIterator(
      iterator = iterator,
      keyDeserializer = serdes.keyDeserializer,
      valueDeserializer = serdes.valueDeserializer,
      storeName = name)
  }

  /**
   * Get an iterator over a given range of keys. This iterator must be closed after use.
   * The returned iterator must be safe from {@link java.util.ConcurrentModificationException}s
   * and must not return null values. No ordering guarantees are provided.
   *
   * @param from The first key that could be in the range
   * @param to   The last key that could be in the range (inclusive)
   *
   * @return The iterator for this range.
   *
   * @throws NullPointerException       If null is used for from or to.
   * @throws InvalidStateStoreException if the store is not initialized
   */
  override def range(from: K, to: K): KeyValueIterator[K, V] = {
    validateStoreOpen()

    val iterator = rocksDB.newIterator()
    iterator.seek(serdes.rawKey(from))

    val toBytesInclusive = serdes.rawKey(to)

    new RocksKeyValueIterator(
      iterator,
      serdes.keyDeserializer,
      serdes.valueDeserializer,
      inner.name) {
      private val comparator = Bytes.BYTES_LEXICO_COMPARATOR

      override def hasNext: Boolean = {
        super.hasNext &&
        comparator.compare(iterator.key(), toBytesInclusive) <= 0 // <= 0 since to is inclusive
      }
    }
  }

  /* Public Finatra Additions */

  override def range(
    fromBytesInclusive: Array[Byte],
    toBytesExclusive: Array[Byte]
  ): KeyValueIterator[K, V] = {
    validateStoreOpen()

    val iterator = rocksDB.newIterator()
    iterator.seek(fromBytesInclusive)

    new RocksKeyValueIterator(
      iterator,
      serdes.keyDeserializer,
      serdes.valueDeserializer,
      inner.name) {
      private val comparator = Bytes.BYTES_LEXICO_COMPARATOR

      override def hasNext: Boolean = {
        super.hasNext &&
        comparator.compare(iterator.key(), toBytesExclusive) < 0 // < 0 since to is exclusive
      }
    }
  }

  /**
   * A range scan starting from bytes. If RocksDB "prefix seek mode" is not enabled, than the
   * iteration will NOT end when fromBytes is no longer the prefix
   *
   * Note 1: This is an API for Advanced users only
   *
   * Note 2: If this RocksDB instance is configured in "prefix seek mode", than fromBytes will be
   * used as a "prefix" and the iteration will end when the prefix is no longer part of the next element.
   * Enabling "prefix seek mode" can be done by calling options.useFixedLengthPrefixExtractor.
   * When enabled, prefix scans can take advantage of a prefix based bloom filter for better seek performance
   * See: https://github.com/facebook/rocksdb/wiki/Prefix-Seek-API-Changes
   *
   * TODO: Save off iterators to make sure they are all closed
   */
  override def range(fromBytes: Array[Byte]): KeyValueIterator[K, V] = {
    val iterator = rocksDB.newIterator()
    iterator.seek(fromBytes)

    new RocksKeyValueIterator(
      iterator,
      serdes.keyDeserializer,
      serdes.valueDeserializer,
      inner.name)
  }

  override def deleteRange(from: K, to: K): Unit = {
    val iterator = range(from, to)
    try {
      while (iterator.hasNext) {
        delete(iterator.next.key)
      }
    } finally {
      iterator.close()
    }
  }

  // Optimization which avoid getting the prior value which keyValueStore.delete does :-/
  override final def deleteWithoutGettingPriorValue(key: K): Unit = {
    inner.put(rawKey(key), null)
  }

  override final def getOrDefault(key: K, default: => V): V = {
    val existing = inner.get(rawKey(key))
    if (existing == null) {
      default
    } else {
      valueFrom(existing)
    }
  }

  override def deleteRangeExperimentalWithNoChangelogUpdates(
    beginKeyInclusive: Array[Byte],
    endKeyExclusive: Array[Byte]
  ): Unit = {
    rocksDB.deleteRange(beginKeyInclusive, endKeyExclusive)
  }

  /* Private */

  private def rawKey(key: K): Bytes = {
    Bytes.wrap(serdes.rawKey(key))
  }

  private def rawValue(value: V): Array[Byte] = {
    serdes.rawValue(value)
  }

  private def valueFrom(bytes: Array[Byte]) = {
    serdes.valueFrom(bytes)
  }

  private def validateStoreOpen(): Unit = {
    if (!isOpen) throw new InvalidStateStoreException("Store " + this.name + " is currently closed")
  }

  /*
   * Note: We need to constantly check if _rocksDB is set and still owns a handle to the underlying
   * RocksDB resources because some operations (such as restoring state from the changelog) can
   * result in the underlying rocks store being closed and reopened
   * e.g. https://github.com/apache/kafka/blob/2.0/streams/src/main/java/org/apache/kafka/streams/state/internals/RocksDBStore.java#L233
   */
  private def rocksDB: RocksDB = {
    if (_rocksDB == null || !_rocksDB.isOwningHandle) {
      _rocksDB = ReflectionUtils.getFinalField(_rocksDBStore, "db")
    }
    _rocksDB
  }
}
