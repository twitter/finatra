package com.twitter.finatra.streams.transformer

import com.google.common.annotations.Beta
import com.twitter.finatra.streams.stores.FinatraKeyValueStore
import com.twitter.finatra.streams.transformer.domain.{Time, TimerMetadata, Watermark}
import com.twitter.finatra.streams.transformer.internal.OnInit
import com.twitter.finatra.streams.transformer.internal.domain.Timer
import java.util
import org.apache.kafka.streams.processor.PunctuationType
import scala.reflect.ClassTag

/**
 * Per-Key Persistent Timers inspired by Flink's ProcessFunction:
 * https://ci.apache.org/projects/flink/flink-docs-stable/dev/stream/operators/process_function.html
 *
 * Note: Timers are based on a sorted RocksDB KeyValueStore
 * Note: Timers that fire at the same time MAY NOT fire in the order which they were added
 */
@Beta
trait PersistentTimers extends OnWatermark with OnInit {

  private val timerStoresMap = scala.collection.mutable.Map[String, PersistentTimerStore[_]]()
  private val timerStores = new util.ArrayList[PersistentTimerStore[_]]

  protected def getKeyValueStore[KK: ClassTag, VV](name: String): FinatraKeyValueStore[KK, VV]

  override def onInit(): Unit = {
    val iterator = timerStores.iterator
    while (iterator.hasNext) {
      iterator.next.onInit()
    }
    super.onInit()
  }

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
      timerStoresMap.put(timerStoreName, store).isEmpty,
      s"getPersistentTimerStore already called for $timerStoreName")

    timerStores.add(store)

    store
  }

  //TODO: protected def getCursoredTimerStore[TimerKey, CursorKey] ...

  final override def onWatermark(watermark: Watermark): Unit = {
    val iterator = timerStores.iterator
    while (iterator.hasNext) {
      iterator.next.onWatermark(watermark)
    }
  }
}
