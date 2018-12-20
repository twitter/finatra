package com.twitter.finatra.streams.transformer

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.streams.transformer.FinatraTransformer.WindowStartTime
import com.twitter.finatra.streams.transformer.domain._
import com.twitter.util.Duration
import org.apache.kafka.streams.state.KeyValueIterator

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
      TimeWindowed[CK],
      WindowStartTime,
      TimeWindowed[K],
      WindowedValue[
        scala.collection.Map[A, Int]
      ]](timerStoreName = timerStoreName, statsReceiver = statsReceiver, cacheTimers = true) {

  private val windowSizeMillis = windowSize.inMillis
  private val allowedLatenessMillis = allowedLateness.inMillis
  private val queryableAfterCloseMillis = queryableAfterClose.inMillis

  private val restatementsCounter = statsReceiver.counter("numRestatements")
  private val deletesCounter = statsReceiver.counter("numDeletes")

  private val closedCounter = statsReceiver.counter("closedWindows")
  private val expiredCounter = statsReceiver.counter("expiredWindows")
  private val getLatencyStat = statsReceiver.stat("getLatency")
  private val putLatencyStat = statsReceiver.stat("putLatency")

  private val stateStore = getKeyValueStore[TimeWindowed[CK], Int](stateStoreName)

  override def onMessage(time: Time, compositeKey: CK, count: Int): Unit = {
    val windowedCompositeKey = TimeWindowed.forSize(time.hourMillis, windowSizeMillis, compositeKey)
    if (windowedCompositeKey.isLate(allowedLatenessMillis, watermark)) {
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
        val closeTime = windowedCompositeKey.startMs + windowSizeMillis + allowedLatenessMillis
        if (emitOnClose) {
          addEventTimeTimer(Time(closeTime), Close, windowedCompositeKey.startMs)
        }
        addEventTimeTimer(
          Time(closeTime + queryableAfterCloseMillis),
          Expire,
          windowedCompositeKey.startMs
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
  override def onEventTimer(
    time: Time,
    timerMetadata: TimerMetadata,
    windowStartMs: WindowStartTime,
    cursor: Option[TimeWindowed[CK]]
  ): TimerResult[TimeWindowed[CK]] = {
    debug(s"onEventTimer $time $timerMetadata")
    val windowIterator = stateStore.range(
      cursor getOrElse TimeWindowed
        .forSize(windowStartMs, windowSizeMillis, compositeKeyRangeStart),
      TimeWindowed.forSize(windowStartMs + 1, windowSizeMillis, compositeKeyRangeStart)
    )

    try {
      if (timerMetadata == Close) {
        onClosed(windowStartMs, windowIterator)
      } else {
        onExpired(windowIterator)
      }
    } finally {
      windowIterator.close()
    }
  }

  private def onClosed(
    windowStartMs: Long,
    windowIterator: KeyValueIterator[TimeWindowed[CK], Int]
  ): TimerResult[TimeWindowed[CK]] = {
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
            key = TimeWindowed.forSize(windowStartMs, windowSizeMillis, key),
            value = WindowedValue(resultState = WindowClosed, value = countsMap)
          )
      }

    deleteOrRetainTimer(windowIterator, onDeleteTimer = closedCounter.incr())
  }

  //Note: We call "put" w/ a null value instead of calling "delete" since "delete" also gets the previous value :-/
  //TODO: Consider performing deletes in a transaction so that queryable state sees all or no keys per "primary key"
  private def onExpired(
    windowIterator: KeyValueIterator[TimeWindowed[CK], Int]
  ): TimerResult[TimeWindowed[CK]] = {
    windowIterator
      .take(maxActionsPerTimer)
      .foreach {
        case (timeWindowedCompositeKey, count) =>
          deletesCounter.incr()
          stateStore.put(timeWindowedCompositeKey, null.asInstanceOf[Int])
      }

    deleteOrRetainTimer(windowIterator, onDeleteTimer = expiredCounter.incr())
  }
}
