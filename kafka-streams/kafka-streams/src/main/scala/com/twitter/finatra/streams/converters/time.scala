package com.twitter.finatra.streams.converters

import org.joda.time.format.ISODateTimeFormat

object time {
  implicit class RichLong(long: Long) {
    def iso8601Millis: String = {
      ISODateTimeFormat.dateTime.print(long)
    }

    def iso8601: String = {
      ISODateTimeFormat.dateTimeNoMillis.print(long)
    }
  }
}
