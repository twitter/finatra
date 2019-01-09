package com.twitter.finatra.streams.transformer.domain

import com.twitter.util.Duration
import org.joda.time.DateTimeConstants
import com.twitter.finatra.streams.converters.time._

object Time {
  def nextInterval(time: Long, duration: Duration): Long = {
    val durationMillis = duration.inMillis
    val currentNumIntervals = time / durationMillis
    (currentNumIntervals + 1) * durationMillis
  }
}

//TODO: Refactor
case class Time(millis: Long) extends AnyVal {

  final def plus(duration: Duration): Time = {
    new Time(millis + duration.inMillis)
  }

  final def hourMillis: Long = {
    val unitsSinceEpoch = millis / DateTimeConstants.MILLIS_PER_HOUR
    unitsSinceEpoch * DateTimeConstants.MILLIS_PER_HOUR
  }

  final def hourlyWindowed[K](key: K): TimeWindowed[K] = {
    val start = hourMillis
    val end = start + DateTimeConstants.MILLIS_PER_HOUR
    TimeWindowed(start, end, key)
  }

  override def toString: String = {
    s"Time(${millis.iso8601Millis})"
  }
}
