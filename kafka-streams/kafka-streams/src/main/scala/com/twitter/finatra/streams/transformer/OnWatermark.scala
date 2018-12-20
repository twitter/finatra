package com.twitter.finatra.streams.transformer

import com.twitter.finatra.streams.transformer.domain.Watermark

trait OnWatermark {
  def onWatermark(watermark: Watermark): Unit
}
