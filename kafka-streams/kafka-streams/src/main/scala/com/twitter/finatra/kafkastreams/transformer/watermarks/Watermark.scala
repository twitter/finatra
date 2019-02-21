package com.twitter.finatra.kafkastreams.transformer.watermarks

import com.twitter.finatra.kafkastreams.utils.time._

object Watermark {
  val unknown: Watermark = Watermark(0L)
}

case class Watermark(timeMillis: Long) extends AnyVal {

  override def toString: String = {
    s"Watermark(${timeMillis.iso8601Millis})"
  }
}
