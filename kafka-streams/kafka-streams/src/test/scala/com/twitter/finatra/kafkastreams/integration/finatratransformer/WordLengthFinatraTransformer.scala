package com.twitter.finatra.kafkastreams.integration.finatratransformer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafkastreams.integration.finatratransformer.WordLengthFinatraTransformer._
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.domain.{Expire, Time, TimerMetadata}
import com.twitter.finatra.kafkastreams.transformer.stores.PersistentTimers
import com.twitter.util.Duration
import org.apache.kafka.streams.processor.PunctuationType

object WordLengthFinatraTransformer {
  val delayedMessageTime: Duration = 5.seconds
}

class WordLengthFinatraTransformer(statsReceiver: StatsReceiver, timerStoreName: String)
    extends FinatraTransformer[String, String, String, String](statsReceiver)
    with PersistentTimers {

  private val timerStore =
    getPersistentTimerStore[String](timerStoreName, onEventTimer, PunctuationType.STREAM_TIME)

  override def onMessage(messageTime: Time, key: String, value: String): Unit = {
    forward(key, "onMessage " + key + " " + key.length)

    val time = messageTime + delayedMessageTime

    timerStore.addTimer(time, Expire, key)
  }

  private def onEventTimer(time: Time, metadata: TimerMetadata, key: String): Unit = {
    forward(key, "onEventTimer " + key + " " + key.length)
  }
}
