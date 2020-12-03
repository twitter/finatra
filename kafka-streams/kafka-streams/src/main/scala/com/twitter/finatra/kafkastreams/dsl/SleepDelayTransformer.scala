package com.twitter.finatra.kafkastreams.dsl

import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import com.twitter.util.Duration
import org.joda.time.DateTime

private[dsl] class SleepDelayTransformer[K, V](delay: Duration)
    extends FinatraTransformer[K, V, K, V](NullStatsReceiver) {
  override def onMessage(
    messageTime: Time,
    key: K,
    value: V
  ): Unit = {
    val wallClock: Time = Time.create(DateTime.now())
    val gap = wallClock.millis - messageTime.millis
    if (gap < delay.inMillis) {
      Thread.sleep(delay.inMillis - gap)
    }
    forward(key, value)
  }
}
