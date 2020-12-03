package com.twitter.finatra.kafkastreams.dsl

import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.domain.{Expire, Time, TimerMetadata}
import com.twitter.finatra.kafkastreams.transformer.stores.{
  PersistentTimerValueStore,
  PersistentTimers
}
import com.twitter.util.Duration
import org.apache.kafka.streams.processor.PunctuationType
import scala.reflect.ClassTag

private[dsl] class TimerStoreDelayTransformer[K: ClassTag, V](
  delay: Duration,
  timerStoreName: String)
    extends FinatraTransformer[K, V, K, V](
      NullStatsReceiver // This StatsReceiver is unused in FinatraTransformer
    )
    with PersistentTimers {
  private val timerStore: PersistentTimerValueStore[K, V] =
    getPersistentTimerValueStore[K, V](
      timerStoreName = timerStoreName,
      onTimer = forwardMessage,
      punctuationType = PunctuationType.STREAM_TIME)

  override def onMessage(messageTime: Time, key: K, value: V): Unit = {
    addTimer(messageTime, key, value)
  }

  private def forwardMessage(
    time: Time,
    metadata: TimerMetadata,
    timerKey: K,
    timerValue: V
  ): Unit = {
    forward(timerKey, timerValue)
  }

  private def addTimer(messageTime: Time, key: K, value: V): Unit = {
    timerStore.addTimer(messageTime + delay, Expire, key, value)
  }
}
