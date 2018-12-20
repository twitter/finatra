package com.twitter.finatra.streams.transformer.watermarks.internal

import com.twitter.finatra.streams.transformer.OnWatermark
import com.twitter.finatra.streams.transformer.domain.{Time, Watermark}
import com.twitter.finatra.streams.transformer.watermarks.WatermarkAssignor
import com.twitter.inject.Logging

class WatermarkManager[K, V](
  onWatermark: OnWatermark,
  watermarkAssignor: WatermarkAssignor[K, V],
  emitWatermarkPerMessage: Boolean)
    extends Logging {

  @volatile private var lastEmittedWatermark = Watermark(0L)

  /* Public */

  def close(): Unit = {
    setLastEmittedWatermark(Watermark(0L))
  }

  def watermark: Watermark = {
    lastEmittedWatermark
  }

  def onMessage(messageTime: Time, topic: String, key: K, value: V): Unit = {
    watermarkAssignor.onMessage(topic = topic, timestamp = messageTime, key = key, value = value)

    if (emitWatermarkPerMessage) {
      callOnWatermarkIfChanged()
    }
  }

  def callOnWatermarkIfChanged(): Unit = {
    val currentWatermark = watermarkAssignor.getWatermark
    if (currentWatermark.timeMillis > lastEmittedWatermark.timeMillis) {
      onWatermark.onWatermark(currentWatermark)
      setLastEmittedWatermark(currentWatermark)
    }
  }

  protected[streams] def setLastEmittedWatermark(newWatermark: Watermark): Unit = {
    lastEmittedWatermark = newWatermark
  }
}
