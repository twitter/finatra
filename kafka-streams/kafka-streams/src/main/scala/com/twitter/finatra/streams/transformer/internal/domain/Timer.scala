package com.twitter.finatra.streams.transformer.internal.domain

import com.twitter.finatra.streams.transformer.domain.TimerMetadata
import org.joda.time.DateTime

/**
 * @param time Time to fire the timer
 */
case class Timer[K](time: Long, metadata: TimerMetadata, key: K) {

  override def toString: String = {
    s"Timer(${new DateTime(time)}-$metadata-$key)"
  }
}
