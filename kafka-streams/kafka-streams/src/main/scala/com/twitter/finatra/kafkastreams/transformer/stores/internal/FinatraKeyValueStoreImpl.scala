package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finagle.stats.{Gauge, Stat, StatsReceiver}
import com.twitter.finatra.kafkastreams.internal.utils.ReflectionUtils
import FinatraKeyValueStoreImpl._
import com.twitter.finatra.kafkastreams.transformer.stores.{FinatraKeyValueStore, FinatraReadOnlyKeyValueStore}
import com.twitter.finatra.kafkastreams.transformer.utils.IteratorImplicits
import com.twitter.finatra.kafkastreams.utils.RocksKeyValueIterator
import com.twitter.inject.Logging
import java.util
import java.util.Comparator
import java.util.concurrent.TimeUnit
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore, TaskId}
import org.apache.kafka.streams.state.internals.{MeteredKeyValueBytesStore, RocksDBStore}
import org.apache.kafka.streams.state.{KeyValueIterator, KeyValueStore, StateSerdes}
import org.rocksdb.{RocksDB, WriteOptions}
import scala.reflect.ClassTag

object FinatraKeyValueStoreImpl {
  val InitLatencyStatName = "init"
  val CloseLatencyStatName = "close"
  val PutLatencyStatName = "put"
  val PutIfAbsentLatencyStatName = "put_if_absent"
  val PutAllLatencyStatName = "put_all"
  val DeleteLatencyStatName = "delete"
  val FlushLatencyStatName = "flush"
  val PersistentLatencyStatName = "persistent"
  val IsOpenLatencyStatName = "is_open"
  val GetLatencyStatName = "get"
  val RangeLatencyStatName = "range"
  val AllLatencyStatName = "all"
  val ApproximateNumEntriesLatencyStatName = "approximate_num_entries"
  val DeleteRangeLatencyStatName = "delete_range"
  val DeleteWithoutGettingPriorValueLatencyStatName = "delete_without_getting_prior_value"
  val FinatraRangeLatencyStatName = "finatra_range"
  val DeleteRangeExperimentalLatencyStatName = "delete_range_experimental"
}

