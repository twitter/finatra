package com.twitter.finatra.kafkastreams.transformer.stores

import com.google.common.annotations.Beta
import com.twitter.finatra.kafkastreams.transformer.domain.{Time, TimerMetadata}
import com.twitter.finatra.kafkastreams.transformer.lifecycle.{OnInit, OnWatermark}
import com.twitter.finatra.kafkastreams.transformer.stores.internal.Timer
import com.twitter.finatra.kafkastreams.transformer.watermarks.Watermark
import java.util
import org.apache.kafka.streams.processor.PunctuationType
import scala.reflect.ClassTag

/**
 * Per-Key Persistent Timers inspired by Flink's ProcessFunction:
 * https://ci.apache.org/projects/flink/flink-docs-stable/dev/stream/operators/process_function.html
 *
 * @note Timers are based on a sorted RocksDB KeyValueStore
 * @note Timers that fire at the same time MAY NOT fire in the order which they were added
 */
@Beta
trait PersistentTimers extends OnWatermark with OnInit {
  private val timerValueStoresMap =
    scala.collection.mutable.Map[String, PersistentTimerValueStore[_, _]]()
  private val timerValueStores = new util.ArrayList[PersistentTimerValueStore[_, _]]

  protected def getKeyValueStore[KK: ClassTag, VV](name: String): FinatraKeyValueStore[KK, VV]

  override def onInit(): Unit = {
    val valueIterator = timerValueStores.iterator
    while (valueIterator.hasNext) {
      valueIterator.next.onInit()
    }
    super.onInit()
  }

  /**
   * Creates a key-only timer store. The timer store is implemented using a
   * PersistentTimerValueStore, but the values stored are just zero-length
   * byte arrays.
   *
   * @param timerStoreName The name of the timerstore
   * @param onTimer A function that is called when the timer is triggered
   * @param punctuationType Must be STREAM_TIME, WALL_CLOCK_TIME is not supported
   * @param maxTimerFiresPerWatermark Maximum number of timers can can fire per watermark event
   * @tparam TimerKey The type of the key for each timer
   */
  protected def getPersistentTimerStore[TimerKey](
    timerStoreName: String,
    onTimer: (Time, TimerMetadata, TimerKey) => Unit,
    punctuationType: PunctuationType,
    maxTimerFiresPerWatermark: Int = 10000
  ): PersistentTimerStore[TimerKey] = {
    assert(punctuationType == PunctuationType.STREAM_TIME) //TODO: Support WALL CLOCK TIME

    val store = new PersistentTimerStore[TimerKey](
      timersStore = getKeyValueStore[Timer[TimerKey], Array[Byte]](timerStoreName),
      onTimer = onTimer,
      maxTimerFiresPerWatermark = maxTimerFiresPerWatermark)

    assert(
      timerValueStoresMap.put(timerStoreName, store).isEmpty,
      s"getPersistentTimerStore already called for $timerStoreName")

    timerValueStores.add(store)

    store
  }

  /**
   * Creates a key/value timer store. When addTimer is called on the timer store with
   * a time, key, and value, a timer is stored in the store which triggered as the specified
   * time. The specified `onTimer` function is then called with the key and value.
   *
   * @param timerStoreName The name of the timerstore, created separately
   * @param onTimer A function that is called when the timer is triggered
   * @param punctuationType Must be STREAM_TIME, WALL_CLOCK_TIME is not supported
   * @param maxTimerFiresPerWatermark Maximum number of timers can can fire per watermark event
   * @tparam TimerKey The type of the key for each timer
   * @tparam TimerValue The type of the value stored for each timer
   */
  protected def getPersistentTimerValueStore[TimerKey, TimerValue](
    timerStoreName: String,
    onTimer: (Time, TimerMetadata, TimerKey, TimerValue) => Unit,
    punctuationType: PunctuationType,
    maxTimerFiresPerWatermark: Int = 10000
  ): PersistentTimerValueStore[TimerKey, TimerValue] = {
    assert(punctuationType == PunctuationType.STREAM_TIME) //TODO: Support WALL CLOCK TIME

    val store = new PersistentTimerValueStore[TimerKey, TimerValue](
      timersStore = getKeyValueStore[Timer[TimerKey], TimerValue](timerStoreName),
      onTimer = onTimer,
      maxTimerFiresPerWatermark = maxTimerFiresPerWatermark)

    assert(
      timerValueStoresMap.put(timerStoreName, store).isEmpty,
      s"getPersistentTimerStore already called for $timerStoreName")

    timerValueStores.add(store)

    store
  }

  //TODO: protected def getCursoredTimerStore[TimerKey, CursorKey] ...

  final override def onWatermark(watermark: Watermark): Unit = {
    val iterator = timerValueStores.iterator
    while (iterator.hasNext) {
      iterator.next.onWatermark(watermark)
    }
  }
}
