package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.time._
import com.twitter.finatra.test.Test
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
  }

  "Duration" should {
    "be convertable to twitter duration" in {
      val duration = 5.seconds
      val twitterDuration = duration.toTwitterDuration
      duration.millis should be(twitterDuration.inMillis)
    }
  }
}
