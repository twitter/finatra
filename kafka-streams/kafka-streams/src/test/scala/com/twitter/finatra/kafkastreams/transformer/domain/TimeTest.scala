package com.twitter.finatra.kafkastreams.transformer.domain

import com.twitter.inject.Test
import com.twitter.util.Duration
import java.util.concurrent.TimeUnit
import org.joda.time.{DateTime, DateTimeConstants}

class TimeTest extends Test {

  private val timestamp = DateTimeConstants.MILLIS_PER_WEEK
  private val durationMs = DateTimeConstants.MILLIS_PER_MINUTE

  test("create Time from DateTime") {
    val datetime = new DateTime(timestamp)
    val time = Time.create(datetime)
    assert(time.millis == datetime.getMillis)
  }

  test("test nextInterval") {
    val baseTime = Time(0L)
    val duration = Duration(durationMs, TimeUnit.MILLISECONDS)
    val offsetTime = baseTime + duration
    assert(Time.nextInterval(baseTime, duration).millis == durationMs)
    assert(Time.nextInterval(offsetTime, duration).millis == 2 * durationMs)
  }

  test("test Time equality") {
    val timeA = Time(timestamp)
    val timeB = Time(timestamp)
    assert(timeA == timeB)
  }

  test("add Time with Time") {
    val time = Time(timestamp) + Time(timestamp)
    assert(time == Time(2 * timestamp))
  }

  test("add Time with Duration") {
    val adjustedTime = Time(timestamp) + Duration(durationMs, TimeUnit.MILLISECONDS)
    assert(adjustedTime == Time(timestamp + durationMs))
  }

  test("test nearest hour") {
    val oneHourTime = Time(DateTimeConstants.MILLIS_PER_HOUR)
    val twoHourTime = Time(2 * DateTimeConstants.MILLIS_PER_HOUR)
    val hourTimePositiveOffset = oneHourTime + Duration(1, TimeUnit.MILLISECONDS)
    val twoHourTimeNegativeOffset = twoHourTime + Duration(-1, TimeUnit.MILLISECONDS)
    assert(oneHourTime.hour == oneHourTime)
    assert(twoHourTime.hour == twoHourTime)
    assert(hourTimePositiveOffset.hour == oneHourTime)
    assert(twoHourTimeNegativeOffset.hour == oneHourTime)
  }

  test("test roundDown") {
    val baseTimestamp = 100L
    val roundingMillis = 20L
    val offsetTimestamp = baseTimestamp + (roundingMillis / 2)
    assert(Time(offsetTimestamp).roundDown(roundingMillis) == Time(baseTimestamp))
  }
}
