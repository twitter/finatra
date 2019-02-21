package com.twitter.finatra.kafkastreams.transformer.watermarks

import com.twitter.finatra.kafkastreams.transformer.domain.Time

trait WatermarkAssignor[K, V] {
  def onMessage(topic: String, timestamp: Time, key: K, value: V): Unit

  def getWatermark: Watermark
}
