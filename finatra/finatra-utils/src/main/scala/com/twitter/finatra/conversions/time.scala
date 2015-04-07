package com.twitter.finatra.conversions

import com.github.nscala_time.time.{DurationBuilder, Implicits}
import com.twitter.util.{Duration => TwitterDuration, Time}
import org.joda.time.{DateTime, DateTimeZone, Duration}

/**
 * Add additional conversions to 'scala-time' and also
 * overcome issues with scala time joda wrappers not being serializable by jackson
 */
object time extends Implicits {

  /* ------------------------------------------------ */
  implicit class FinatraRichDateTime(dateTime: org.joda.time.DateTime) {

    private val LongTimeFromNowMillis = new DateTime(9999, 1, 1, 0, 0, 0, 0).getMillis

    def utcIso8601: String =
      utcIso8601(dateTime)

    def reverseUtcIso8601 = {
      utcIso8601(
        new DateTime(LongTimeFromNowMillis - dateTime.getMillis))
    }

    private def utcIso8601(dateTime: DateTime): String = {
      dateTime.withZone(DateTimeZone.UTC).toString
    }

    def toTwitterTime: Time = {
      Time.fromMilliseconds(dateTime.getMillis)
    }

    def epochSeconds: Int = {
      (dateTime.getMillis / 1000).toInt
    }
  }

  /* ------------------------------------------------ */
  implicit class FinatraRichDuration(duration: Duration) {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        duration.getMillis)
    }
  }

  /* ------------------------------------------------ */
  implicit class FinatraRichDurationBuilder(duration: DurationBuilder) {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        duration.toDuration.getMillis)
    }
  }

  /* ------------------------------------------------ */
  implicit class RichStringTime(string: String) {
    def toDateTime: DateTime = {
      DateTime.parse(string)
    }
  }
}
