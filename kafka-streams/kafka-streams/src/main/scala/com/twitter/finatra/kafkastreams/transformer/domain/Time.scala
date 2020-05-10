package com.twitter.finatra.kafkastreams.transformer.domain

import com.twitter.finatra.kafkastreams.transformer.aggregation.TimeWindowed
import com.twitter.finatra.kafkastreams.utils.time._
import com.twitter.util.Duration
import org.joda.time.{DateTime, DateTimeConstants}

object Time {

  /**
   * Construct a [[Time]] from a [[DateTime]].
   */
  def create(datetime: DateTime): Time = {
    new Time(datetime.getMillis)
  }

  /**
   * Finds the next interval occurrence of a [[Duration]] from a [[Time]].
   *
   * @param time A point in time.
   * @param duration The duration of an interval.
   */
  def nextInterval(time: Time, duration: Duration): Time = {
    val durationMillis = duration.inMillis
    val currentNumIntervals = time.millis / durationMillis
    Time((currentNumIntervals + 1) * durationMillis)
  }
}

/**
 * A Value Class representing a point in time.
 *
 * @param millis A millisecond timestamp.
 */
case class Time(millis: Long) extends AnyVal {

  /**
   * Adds a [[Time]] to the [[Time]].
   */
  final def +(time: Time): Time = {
    new Time(millis + time.millis)
  }

  /**
   * Adds a [[Duration]] to the [[Time]].
   */
  final def +(duration: Duration): Time = {
    new Time(millis + duration.inMillis)
  }

  /**
   * Rounds down [[Time]] to the nearest hour.
   */
  final def hour: Time = {
    roundDown(DateTimeConstants.MILLIS_PER_HOUR)
  }

  /**
   * Rounds down ''millis'' to the nearest multiple of ''milliseconds''.
   */
  final def roundDown(milliseconds: Long): Time = {
    val unitsSinceEpoch = millis / milliseconds
    Time(unitsSinceEpoch * milliseconds)
  }

  /**
   * @return An hourly window from derived from the [[Time]].
   */
  final def hourlyWindowed[K](key: K): TimeWindowed[K] = {
    val start = hour
    val end = Time(start.millis + DateTimeConstants.MILLIS_PER_HOUR)
    TimeWindowed(start, end, key)
  }

  override def toString: String = {
    s"Time(${millis.iso8601Millis})"
  }
}
