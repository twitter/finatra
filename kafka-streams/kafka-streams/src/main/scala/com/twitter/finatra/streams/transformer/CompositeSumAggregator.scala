package com.twitter.finatra.streams.transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.streams.transformer.FinatraTransformer.WindowStartTime
import com.twitter.finatra.streams.transformer.domain._
import com.twitter.finatra.streams.transformer.internal.StateStoreImplicits
import com.twitter.util.Duration
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueIterator

@deprecated("Use AggregatorTransformer", "1/7/2019")
class CompositeSumAggregator[K, A, CK <: CompositeKey[K, A]](
  commitInterval: Duration,
  compositeKeyRangeStart: CK,
  statsReceiver: StatsReceiver,
  stateStoreName: String,
  timerStoreName: String,
  windowSize: Duration,
  allowedLateness: Duration,
  queryableAfterClose: Duration,
  emitOnClose: Boolean = true,
  maxActionsPerTimer: Int = 25000)
    extends FinatraTransformer[
      CK,
      Int,
      TimeWindowed[K],
      WindowedValue[
        scala.collection.Map[A, Int]
      ]](statsReceiver = statsReceiver)
    with PersistentTimers
    with StateStoreImplicits
    with IteratorImplicits {

  private val restatementsCounter = statsReceiver.counter("numRestatements")
  private val deletesCounter = statsReceiver.counter("numDeletes")

  private val closedCounter = statsReceiver.counter("closedWindows")
  private val expiredCounter = statsReceiver.counter("expiredWindows")
  private val getLatencyStat = statsReceiver.stat("getLatency")
  private val putLatencyStat = statsReceiver.stat("putLatency")

  private val stateStore = getKeyValueStore[TimeWindowed[CK], Int](stateStoreName)
  private val timerStore = getPersistentTimerStore[WindowStartTime](
    timerStoreName,
    onEventTimer,
    PunctuationType.STREAM_TIME
  )

  override def onMessage(time: Time, compositeKey: CK, count: Int): Unit = {
    val windowedCompositeKey = TimeWindowed.forSize(time.hour, windowSize, compositeKey)
    if (windowedCompositeKey.isLate(allowedLateness, Watermark(watermark.timeMillis))) {
      restatementsCounter.incr()
      forward(windowedCompositeKey.map { _ =>
        compositeKey.primary
      }, WindowedValue(Restatement, Map(compositeKey.secondary -> count)))
    } else {
      val newCount = stateStore.increment(
        windowedCompositeKey,
        count,
        getStat = getLatencyStat,
        putStat = putLatencyStat
      )
      if (newCount == count) {
        val closeTime = windowedCompositeKey.start + windowSize + allowedLateness
        if (emitOnClose) {
          timerStore.addTimer(closeTime, Close, windowedCompositeKey.start.millis)
        }
        timerStore.addTimer(
          closeTime + queryableAfterClose,
          Expire,
          windowedCompositeKey.start.millis
        )
      }
    }
  }

  /*
   * TimeWindowedKey(2018-08-04T10:00:00.000Z-20-displayed) -> 50
   * TimeWindowedKey(2018-08-04T10:00:00.000Z-20-fav)       -> 10
   * TimeWindowedKey(2018-08-04T10:00:00.000Z-30-displayed) -> 30
   * TimeWindowedKey(2018-08-04T10:00:00.000Z-40-retweet)   -> 4
   */
  //Note: We use the cursor even for deletes to skip tombstones that may otherwise slow down the range scan
  private def onEventTimer(
    time: Time,
    timerMetadata: TimerMetadata,
    windowStartMs: WindowStartTime
  ): Unit = {
    debug(s"onEventTimer $time $timerMetadata")
    val windowStart = Time(windowStartMs)
    val windowIterator = stateStore.range(
      TimeWindowed.forSize(windowStart, windowSize, compositeKeyRangeStart),
      TimeWindowed.forSize(windowStart + 1.millis, windowSize, compositeKeyRangeStart)
    )

    try {
      if (timerMetadata == Close) {
        onClosed(windowStart, windowIterator)
      } else {
        onExpired(windowIterator)
      }
    } finally {
      windowIterator.close()
    }
  }

  private def onClosed(
    windowStart: Time,
    windowIterator: KeyValueIterator[TimeWindowed[CK], Int]
  ): Unit = {
    windowIterator
      .groupBy(
        primaryKey = timeWindowed => timeWindowed.value.primary,
        secondaryKey = timeWindowed => timeWindowed.value.secondary,
        mapValue = count => count,
        sharedMap = true
      )
      .take(maxActionsPerTimer)
      .foreach {
        case (key, countsMap) =>
          forward(
            key = TimeWindowed.forSize(windowStart, windowSize, key),
            value = WindowedValue(resultState = WindowClosed, value = countsMap)
          )
      }

    closedCounter.incr()
  }

  //Note: We call "put" w/ a null value instead of calling "delete" since "delete" also gets the previous value :-/
  //TODO: Consider performing deletes in a transaction so that queryable state sees all or no keys per "primary key"
  private def onExpired(windowIterator: KeyValueIterator[TimeWindowed[CK], Int]): Unit = {
    windowIterator
      .take(maxActionsPerTimer)
      .foreach {
        case (timeWindowedCompositeKey, count) =>
          deletesCounter.incr()
          stateStore.put(timeWindowedCompositeKey, null.asInstanceOf[Int])
      }

    expiredCounter.incr()
  }
}
