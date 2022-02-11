package com.twitter.finatra.kafkastreams.transformer.stores

import com.google.common.annotations.Beta
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer.TimerTime
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import com.twitter.finatra.kafkastreams.transformer.domain.TimerMetadata
import com.twitter.finatra.kafkastreams.transformer.lifecycle.OnWatermark
import com.twitter.finatra.kafkastreams.transformer.stores.internal.Timer
import com.twitter.finatra.kafkastreams.transformer.watermarks.Watermark
import com.twitter.finatra.kafkastreams.utils.time._
import com.twitter.finatra.streams.transformer.internal.domain.TimerSerde
import com.twitter.util.logging.Logging
import org.apache.kafka.streams.state.KeyValueIterator

@Beta
class PersistentTimerValueStore[TimerKey, TimerValue](
  timersStore: FinatraKeyValueStore[Timer[TimerKey], TimerValue],
  onTimer: (Time, TimerMetadata, TimerKey, TimerValue) => Unit,
  maxTimerFiresPerWatermark: Int)
    extends OnWatermark
    with Logging {

  /* Private Mutable */

  @volatile private var nextTimerTime: Long = _
  @volatile private var currentWatermark: Watermark = _

  /* Public */

  def onInit(): Unit = {
    trace(s"onInit ${timersStore.name()}")
    assert(timersStore.isOpen)

    setNextTimerTime(Long.MaxValue)
    currentWatermark = Watermark(0L)

    val iterator = timersStore.all()
    try {
      if (iterator.hasNext) {
        setNextTimerTime(iterator.next.key.time.millis)
      }
    } finally {
      iterator.close()
    }
  }

  final override def onWatermark(watermark: Watermark): Unit = {
    if (watermark.timeMillis < 10000) {
      warn(s"onWatermark too small $watermark")
    } else {
      trace(s"onWatermark $watermark nextTimerTime ${nextTimerTime.iso8601Millis}")
    }

    if (watermark.timeMillis >= nextTimerTime) {
      trace(s"Calling fireTimers($watermark)")
      fireTimers(watermark)
    }
    currentWatermark = watermark
  }

  def addTimer(time: Time, metadata: TimerMetadata, key: TimerKey, value: TimerValue): Unit = {
    if (time.millis < currentWatermark.timeMillis) {
      info(
        f"${"DirectlyFireTimer:"}%-20s ${metadata.getClass.getSimpleName}%-12s Key $key Timer $time since $time < $currentWatermark")

      onTimer(time, metadata, key, value)
    } else {
      debug(f"${"AddTimer:"}%-20s ${metadata.getClass.getSimpleName}%-12s Key $key Timer $time")
      timersStore.put(Timer(time = time, metadata = metadata, key = key), value)

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
        val next = timerIterator.next()
        currentTimer = next.key
        val currentValue = next.value

        if (watermark.timeMillis >= currentTimer.time.millis) {
          fireAndDeleteTimer(currentTimer, currentValue)
          numTimerFires += 1
          if (numTimerFires >= maxTimerFiresPerWatermark) {
            timerIteratorState = ExceededMaxTimers
          }
        } else {
          timerIteratorState = FoundTimerAfterWatermark
        }
      }

      if (timerIteratorState == FoundTimerAfterWatermark) {
        setNextTimerTime(currentTimer.time.millis)
      } else if (timerIteratorState == ExceededMaxTimers && timerIterator.hasNext) {
        setNextTimerTime(timerIterator.next().key.time.millis)
        debug(
          s"Exceeded $maxTimerFiresPerWatermark max timer fires per watermark. LastTimerFired: ${currentTimer.time.millis.iso8601Millis} NextTimer: ${nextTimerTime.iso8601Millis}"
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
  private def timersStoreIterator(): KeyValueIterator[Timer[TimerKey], TimerValue] = {
    timersStore.range(TimerSerde.timerTimeToBytes(nextTimerTime))
  }

  private def fireAndDeleteTimer(timer: Timer[TimerKey], value: TimerValue): Unit = {
    trace(s"fireAndDeleteTimer $timer")
    onTimer(timer.time, timer.metadata, timer.key, value)
    if (timersStore.get(timer) == null) {
      warn(s"TimerStore can't delete key: $timer")
    }
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
