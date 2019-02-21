package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finatra.kafkastreams.transformer.domain.{Time, TimerMetadata}
import com.twitter.finatra.kafkastreams.utils.time._

/**
 * @param time Time to fire the timer
 */
case class Timer[K](time: Time, metadata: TimerMetadata, key: K) {

  override def toString: String = {
    s"Timer(${metadata.getClass.getName} $key @${time.millis.iso8601Millis})"
  }
}
