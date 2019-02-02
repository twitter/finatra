package com.twitter.finatra.kafkastreams.utils

import com.twitter.finatra.kafkastreams.transformer.domain.Time
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/**
 * Time conversion utilities.
 */
object time {
  implicit class RichFinatraKafkaStreamsLong(val long: Long) extends AnyVal {
    def iso8601Millis: String = {
      ISODateTimeFormat.dateTime.print(long)
    }

    def iso8601: String = {
      ISODateTimeFormat.dateTimeNoMillis.print(long)
    }
  }

  implicit class RichFinatraKafkaStreamsDatetime(val datetime: DateTime) extends AnyVal {
    def toTime: Time = {
      Time.create(datetime)
    }
  }
}
