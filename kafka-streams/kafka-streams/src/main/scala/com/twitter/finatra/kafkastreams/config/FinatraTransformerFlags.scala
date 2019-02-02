package com.twitter.finatra.kafkastreams.config

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.FinatraTransformerFlags._

object FinatraTransformerFlags {
  val AutoWatermarkInterval = "finatra.streams.watermarks.auto.interval"
  val EmitWatermarkPerMessage = "finatra.streams.watermarks.per.message"
}

/**
 * A trait providing flags for configuring FinatraTransformers
 */
trait FinatraTransformerFlags extends KafkaStreamsTwitterServer {

  protected val autoWatermarkIntervalFlag = flag(
    AutoWatermarkInterval,
    100.milliseconds,
    "Minimum interval at which to call onWatermark when a new watermark is assigned. Set to 0.millis to disable auto watermark functionality which can be useful during topology tests."
  )

  protected val emitWatermarkPerMessageFlag = flag(
    EmitWatermarkPerMessage,
    false,
    "Call onWatermark after each message. When set to false, onWatermark is called every finatra.streams.auto.watermark.interval. Note: onWatermark is only called when the watermark changes."
  )
}
