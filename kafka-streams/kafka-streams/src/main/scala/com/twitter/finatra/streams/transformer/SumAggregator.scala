package com.twitter.finatra.streams.transformer

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.streams.transformer.FinatraTransformer.WindowStartTime
import com.twitter.finatra.streams.transformer.domain._
import com.twitter.finatra.streams.transformer.internal.StateStoreImplicits
import com.twitter.util.Duration
import org.apache.kafka.streams.state.KeyValueIterator
import org.apache.kafka.streams.processor.PunctuationType

@deprecated("Use AggregatorTransformer")
class SumAggregator[K, V](
  commitInterval: Duration,
  keyRangeStart: K,
  statsReceiver: StatsReceiver,
  stateStoreName: String,
  timerStoreName: String,
  windowSize: Duration,
  allowedLateness: Duration,
  queryableAfterClose: Duration,
  windowStart: (Time, K, V) => Long,
  countToAggregate: (K, V) => Int,
  emitOnClose: Boolean = true,
  maxActionsPerTimer: Int = 25000)
    extends FinatraTransformer[
      K,
      V,
      TimeWindowed[K],
      WindowedValue[
        Int
      ]](statsReceiver = statsReceiver)
    with PersistentTimers
    with StateStoreImplicits
    with IteratorImplicits {

  private val windowSizeMillis = windowSize.inMillis
  private val allowedLatenessMillis = allowedLateness.inMillis
  private val queryableAfterCloseMillis = queryableAfterClose.inMillis

  private val restatementsCounter = statsReceiver.counter("numRestatements")
  private val closedCounter = statsReceiver.counter("closedWindows")
  private val expiredCounter = statsReceiver.counter("expiredWindows")

  private val stateStore = getKeyValueStore[TimeWindowed[K], Int](stateStoreName)
  private val timerStore = getPersistentTimerStore[WindowStartTime](
    timerStoreName,
    onEventTimer,
    PunctuationType.STREAM_TIME
  )

  override def onMessage(time: Time, key: K, value: V): Unit = {
    val windowedKey = TimeWindowed.forSize(
      startMs = windowStart(time, key, value),
      sizeMs = windowSizeMillis,
      value = key
    )

    val count = countToAggregate(key, value)
    if (windowedKey.isLate(allowedLatenessMillis, Watermark(watermark.timeMillis))) {
      restatementsCounter.incr()
      forward(windowedKey, WindowedValue(Restatement, count))
    } else {
      val newCount = stateStore.increment(windowedKey, count)
      if (newCount == count) {
        val closeTime = windowedKey.startMs + windowSizeMillis + allowedLatenessMillis
        if (emitOnClose) {
          timerStore.addTimer(Time(closeTime), Close, windowedKey.startMs)
        }
        timerStore.addTimer(
          Time(closeTime + queryableAfterCloseMillis),
          Expire,
          windowedKey.startMs
        )
      }
    }
  }

  private def onEventTimer(
    time: Time,
    timerMetadata: TimerMetadata,
    windowStartMs: WindowStartTime
  ): Unit = {
    val hourlyWindowIterator = stateStore.range(
      TimeWindowed.forSize(windowStartMs, windowSizeMillis, keyRangeStart),
      TimeWindowed.forSize(windowStartMs + 1, windowSizeMillis, keyRangeStart)
    )

    try {
      if (timerMetadata == Close) {
        onClosed(windowStartMs, hourlyWindowIterator)
      } else {
        onExpired(hourlyWindowIterator)
      }
    } finally {
      hourlyWindowIterator.close()
    }
  }

  private def onClosed(
    windowStartMs: Long,
    windowIterator: KeyValueIterator[TimeWindowed[K], Int]
  ): Unit = {
    windowIterator
      .take(maxActionsPerTimer)
      .foreach {
        case (key, value) =>
          forward(key = key, value = WindowedValue(resultState = WindowClosed, value = value))
      }

    closedCounter.incr()
  }

  private def onExpired(windowIterator: KeyValueIterator[TimeWindowed[K], Int]): Unit = {
    windowIterator
      .take(maxActionsPerTimer)
      .foreach {
        case (key, value) =>
          stateStore.deleteWithoutGettingPriorValue(key)
      }

    expiredCounter.incr()
  }
}
