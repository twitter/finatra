package com.twitter.finatra.streams.transformer

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.streams.transformer.FinatraTransformer.WindowStartTime
import com.twitter.finatra.streams.transformer.domain._
import com.twitter.util.Duration
import org.apache.kafka.streams.state.KeyValueIterator

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
      WindowStartTime,
      TimeWindowed[K],
      WindowedValue[
        Int
      ]](timerStoreName = timerStoreName, statsReceiver = statsReceiver, cacheTimers = true) {

  private val windowSizeMillis = windowSize.inMillis
  private val allowedLatenessMillis = allowedLateness.inMillis
  private val queryableAfterCloseMillis = queryableAfterClose.inMillis

  private val restatementsCounter = statsReceiver.counter("numRestatements")
  private val closedCounter = statsReceiver.counter("closedWindows")
  private val expiredCounter = statsReceiver.counter("expiredWindows")

  private val stateStore = getKeyValueStore[TimeWindowed[K], Int](stateStoreName)

  override def onMessage(time: Time, key: K, value: V): Unit = {
    val windowedKey = TimeWindowed.forSize(
      startMs = windowStart(time, key, value),
      sizeMs = windowSizeMillis,
      value = key
    )

    val count = countToAggregate(key, value)
    if (windowedKey.isLate(allowedLatenessMillis, Watermark(watermark))) {
      restatementsCounter.incr()
      forward(windowedKey, WindowedValue(Restatement, count))
    } else {
      val newCount = stateStore.increment(windowedKey, count)
      if (newCount == count) {
        val closeTime = windowedKey.startMs + windowSizeMillis + allowedLatenessMillis
        if (emitOnClose) {
          addEventTimeTimer(Time(closeTime), Close, windowedKey.startMs)
        }
        addEventTimeTimer(Time(closeTime + queryableAfterCloseMillis), Expire, windowedKey.startMs)
      }
    }
  }

  override def onEventTimer(
    time: Time,
    timerMetadata: TimerMetadata,
    windowStartMs: WindowStartTime,
    cursor: Option[TimeWindowed[K]]
  ): TimerResult[TimeWindowed[K]] = {
    val hourlyWindowIterator = stateStore.range(
      cursor getOrElse TimeWindowed.forSize(windowStartMs, windowSizeMillis, keyRangeStart),
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
  ): TimerResult[TimeWindowed[K]] = {
    windowIterator
      .take(maxActionsPerTimer)
      .foreach {
        case (key, value) =>
          forward(key = key, value = WindowedValue(resultState = WindowClosed, value = value))
      }

    deleteOrRetainTimer(windowIterator, closedCounter.incr())
  }

  private def onExpired(
    windowIterator: KeyValueIterator[TimeWindowed[K], Int]
  ): TimerResult[TimeWindowed[K]] = {
    windowIterator
      .take(maxActionsPerTimer)
      .foreach {
        case (key, value) =>
          stateStore.deleteWithoutGettingPriorValue(key)
      }

    deleteOrRetainTimer(windowIterator, expiredCounter.incr())
  }
}
