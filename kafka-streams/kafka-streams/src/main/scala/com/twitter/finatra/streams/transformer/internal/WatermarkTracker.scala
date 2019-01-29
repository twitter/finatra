package com.twitter.finatra.streams.transformer.internal

import com.twitter.finatra.streams.transformer.domain.Time

//TODO: Need method called by processing timer so that watermarks can be emitted without input records
class WatermarkTracker {
  private var _watermark: Long = 0L
  reset()

  def watermark: Time = Time(_watermark)

  def reset(): Unit = {
    _watermark = 0L
  }

  /**
   * @param timestamp
   *
   * @return True if watermark changed
   */
  //TODO: Verify topic is correct when merging inputs
  //TODO: Also take in deserialized key and value since we can extract source info (e.g. source of interactions)
  //TODO: Also take in maxOutOfOrder param
  //TODO: Use rolling histogram
  def track(topic: String, timestamp: Long): Boolean = {
    val potentialWatermark = timestamp - 1
    if (potentialWatermark > _watermark) {
      _watermark = potentialWatermark
      true
    } else {
      false
    }
  }
}
