package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.time._
import org.joda.time.DateTime

class TimeConversionsTest extends Test {

  test("DateTime#be comparable") {
    val d1 = DateTime.now
    val d2 = d1.plusMinutes(1)
    d1 < d2 should be(true)
  }

  test("DateTime#be convertable to twitter time") {
    val dateTime = DateTime.now
    val twitterTime = dateTime.toTwitterTime
    dateTime.getMillis should equal(twitterTime.inMillis)
  }

  test("DateTime#utc iso") {
    val dateStr = "2014-05-30TZ"
    val expectedDate = new DateTime(dateStr)
    val date = dateStr.toDateTime
    date should be(expectedDate)
    date.utcIso8601 should be("2014-05-30T00:00:00.000Z")
    date.reverseUtcIso8601 should be("9954-08-05T00:00:00.000Z")
  }

  test("DateTime#epochSeconds") {
    val date = new DateTime("2014-05-30TZ")
    date.epochSeconds should be(1401408000)
  }

  test("Duration#be convertible to twitter duration") {
    val duration = 5.seconds
    val twitterDuration = duration.toTwitterDuration
    duration.millis should be(twitterDuration.inMillis)
  }
}
