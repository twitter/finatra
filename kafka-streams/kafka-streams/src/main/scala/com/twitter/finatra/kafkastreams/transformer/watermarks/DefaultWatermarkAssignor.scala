package com.twitter.finatra.kafkastreams.transformer.watermarks

import com.twitter.finatra.kafkastreams.transformer.domain.Time
import com.twitter.util.logging.Logging

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