case class FinatraKeyValueStoreImpl[K: ClassTag, V](
  override val name: String,
  statsReceiver: StatsReceiver)
    extends KeyValueStore[K, V]
    with Logging
    with IteratorImplicits
    with FinatraKeyValueStore[K, V]
    with FinatraReadOnlyKeyValueStore[K, V] {

  private val latencyStatName: String = "latency_us"
  private val storeStatsScope: StatsReceiver = statsReceiver.scope("stores").scope(name)

  /* Private Mutable */
  private var _taskId: TaskId = _
  private var _keyValueStore: MeteredKeyValueBytesStore[K, V] = _
  private var rocksDb: RocksDB = _
  private var writeOptions: WriteOptions = _
  private var serdes: StateSerdes[K, V] = _
  private var keySerializer: Serializer[K] = _
  private var keyDeserializer: Deserializer[K] = _
  private var valueDeserializer: Deserializer[V] = _
  private var numEntriesGauge: Gauge = _

  /* Private Stats */
  private val initLatencyStat = createStat(InitLatencyStatName)
  private val closeLatencyStat = createStat(CloseLatencyStatName)
  private val putLatencyStat = createStat(PutLatencyStatName)
  private val putIfAbsentLatencyStat = createStat(PutIfAbsentLatencyStatName)
  private val putAllLatencyStat = createStat(PutAllLatencyStatName)
  private val deleteLatencyStat = createStat(DeleteLatencyStatName)
  private val flushLatencyStat = createStat(FlushLatencyStatName)
  private val persistentLatencyStat = createStat(PersistentLatencyStatName)
  private val isOpenLatencyStat = createStat(IsOpenLatencyStatName)
  private val getLatencyStat = createStat(GetLatencyStatName)
  private val rangeLatencyStat = createStat(RangeLatencyStatName)
  private val allLatencyStat = createStat(AllLatencyStatName)
  private val approximateNumEntriesLatencyStat = createStat(ApproximateNumEntriesLatencyStatName)
  private val deleteRangeLatencyStat = createStat(DeleteRangeLatencyStatName)
  private val deleteWithoutGettingPriorValueLatencyStat = createStat(
    DeleteWithoutGettingPriorValueLatencyStatName)
  private val finatraRangeLatencyStat = createStat(FinatraRangeLatencyStatName)
  private val deleteRangeExperimentalLatencyStat = createStat(
    DeleteRangeExperimentalLatencyStatName)

  /* Public */

  override def init(processorContext: ProcessorContext, root: StateStore): Unit = {
    _taskId = processorContext.taskId()

    meterLatency(initLatencyStat) {
      _keyValueStore = processorContext
        .getStateStore(name)
        .asInstanceOf[MeteredKeyValueBytesStore[K, V]]

      serdes = ReflectionUtils.getField[StateSerdes[K, V]](_keyValueStore, "serdes")
      keySerializer = serdes.keySerializer()
      keyDeserializer = serdes.keyDeserializer()
      valueDeserializer = serdes.valueDeserializer()

      _keyValueStore.inner() match {
        case rocksDbStore: RocksDBStore =>
          rocksDb = ReflectionUtils.getField[RocksDB](rocksDbStore, "db")
        case _ =>
          throw new Exception("FinatraTransformer only supports RocksDB State Stores")
      }

      writeOptions = new WriteOptions
      writeOptions.setDisableWAL(true)

      numEntriesGauge =
        storeStatsScope.addGauge(s"approxNumEntries")(_keyValueStore.approximateNumEntries)
    }
  }

  override def close(): Unit = {
    meterLatency(closeLatencyStat) {
      if (numEntriesGauge != null) {
        numEntriesGauge.remove()
      }
      numEntriesGauge = null

      _keyValueStore = null
      rocksDb = null

      if (writeOptions != null) {
        writeOptions.close()
        writeOptions = null
      }

      serdes = null
      keySerializer = null
      keyDeserializer = null
      valueDeserializer = null
    }
  }

  override def taskId: TaskId = _taskId

  override def put(key: K, value: V): Unit =
    meterLatency(putLatencyStat)(keyValueStore.put(key, value))

  override def putIfAbsent(k: K, v: V): V =
    meterLatency(putIfAbsentLatencyStat)(keyValueStore.putIfAbsent(k, v))

  override def putAll(list: util.List[KeyValue[K, V]]): Unit =
    meterLatency(putAllLatencyStat)(keyValueStore.putAll(list))

  override def delete(k: K): V = meterLatency(deleteLatencyStat)(keyValueStore.delete(k))

  override def flush(): Unit = meterLatency(flushLatencyStat)(keyValueStore.flush())

  override def persistent(): Boolean =
    meterLatency(persistentLatencyStat)(keyValueStore.persistent())

  override def isOpen: Boolean =
    _keyValueStore != null && meterLatency(isOpenLatencyStat)(keyValueStore.isOpen)

  override def get(key: K): V = meterLatency(getLatencyStat)(keyValueStore.get(key))

  override def range(from: K, to: K): KeyValueIterator[K, V] =
    meterLatency(rangeLatencyStat)(keyValueStore.range(from, to))

  override def range(from: K, to: K, allowStaleReads: Boolean): KeyValueIterator[K, V] =
    meterLatency(rangeLatencyStat)(keyValueStore.range(from, to))

  override def all(): KeyValueIterator[K, V] = meterLatency(allLatencyStat)(keyValueStore.all())

  override def approximateNumEntries(): Long =
    meterLatency(approximateNumEntriesLatencyStat)(keyValueStore.approximateNumEntries())

  /* Finatra Additions */

  override def deleteRange(from: K, to: K): Unit = {
    meterLatency(deleteRangeLatencyStat) {
      val iterator = range(from, to)
      try {
        while (iterator.hasNext) {
          delete(iterator.next.key)
        }
      } finally {
        iterator.close()
      }
    }
  }

  // Optimization which avoid getting the prior value which keyValueStore.delete does :-/
  override final def deleteWithoutGettingPriorValue(key: K): Unit = {
    meterLatency(deleteWithoutGettingPriorValueLatencyStat) {
      keyValueStore.put(key, null.asInstanceOf[V])
    }
  }

  override final def getOrDefault(key: K, default: => V): V = {
    val existing = keyValueStore.get(key)
    if (existing == null) {
      default
    } else {
      existing
    }
  }

  /**
   * A range scan starting from bytes. If RocksDB "prefix seek mode" is not enabled, than the iteration will NOT end when fromBytes is no longer the prefix
   *
   * Note 1: This is an API for Advanced users only
   *
   * Note 2: If this RocksDB instance is configured in "prefix seek mode", than fromBytes will be used as a "prefix" and the iteration will end when the prefix is no longer part of the next element.
   * Enabling "prefix seek mode" can be done by calling options.useFixedLengthPrefixExtractor. When enabled, prefix scans can take advantage of a prefix based bloom filter for better seek performance
   * See: https://github.com/facebook/rocksdb/wiki/Prefix-Seek-API-Changes
   */
  override def range(fromBytes: Array[Byte]): KeyValueIterator[K, V] = {
    meterLatency(finatraRangeLatencyStat) {
      val iterator = rocksDb.newIterator() //TODO: Save off iterators to make sure they are all closed...
      iterator.seek(fromBytes)

      new RocksKeyValueIterator(iterator, keyDeserializer, valueDeserializer, keyValueStore.name)
    }
  }

  override def range(
    fromBytesInclusive: Array[Byte],
    toBytesExclusive: Array[Byte]
  ): KeyValueIterator[K, V] =
    meterLatency(finatraRangeLatencyStat) {
      val iterator = rocksDb.newIterator()
      iterator.seek(fromBytesInclusive)

      new RocksKeyValueIterator(iterator, keyDeserializer, valueDeserializer, keyValueStore.name) {
        private val comparator: Comparator[Array[Byte]] = Bytes.BYTES_LEXICO_COMPARATOR

        override def hasNext: Boolean = {
          super.hasNext &&
            comparator.compare(iterator.key(), toBytesExclusive) < 0 // < 0 since to is exclusive
        }
      }
    }

  override def deleteRangeExperimentalWithNoChangelogUpdates(
    beginKeyInclusive: Array[Byte],
    endKeyExclusive: Array[Byte]
  ): Unit = {
    meterLatency(deleteRangeExperimentalLatencyStat) {
      rocksDb.deleteRange(beginKeyInclusive, endKeyExclusive)
    }
  }

  /* Private */

  private def meterLatency[T](stat: Stat)(operation: => T): T = {
    Stat.time[T](stat, TimeUnit.MICROSECONDS) {
      try {
        operation
      } catch {
        case e: Throwable =>
          error("Failure operation", e)
          throw e
      }
    }
  }

  private def keyValueStore: KeyValueStore[K, V] = {
    assert(
      _keyValueStore != null,
      "FinatraTransformer.getKeyValueStore must be called once outside of onMessage"
    )
    _keyValueStore
  }

  private def createStat(name: String) = {
    storeStatsScope.scope(name).stat(latencyStatName)
  }
}
