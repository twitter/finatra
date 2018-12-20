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
 * @param startMs the start timestamp of the window (inclusive)
 * @param endMs   the end timestamp of the window (exclusive)
 */
case class TimeWindowed[V](startMs: Long, endMs: Long, value: V) {

  //TODO: Use Time types
  def isLate(allowedLateness: Long, time: Long): Boolean = {
    time > endMs + allowedLateness
  }

  def nextInterval(time: Long, duration: Duration): Long = {
    val intervalStart = math.max(startMs, time)
    Time.nextInterval(intervalStart, duration)
  }

  def map[KK](f: V => KK): TimeWindowed[KK] = {
    copy(value = f(value))
  }

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
