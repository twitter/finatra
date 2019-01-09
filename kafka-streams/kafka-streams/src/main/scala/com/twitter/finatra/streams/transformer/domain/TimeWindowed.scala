package com.twitter.finatra.streams.transformer.domain

import com.twitter.util.Duration
import org.joda.time.{DateTime, DateTimeConstants}

object TimeWindowed {

  def forSize[V](startMs: Long, sizeMs: Long, value: V): TimeWindowed[V] = {
    TimeWindowed(startMs, startMs + sizeMs, value)
  }

  def forSizeFromMessageTime[V](messageTime: Time, sizeMs: Long, value: V): TimeWindowed[V] = {
    val windowStartMs = windowStart(messageTime, sizeMs)
    TimeWindowed(windowStartMs, windowStartMs + sizeMs, value)
  }

  def hourly[V](startMs: Long, value: V): TimeWindowed[V] = {
    TimeWindowed(startMs, startMs + DateTimeConstants.MILLIS_PER_HOUR, value)
  }

  def windowStart(messageTime: Time, sizeMs: Long): Long = {
    (messageTime.millis / sizeMs) * sizeMs
  }
}

/**
 * A time windowed value specified by a start and end time
 * @param startMs the start timestamp of the window (inclusive)
 * @param endMs   the end timestamp of the window (exclusive)
 */
case class TimeWindowed[V](startMs: Long, endMs: Long, value: V) {

  /**
   * Determine if this windowed value is late given the allowedLateness configuration and the
   * current watermark
   *
   * @param allowedLateness the configured amount of allowed lateness specified in milliseconds
   * @param watermark a watermark used to determine if this windowed value is late
   * @return If the windowed value is late
   */
  def isLate(allowedLateness: Long, watermark: Watermark): Boolean = {
    watermark.timeMillis > endMs + allowedLateness
  }

  /**
   * Determine the start of the next fixed window interval
   */
  def nextInterval(time: Long, duration: Duration): Long = {
    val intervalStart = math.max(startMs, time)
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
  def sizeMillis: Long = endMs - startMs

  final override val hashCode: Int = {
    var result = value.hashCode()
    result = 31 * result + (startMs ^ (startMs >>> 32)).toInt
    result = 31 * result + (endMs ^ (endMs >>> 32)).toInt
    result
  }

  final override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: TimeWindowed[V] =>
        startMs == other.startMs &&
          endMs == other.endMs &&
          value == other.value
      case _ =>
        false
    }
  }

  override def toString: String = {
    s"TimeWindowed(${new DateTime(startMs)}-${new DateTime(endMs)}-$value)"
  }
}
