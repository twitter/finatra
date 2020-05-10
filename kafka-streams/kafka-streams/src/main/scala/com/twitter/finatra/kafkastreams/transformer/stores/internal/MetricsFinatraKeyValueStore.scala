package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finagle.stats.{Gauge, Stat, StatsReceiver}
import com.twitter.finatra.kafkastreams.transformer.stores.FinatraKeyValueStore
import com.twitter.finatra.kafkastreams.transformer.stores.internal.MetricsFinatraKeyValueStore._
import java.util
import java.util.concurrent.TimeUnit
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore, TaskId}
import org.apache.kafka.streams.state.KeyValueIterator

object MetricsFinatraKeyValueStore {
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
  val GetOrDefaultLatencyStatName = "get_or_default"
  val RangeLatencyStatName = "range"
  val AllLatencyStatName = "all"
  val ApproximateNumEntriesLatencyStatName = "approximate_num_entries"
  val DeleteRangeLatencyStatName = "delete_range"
  val DeleteWithoutGettingPriorValueLatencyStatName = "delete_without_getting_prior_value"
  val FinatraRangeLatencyStatName = "finatra_range"
  val DeleteRangeExperimentalLatencyStatName = "delete_range_experimental"
}

/**
 * Metrics wrapper around a FinatraKeyValueStore used for recording latency of methods
 */
case class MetricsFinatraKeyValueStore[K, V](
  inner: FinatraKeyValueStore[K, V],
  statsReceiver: StatsReceiver)
    extends FinatraKeyValueStore[K, V] {

  /* Private Mutable Stats */

  @volatile private var numEntriesGauge: Gauge = _

  /* Private Stats */

  private val storeStatsScope = statsReceiver.scope("stores").scope(name)
  private val initLatencyStat = stat(InitLatencyStatName)
  private val closeLatencyStat = stat(CloseLatencyStatName)
  private val putLatencyStat = stat(PutLatencyStatName)
  private val putIfAbsentLatencyStat = stat(PutIfAbsentLatencyStatName)
  private val putAllLatencyStat = stat(PutAllLatencyStatName)
  private val deleteLatencyStat = stat(DeleteLatencyStatName)
  private val flushLatencyStat = stat(FlushLatencyStatName)
  private val persistentLatencyStat = stat(PersistentLatencyStatName)
  private val isOpenLatencyStat = stat(IsOpenLatencyStatName)
  private val getLatencyStat = stat(GetLatencyStatName)
  private val getOrDefaultLatencyStat = stat(GetOrDefaultLatencyStatName)
  private val rangeLatencyStat = stat(RangeLatencyStatName)
  private val allLatencyStat = stat(AllLatencyStatName)
  private val approximateNumEntriesLatencyStat = stat(ApproximateNumEntriesLatencyStatName)
  private val deleteRangeLatencyStat = stat(DeleteRangeLatencyStatName)
  private val deleteWithoutGettingPriorValueLatencyStat = stat(
    DeleteWithoutGettingPriorValueLatencyStatName)
  private val finatraRangeLatencyStat = stat(FinatraRangeLatencyStatName)
  private val deleteRangeExperimentalLatencyStat = stat(DeleteRangeExperimentalLatencyStatName)

  /* Public */

  override def name: String = inner.name()

  override def taskId: TaskId = inner.taskId

  override def init(processorContext: ProcessorContext, root: StateStore): Unit = {
    time(initLatencyStat) {
      inner.init(processorContext, root)

      numEntriesGauge = storeStatsScope.addGauge(s"approxNumEntries") {
        if (inner.isOpen) {
          inner.approximateNumEntries
        } else {
          -1 //TODO: What value should we return?
        }
      }
    }
  }

  override def close(): Unit = {
    time(closeLatencyStat) {
      if (numEntriesGauge != null) {
        numEntriesGauge.remove()
      }
      numEntriesGauge = null

      inner.close()
    }
  }

  override def put(key: K, value: V): Unit =
    time(putLatencyStat)(inner.put(key, value))

  override def putIfAbsent(k: K, v: V): V =
    time(putIfAbsentLatencyStat)(inner.putIfAbsent(k, v))

  override def putAll(list: util.List[KeyValue[K, V]]): Unit =
    time(putAllLatencyStat)(inner.putAll(list))

  override def delete(k: K): V = time(deleteLatencyStat)(inner.delete(k))

  override def flush(): Unit = time(flushLatencyStat)(inner.flush())

  override def persistent(): Boolean =
    time(persistentLatencyStat)(inner.persistent())

  override def isOpen: Boolean = {
    time(isOpenLatencyStat)(inner.isOpen)
  }

  override def get(key: K): V = time(getLatencyStat)(inner.get(key))

  override def range(from: K, to: K): KeyValueIterator[K, V] =
    time(rangeLatencyStat)(inner.range(from, to))

  override def range(from: K, to: K, allowStaleReads: Boolean): KeyValueIterator[K, V] =
    time(rangeLatencyStat)(inner.range(from, to))

  override def all(): KeyValueIterator[K, V] = time(allLatencyStat)(inner.all())

  override def approximateNumEntries(): Long =
    time(approximateNumEntriesLatencyStat)(inner.approximateNumEntries())

  override def deleteRange(from: K, to: K): Unit = {
    time(deleteRangeLatencyStat) {
      inner.deleteRange(from, to)
    }
  }

  override final def deleteWithoutGettingPriorValue(key: K): Unit = {
    time(deleteWithoutGettingPriorValueLatencyStat) {
      inner.deleteWithoutGettingPriorValue(key)
    }
  }

  override final def getOrDefault(key: K, default: => V): V = time(getOrDefaultLatencyStat) {
    inner.getOrDefault(key, default)
  }

  override def range(fromBytes: Array[Byte]): KeyValueIterator[K, V] = {
    time(finatraRangeLatencyStat) {
      inner.range(fromBytes)
    }
  }

  override def range(
    fromBytesInclusive: Array[Byte],
    toBytesExclusive: Array[Byte]
  ): KeyValueIterator[K, V] =
    time(finatraRangeLatencyStat) {
      inner.range(fromBytesInclusive, toBytesExclusive)
    }

  override def deleteRangeExperimentalWithNoChangelogUpdates(
    beginKeyInclusive: Array[Byte],
    endKeyExclusive: Array[Byte]
  ): Unit = {
    time(deleteRangeExperimentalLatencyStat) {
      inner.deleteRangeExperimentalWithNoChangelogUpdates(beginKeyInclusive, endKeyExclusive)
    }
  }

  /* Private */

  private def time[T](stat: Stat)(operation: => T): T = {
    Stat.time[T](stat, TimeUnit.MICROSECONDS)(operation)
  }

  private def stat(name: String): Stat = {
    storeStatsScope.scope(name).stat("latency_us")
  }
}
