package com.twitter.finatra.kafkastreams.transformer.watermarks

trait PeriodicWatermarkManager[K, V] {

  def init(onWatermark: Long => Unit): Unit

  def close(): Unit

  def currentWatermark: Long

  def onMessage(topic: String, timestamp: Long, key: K, value: V): Unit

  /**
   * Called every watermarkPeriodicWallClockDuration allowing the Watermark manager decide whether to call onWatermark to emit a new watermark
   */
  def onPeriodicWallClockDuration(): Unit
}
