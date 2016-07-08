package com.twitter.finatra.conversions

import com.github.nscala_time.time.{DurationBuilder, Implicits}
import com.twitter.util.{Duration => TwitterDuration, Time}
import org.joda.time.{DateTime, DateTimeZone, Duration}

/**
 * Add additional conversions to 'scala-time' and also
 * overcome issues with scala time joda wrappers not being serializable by jackson
 */
object time extends Implicits {

  private[time] val LongTimeFromNowMillis = new DateTime(9999, 1, 1, 0, 0, 0, 0).getMillis

  // defined here to avoid https://issues.scala-lang.org/browse/SI-8847
  private[time] def utcIso8601(dateTime: DateTime): String = {
    dateTime.withZone(DateTimeZone.UTC).toString
  }

  /* ------------------------------------------------ */
  implicit class RichDateTime(val self: org.joda.time.DateTime) extends AnyVal {
    def utcIso8601: String = time.utcIso8601(self)

    def reverseUtcIso8601 = {
      time.utcIso8601(
        new DateTime(LongTimeFromNowMillis - self.getMillis))
    }

    def toTwitterTime: Time = {
      Time.fromMilliseconds(self.getMillis)
    }

    def epochSeconds: Int = {
      (self.getMillis / 1000).toInt
    }
  }

  /* ------------------------------------------------ */
  implicit class RichDuration(val self: Duration) extends AnyVal {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        self.getMillis)
    }
  }

  /* ------------------------------------------------ */
  // com.github.nscala_time.time.DurationBuilder already extends AnyVal
  implicit class RichDurationBuilder(val self: DurationBuilder) {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        self.toDuration.getMillis)
    }
  }

  /* ------------------------------------------------ */
  implicit class RichStringTime(val self: String) extends AnyVal {
    def toDateTime: DateTime = {
      DateTime.parse(self)
    }
  }
}
