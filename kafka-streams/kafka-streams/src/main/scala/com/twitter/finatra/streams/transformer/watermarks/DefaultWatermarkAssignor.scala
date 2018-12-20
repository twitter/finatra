package com.twitter.finatra.streams.transformer.watermarks

import com.twitter.finatra.streams.transformer.domain.{Time, Watermark}
import com.twitter.inject.Logging

class DefaultWatermarkAssignor[K, V] extends WatermarkAssignor[K, V] with Logging {

  @volatile private var watermark = Watermark(0L)

  override def onMessage(topic: String, timestamp: Time, key: K, value: V): Unit = {
    trace(s"onMessage $topic $timestamp $key -> $value")
    val potentialWatermark = timestamp.millis - 1
    if (potentialWatermark > watermark.timeMillis) {
      watermark = Watermark(potentialWatermark)
    }
  }

  override def getWatermark: Watermark = {
    watermark
  }
}
