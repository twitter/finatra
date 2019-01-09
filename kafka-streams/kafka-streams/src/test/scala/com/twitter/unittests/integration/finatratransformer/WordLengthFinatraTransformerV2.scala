package com.twitter.unittests.integration.finatratransformer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.streams.transformer.domain.{Expire, Time, TimerMetadata}
import com.twitter.finatra.streams.transformer.{FinatraTransformerV2, PersistentTimers}
import com.twitter.unittests.integration.finatratransformer.WordLengthFinatraTransformerV2._
import com.twitter.util.Duration
import org.apache.kafka.streams.processor.PunctuationType

object WordLengthFinatraTransformerV2 {
  val delayedMessageTime: Duration = 5.seconds
}

class WordLengthFinatraTransformerV2(statsReceiver: StatsReceiver, timerStoreName: String)
    extends FinatraTransformerV2[String, String, String, String](statsReceiver)
    with PersistentTimers {

  private val timerStore =
    getPersistentTimerStore[String](timerStoreName, onEventTimer, PunctuationType.STREAM_TIME)

  override def onMessage(messageTime: Time, key: String, value: String): Unit = {
    forward(key, "onMessage " + key + " " + key.length)

    val time = messageTime.plus(delayedMessageTime)

    timerStore.addTimer(time, Expire, key)
  }

  private def onEventTimer(time: Time, metadata: TimerMetadata, key: String): Unit = {
    forward(key, "onEventTimer " + key + " " + key.length)
  }
}
