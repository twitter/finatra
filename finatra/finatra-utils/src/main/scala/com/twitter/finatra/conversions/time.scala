package com.twitter.finatra.conversions

import com.twitter.util.{Time, Duration => TwitterDuration}
import org.joda.time.{DateTime, DateTimeZone, Duration}
import org.scala_tools.time.{Implicits, RichDurationBuilder}

object time extends Implicits with RichDurationBuilder {

  /* ------------------------------------------------ */
  class RichDateTime(dateTime: org.joda.time.DateTime) {

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
  }

  /* ------------------------------------------------ */
  class RichDuration(duration: Duration) {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        duration.getMillis)
    }
  }

  implicit def dateTimeToRichDateTime(dateTime: org.joda.time.DateTime): RichDateTime = new RichDateTime(dateTime)
  implicit def durationToRichDuration(duration: Duration): RichDuration = new RichDuration(duration)
}
