package com.twitter.finatra.kafkastreams.test

import com.github.nscala_time.time.DurationBuilder
import com.twitter.util.Duration
import org.joda.time.{DateTime, DateTimeUtils}

/**
 * Helper class to modify the timestamps that will be used in FinatraStreams tests.
 */
trait TimeTraveler {
  def setTime(now: String): Unit = {
    setTime(new DateTime(now))
  }

  def setTime(now: DateTime): Unit = {
    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
  }

  def advanceTime(duration: Duration): DateTime = {
    advanceTimeMillis(duration.inMillis)
  }

  def advanceTime(duration: DurationBuilder): DateTime = {
    advanceTimeMillis(duration.toDuration.getMillis)
  }

  def currentHour: DateTime = {
    now.hourOfDay().roundFloorCopy()
  }

  def now: DateTime = {
    DateTime.now()
  }

  def currentHourMillis: Long = {
    currentHour.getMillis
  }

  def priorHour: DateTime = {
    currentHour.minusHours(1)
  }

  def priorHourMillis: Long = {
    priorHour.getMillis
  }

  private def advanceTimeMillis(durationMillis: Long) = {
    DateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + durationMillis)
    now
  }
}
