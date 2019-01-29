package com.twitter.finatra.streams.transformer

import com.twitter.conversions.DurationOps._
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
  windowStart: (Time, K, V) => Time,
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
      start = windowStart(time, key, value),
      size = windowSize,
      value = key
    )

    val count = countToAggregate(key, value)
    if (windowedKey.isLate(allowedLateness, Watermark(watermark.timeMillis))) {
      restatementsCounter.incr()
      forward(windowedKey, WindowedValue(Restatement, count))
    } else {
      val newCount = stateStore.increment(windowedKey, count)
      if (newCount == count) {
        val closeTime = windowedKey.start + windowSize + allowedLateness
        if (emitOnClose) {
          timerStore.addTimer(closeTime, Close, windowedKey.start.millis)
        }
        timerStore.addTimer(
          closeTime + queryableAfterClose,
          Expire,
          windowedKey.start.millis
        )
      }
    }
  }

  private def onEventTimer(
    time: Time,
    timerMetadata: TimerMetadata,
    windowStartMs: WindowStartTime
  ): Unit = {
    val windowStart = Time(windowStartMs)
    val hourlyWindowIterator = stateStore.range(
      TimeWindowed.forSize(windowStart, windowSize, keyRangeStart),
      TimeWindowed.forSize(windowStart + 1.millis, windowSize, keyRangeStart)
    )

    try {
      if (timerMetadata == Close) {
        onClosed(windowStart, hourlyWindowIterator)
      } else {
        onExpired(hourlyWindowIterator)
      }
    } finally {
      hourlyWindowIterator.close()
    }
  }

  private def onClosed(
    windowStart: Time,
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
