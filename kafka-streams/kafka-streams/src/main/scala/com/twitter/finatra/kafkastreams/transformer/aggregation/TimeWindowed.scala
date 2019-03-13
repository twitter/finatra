package com.twitter.finatra.kafkastreams.transformer.aggregation

import com.twitter.finatra.kafkastreams.transformer.domain.Time
import com.twitter.finatra.kafkastreams.transformer.watermarks.Watermark
import com.twitter.util.Duration
import org.joda.time.{DateTime, DateTimeConstants}

object TimeWindowed {

  /**
   * Create a time windowed value
   *
   * @param start Start of the window
   * @param size Size of the window
   * @param value Windowed value
   * @tparam V Type of the windowed value
   * @return A time windowed value
   */
  def forSize[V](start: Time, size: Duration, value: V): TimeWindowed[V] = {
    TimeWindowed(start, start + size, value)
  }

  /**
   * Create an hourly time windowed value
   *
   * @param start Start of the hourly window
   * @param value Windowed value
   * @tparam V Type of the windowed value
   * @return An hourly time windowed value
   */
  def hourly[V](start: Time, value: V): TimeWindowed[V] = {
    TimeWindowed(start, Time(start.millis + DateTimeConstants.MILLIS_PER_HOUR), value)
  }

  /**
   * Calculate the start time of the window that the specified message belongs to
   *
   * @param messageTime Message time
   * @param size Fixed window size
   * @return Start time of a window
   */
  def windowStart(messageTime: Time, size: Duration): Time = {
    Time((messageTime.millis / size.inMillis) * size.inMillis)
  }

  /**
   * Calculate the end time of the window that the specified message belongs to
   *
   * @param messageTime Message time
   * @param size Fixed window size
   * @return End time of a window
   */
  def windowEnd(messageTime: Time, size: Duration): Time = {
    windowStart(messageTime, size) + size
  }
}

/**
 * A time windowed value specified by a start and end time
 * @param start the start time of the window (inclusive)
 * @param end   the end time of the window (exclusive)
 */
case class TimeWindowed[V](start: Time, end: Time, value: V) {

  /**
   * Determine if this windowed value is late given the allowedLateness configuration and the
   * current watermark
   *
   * @param allowedLateness the configured amount of allowed lateness specified in milliseconds
   * @param watermark a watermark used to determine if this windowed value is late
   * @return If the windowed value is late
   */
  def isLate(allowedLateness: Duration, watermark: Watermark): Boolean = {
    watermark.timeMillis > end.millis + allowedLateness.inMillis
  }

  /**
   * Determine the start of the next fixed window interval
   */
  def nextInterval(time: Time, duration: Duration): Time = {
    val intervalStart = Time(math.max(start.millis, time.millis))
    Time.nextInterval(intervalStart, duration)
  }

  /**
   * Map the time windowed value into another value occurring in the same window
   */
  def map[KK](f: V => KK): TimeWindowed[KK] = {
    copy(value = f(value))
  }

  /**
   * The size of this windowed value in milliseconds
   */
  def sizeMillis: Long = end.millis - start.millis

  final override val hashCode: Int = {
    var result = value.hashCode()
    result = 31 * result + (start.millis ^ (start.millis >>> 32)).toInt
    result = 31 * result + (end.millis ^ (end.millis >>> 32)).toInt
    result
  }

  final override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: TimeWindowed[V] =>
        start == other.start &&
          end == other.end &&
          value == other.value
      case _ =>
        false
    }
  }

  override def toString: String = {
    s"TimeWindowed(${new DateTime(start.millis)}-${new DateTime(end.millis)}-$value)"
  }
}
