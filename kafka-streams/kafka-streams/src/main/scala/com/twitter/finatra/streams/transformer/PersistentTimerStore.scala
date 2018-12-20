package com.twitter.finatra.streams.transformer

import com.google.common.annotations.Beta
import com.twitter.finatra.streams.converters.time._
import com.twitter.finatra.streams.stores.FinatraKeyValueStore
import com.twitter.finatra.streams.transformer.FinatraTransformer.TimerTime
import com.twitter.finatra.streams.transformer.domain.{Time, TimerMetadata, Watermark}
import com.twitter.finatra.streams.transformer.internal.domain.{Timer, TimerSerde}
import com.twitter.inject.Logging
import org.apache.kafka.streams.state.KeyValueIterator

@Beta
class PersistentTimerStore[TimerKey](
  timersStore: FinatraKeyValueStore[Timer[TimerKey], Array[Byte]],
  onTimer: (Time, TimerMetadata, TimerKey) => Unit,
  maxTimerFiresPerWatermark: Int)
    extends OnWatermark
    with Logging {

  /* Private Mutable */

  @volatile private var nextTimerTime: Long = _
  @volatile private var currentWatermark: Watermark = _

  /* Public */

  def onInit(): Unit = {
    setNextTimerTime(Long.MaxValue)
    currentWatermark = Watermark(0)

    val iterator = timersStore.all()
    try {
      if (iterator.hasNext) {
        setNextTimerTime(iterator.next.key.time)
      }
    } finally {
      iterator.close()
    }
  }

  final override def onWatermark(watermark: Watermark): Unit = {
    debug(s"onWatermark $watermark nextTimerTime ${nextTimerTime.iso8601Millis}")
    if (watermark.timeMillis >= nextTimerTime) {
      debug(s"Calling fireTimers($watermark)")
      fireTimers(watermark)
    }
    currentWatermark = watermark
  }

  def addTimer(time: Time, metadata: TimerMetadata, key: TimerKey): Unit = {
    trace(f"${"AddTimer:"}%-20s ${metadata.getClass.getSimpleName}%-12s Key $key Timer $time")

    if (time.millis < currentWatermark.timeMillis) {
      info(s"Directly firing $metadata $key since $time < $currentWatermark")
      onTimer(time, metadata, key)
    } else {
      timersStore.put(
        key = Timer(time = time.millis, metadata = metadata, key = key),
        value = Array.emptyByteArray
      )

      if (time.millis < nextTimerTime) {
        setNextTimerTime(time.millis)
      }
    }
  }

  /* Private */

  private sealed trait TimerIteratorState {
    def done: Boolean
  }
  private object Iterating extends TimerIteratorState {
    override val done = false
  }
  private object FoundTimerAfterWatermark extends TimerIteratorState {
    override val done = true
  }
  private object ExceededMaxTimers extends TimerIteratorState {
    override val done = true
  }

  // Mostly optimized (although hasNext is still called more times than needed)
  private def fireTimers(watermark: Watermark): Unit = {
    val timerIterator = timersStoreIterator()

    try {
      var timerIteratorState: TimerIteratorState = Iterating
      var currentTimer: Timer[TimerKey] = null
      var numTimerFires = 0

      while (timerIterator.hasNext && !timerIteratorState.done) {
        currentTimer = timerIterator.next().key

        if (watermark.timeMillis >= currentTimer.time) {
          fireAndDeleteTimer(currentTimer)
          numTimerFires += 1
          if (numTimerFires >= maxTimerFiresPerWatermark) {
            timerIteratorState = ExceededMaxTimers
          }
        } else {
          timerIteratorState = FoundTimerAfterWatermark
        }
      }

      if (timerIteratorState == FoundTimerAfterWatermark) {
        setNextTimerTime(currentTimer.time)
      } else if (timerIteratorState == ExceededMaxTimers && timerIterator.hasNext) {
        setNextTimerTime(timerIterator.next().key.time)
        debug(
          s"Exceeded $maxTimerFiresPerWatermark max timer fires per watermark. LastTimerFired: ${currentTimer.time.iso8601Millis} NextTimer: ${nextTimerTime.iso8601Millis}"
        )
      } else {
        assert(!timerIterator.hasNext)
        setNextTimerTime(Long.MaxValue)
      }
    } finally {
      timerIterator.close()
    }
  }

  /*
   * Instead of calling timersStore.all, we perform a range scan starting at our nextTimerTime. This optimization
   * avoids a performance issue where timersStore.all may need to traverse lots of tombstoned timers that were
   * deleted but not yet compacted.
   *
   * For more information see:
   * https://github.com/facebook/rocksdb/issues/261
   * https://www.reddit.com/r/IAmA/comments/3de3cv/we_are_rocksdb_engineering_team_ask_us_anything/ct4c0fk/
   */
  private def timersStoreIterator(): KeyValueIterator[Timer[TimerKey], Array[Byte]] = {
    timersStore.range(TimerSerde.timerTimeToBytes(nextTimerTime))
  }

  private def fireAndDeleteTimer(timer: Timer[TimerKey]): Unit = {
    debug(
      s"fireAndDeleteTimer ${timer.metadata.getClass.getName} key: ${timer.key} timerTime: ${timer.time.iso8601Millis}"
    )
    onTimer(Time(timer.time), timer.metadata, timer.key)
    timersStore.deleteWithoutGettingPriorValue(timer)
  }

  private def setNextTimerTime(time: TimerTime): Unit = {
    nextTimerTime = time
    if (time != Long.MaxValue) {
      trace(s"NextTimer: ${nextTimerTime.iso8601Millis}")
    } else {
      trace(s"NextTimer: Long.MaxValue")
    }
  }
}
