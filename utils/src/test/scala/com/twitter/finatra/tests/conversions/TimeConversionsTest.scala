package com.twitter.finatra.tests.conversions

import com.twitter.finatra.conversions.time._
import com.twitter.inject.Test
import org.joda.time.DateTime

class TimeConversionsTest extends Test {

  "DateTime" should {
    "be comparable" in {
      val d1 = DateTime.now
      val d2 = d1.plusMinutes(1)
      d1 < d2 should be(true)
    }

    "be convertable to twitter time" in {
      val dateTime = DateTime.now
      val twitterTime = dateTime.toTwitterTime
      dateTime.getMillis should equal(twitterTime.inMillis)
    }

    "utc iso" in {
      val dateStr = "2014-05-30TZ"
      val expectedDate = new DateTime(dateStr)
      val date = dateStr.toDateTime
      date should be(expectedDate)
      date.utcIso8601 should be("2014-05-30T00:00:00.000Z")
      date.reverseUtcIso8601 should be("9954-08-05T00:00:00.000Z")
    }

    "epochSeconds" in {
      val date = new DateTime("2014-05-30TZ")
      date.epochSeconds should be(1401408000)
    }
  }

  "Duration" should {
    "be convertible to twitter duration" in {
      val duration = 5.seconds
      val twitterDuration = duration.toTwitterDuration
      duration.millis should be(twitterDuration.inMillis)
    }
  }
}
