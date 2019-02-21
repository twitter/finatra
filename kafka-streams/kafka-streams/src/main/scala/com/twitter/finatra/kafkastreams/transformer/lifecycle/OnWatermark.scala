package com.twitter.finatra.kafkastreams.transformer.lifecycle

import com.twitter.finatra.kafkastreams.transformer.watermarks.Watermark

trait OnWatermark {
  def onWatermark(watermark: Watermark): Unit
}
