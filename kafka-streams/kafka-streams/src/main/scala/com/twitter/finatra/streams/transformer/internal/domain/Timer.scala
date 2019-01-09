package com.twitter.finatra.streams.transformer.internal.domain

import com.twitter.finatra.streams.converters.time._
import com.twitter.finatra.streams.transformer.domain.TimerMetadata

/**
 * @param time Time to fire the timer
 */
case class Timer[K](time: Long, metadata: TimerMetadata, key: K) {

  override def toString: String = {
    s"Timer(${metadata.getClass.getName} $key @${time.iso8601Millis})"
  }
}
