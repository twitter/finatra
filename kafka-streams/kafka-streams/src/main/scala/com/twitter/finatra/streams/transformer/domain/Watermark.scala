package com.twitter.finatra.streams.transformer.domain

import com.twitter.finatra.streams.converters.time._

object Watermark {
  val unknown: Watermark = Watermark(0L)
}

case class Watermark(timeMillis: Long) extends AnyVal {

  override def toString: String = {
    s"Watermark(${timeMillis.iso8601Millis})"
  }
}
