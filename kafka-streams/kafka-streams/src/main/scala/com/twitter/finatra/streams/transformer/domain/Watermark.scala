package com.twitter.finatra.streams.transformer.domain

import com.twitter.finatra.streams.converters.time._

case class Watermark(timeMillis: Long) extends AnyVal {

  override def toString: String = {
    s"Watermark(${timeMillis.iso8601Millis})"
  }
}
