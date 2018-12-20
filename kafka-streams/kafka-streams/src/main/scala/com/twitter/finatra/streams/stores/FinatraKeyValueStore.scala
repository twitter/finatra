package com.twitter.finatra.streams.stores

import com.twitter.finagle.stats.{Gauge, Stat, StatsReceiver}
import com.twitter.finatra.kafkastreams.internal.utils.ReflectionUtils
import com.twitter.finatra.streams.stores.FinatraKeyValueStore._
import com.twitter.finatra.streams.transformer.IteratorImplicits
import com.twitter.finatra.streams.transformer.domain.{DeleteTimer, RetainTimer, TimerResult}
import com.twitter.inject.Logging
import java.util
import java.util.concurrent.TimeUnit
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore}
import org.apache.kafka.streams.state.internals.{
  MeteredKeyValueBytesStore,
  RocksDBStore,
  RocksKeyValueIterator
}
import org.apache.kafka.streams.state.{KeyValueIterator, KeyValueStore, StateSerdes}
import org.rocksdb.{RocksDB, WriteOptions}
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object FinatraKeyValueStore {
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
  val FinatraDeleteLatencyStatName = "finatra_delete"
  val DeleteWithoutGettingPriorValueLatencyStatName = "delete_without_getting_prior_value"
  val FinatraRangeLatencyStatName = "finatra_range"
  val DeleteRangeExperimentalLatencyStatName = "delete_range_experimental"
}

