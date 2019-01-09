package com.twitter.finatra.streams.transformer.watermarks

import com.twitter.finatra.streams.transformer.domain.{Time, Watermark}

trait WatermarkAssignor[K, V] {
  def onMessage(topic: String, timestamp: Time, key: K, value: V): Unit

  def getWatermark: Watermark
}