class FinatraKeyValueStore[K: ClassTag, V](override val name: String, statsReceiver: StatsReceiver)
    extends KeyValueStore[K, V]
    with Logging
    with IteratorImplicits {

  private val latencyStatName: String = "latency_us"
  private val storeStatsScope: StatsReceiver = statsReceiver.scope("stores").scope(name)

  /* Private Mutable */
  private var _keyValueStore: MeteredKeyValueBytesStore[K, V] = _
  private var rocksDb: RocksDB = _
  private var writeOptions: WriteOptions = _
  private var serdes: StateSerdes[K, V] = _
  private var keySerializer: Serializer[K] = _
  private var keyDeserializer: Deserializer[K] = _
  private var valueDeserializer: Deserializer[V] = _
  private var numEntriesGauge: Gauge = _
  private var initLatencyStat: Stat = _
  private var closeLatencyStat: Stat = _
  private var putLatencyStat: Stat = _
  private var putIfAbsentLatencyStat: Stat = _
  private var putAllLatencyStat: Stat = _
  private var deleteLatencyStat: Stat = _
  private var flushLatencyStat: Stat = _
  private var persistentLatencyStat: Stat = _
  private var isOpenLatencyStat: Stat = _
  private var getLatencyStat: Stat = _
  private var rangeLatencyStat: Stat = _
  private var allLatencyStat: Stat = _
  private var approximateNumEntriesLatencyStat: Stat = _
  private var deleteRangeLatencyStat: Stat = _
  private var finatraDeleteLatencyStat: Stat = _
  private var deleteWithoutGettingPriorValueLatencyStat: Stat = _
  private var finatraRangeLatencyStat: Stat = _
  private var deleteRangeExperimentalLatencyStat: Stat = _

  /* Public */
  override def init(processorContext: ProcessorContext, root: StateStore): Unit = {
    initLatencyStat = storeStatsScope.scope(InitLatencyStatName).stat(latencyStatName)

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
      closeLatencyStat = storeStatsScope.scope(CloseLatencyStatName).stat(latencyStatName)
      putLatencyStat = storeStatsScope.scope(PutLatencyStatName).stat(latencyStatName)
      putIfAbsentLatencyStat =
        storeStatsScope.scope(PutIfAbsentLatencyStatName).stat(latencyStatName)
      putAllLatencyStat = storeStatsScope.scope(PutAllLatencyStatName).stat(latencyStatName)
      deleteLatencyStat = storeStatsScope.scope(DeleteLatencyStatName).stat(latencyStatName)
      flushLatencyStat = storeStatsScope.scope(FlushLatencyStatName).stat(latencyStatName)
      persistentLatencyStat = storeStatsScope.scope(PersistentLatencyStatName).stat(latencyStatName)
      isOpenLatencyStat = storeStatsScope.scope(IsOpenLatencyStatName).stat(latencyStatName)
      getLatencyStat = storeStatsScope.scope(GetLatencyStatName).stat(latencyStatName)
      rangeLatencyStat = storeStatsScope.scope(RangeLatencyStatName).stat(latencyStatName)
      allLatencyStat = storeStatsScope.scope(AllLatencyStatName).stat(latencyStatName)
      approximateNumEntriesLatencyStat =
        storeStatsScope.scope(ApproximateNumEntriesLatencyStatName).stat(latencyStatName)
      deleteRangeLatencyStat =
        storeStatsScope.scope(DeleteRangeLatencyStatName).stat(latencyStatName)
      finatraDeleteLatencyStat =
        storeStatsScope.scope(FinatraDeleteLatencyStatName).stat(latencyStatName)
      deleteWithoutGettingPriorValueLatencyStat =
        storeStatsScope.scope(DeleteWithoutGettingPriorValueLatencyStatName).stat(latencyStatName)
      finatraRangeLatencyStat =
        storeStatsScope.scope(FinatraRangeLatencyStatName).stat(latencyStatName)
      deleteRangeExperimentalLatencyStat =
        storeStatsScope.scope(DeleteRangeExperimentalLatencyStatName).stat(latencyStatName)
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

  override def isOpen: Boolean = meterLatency(isOpenLatencyStat)(keyValueStore.isOpen)

  override def get(k: K): V = meterLatency(getLatencyStat)(keyValueStore.get(k))

  override def range(k: K, k1: K): KeyValueIterator[K, V] =
    meterLatency(rangeLatencyStat)(keyValueStore.range(k, k1))

  override def all(): KeyValueIterator[K, V] = meterLatency(allLatencyStat)(keyValueStore.all())

  override def approximateNumEntries(): Long =
    meterLatency(approximateNumEntriesLatencyStat)(keyValueStore.approximateNumEntries())

  /* Finatra Additions */

  // TODO: Avoid reading all keys into memory
  def deleteRange(from: K, to: K, maxDeletes: Int = 25000): TimerResult[K] = {
    meterLatency(deleteRangeLatencyStat) {
      val iterator = range(from, to)
      try {
        val keysToDelete =
          iterator.asScala
            .take(maxDeletes)
            .map(_.key)
            .toArray

        delete(keysToDelete)
        deleteOrRetainTimer(iterator)
      } finally {
        iterator.close()
      }
    }
  }

  /**
   * An optimized version of delete that performs multiple deletes in a batch instead
   * of deleting each key one at a time
   *
   * Note: A RocksDB delete is equivalent to a put with a null value
   *
   * TODO: Avoid reading all keys into memory
   *
   * @param keys Keys to delete
   **/
  def delete(keys: Iterable[K]): Unit = {
    meterLatency(finatraDeleteLatencyStat) {
      val keysWithNullValue = keys.toSeq
        .map(key => new KeyValue[K, V](key, null.asInstanceOf[V]))

      keyValueStore.putAll(keysWithNullValue.asJava)
    }
  }

  // Optimization which avoid getting the prior value which keyValueStore.delete does :-/
  final def deleteWithoutGettingPriorValue(key: K): Unit = {
    meterLatency(deleteWithoutGettingPriorValueLatencyStat) {
      keyValueStore.put(key, null.asInstanceOf[V])
    }
  }

  final def getOrDefault(key: K, default: => V): V = {
    val existing = keyValueStore.get(key)
    if (existing == null) {
      default
    } else {
      existing
    }
  }

  /**
   * A range scan starting from bytes.
   *
   * Note 1: This is an API for Advanced users only
   *
   * Note 2: If this RocksDB instance is configured in "prefix seek mode", than fromBytes will be used as a "prefix" and the iteration will end when the prefix is no longer part of the next element.
   * Enabling "prefix seek mode" can be done by calling options.useFixedLengthPrefixExtractor. When enabled, prefix scans can take advantage of a prefix based bloom filter for better seek performance
   * See: https://github.com/facebook/rocksdb/wiki/Prefix-Seek-API-Changes
   */
  def range(fromBytes: Array[Byte]): KeyValueIterator[K, V] = {
    meterLatency(finatraRangeLatencyStat) {
      val iterator = rocksDb.newIterator() //TODO: Save off iterators to make sure they are all closed...
      iterator.seek(fromBytes)

      new RocksKeyValueIterator(iterator, keyDeserializer, valueDeserializer, keyValueStore.name)
    }
  }

  /*
     Removes the database entries in the range ["begin_key", "end_key"), i.e.,
     including "begin_key" and excluding "end_key". Returns OK on success, and
     a non-OK status on error. It is not an error if no keys exist in the range
     ["begin_key", "end_key").

     This feature is currently an experimental performance optimization for
     deleting very large ranges of contiguous keys. Invoking it many times or on
     small ranges may severely degrade read performance; in particular, the
     resulting performance can be worse than calling Delete() for each key in
     the range. Note also the degraded read performance affects keys outside the
     deleted ranges, and affects database operations involving scans, like flush
     and compaction.

     Consider setting ReadOptions::ignore_range_deletions = true to speed
     up reads for key(s) that are known to be unaffected by range deletions.
   */
  def deleteRangeExperimental(
    beginKeyInclusive: Array[Byte],
    endKeyExclusive: Array[Byte]
  ): Unit = {
    assert(
      false,
      "This method can only be used after integrating with the changelog (which currently occurs in ChangeLoggingKeyValueBytesStore)"
    )
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

  private def deleteOrRetainTimer(
    iterator: KeyValueIterator[K, _],
    onDeleteTimer: => Unit = () => ()
  ): TimerResult[K] = {
    if (iterator.hasNext) {
      RetainTimer(stateStoreCursor = iterator.peekNextKeyOpt, throttled = true)
    } else {
      onDeleteTimer
      DeleteTimer()
    }
  }

  private def keyValueStore: KeyValueStore[K, V] = {
    assert(
      _keyValueStore != null,
      "FinatraTransformer.getKeyValueStore must be called once outside of onMessage"
    )
    _keyValueStore
  }
}
