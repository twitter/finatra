package com.twitter.finatra.kafkastreams.query

import com.twitter.inject.Test
import com.twitter.conversions.DurationOps._
import org.joda.time.{DateTime, DateTimeUtils}

class QueryableFinatraWindowStoreTest extends Test {

  test("defaultQueryRangeMillis") {
    val queryRangeMillis = QueryableFinatraWindowStore.defaultQueryRange(
      windowSize = 1.hour,
      allowedLateness = 5.minutes,
      queryableAfterClose = 30.minutes)

    queryRangeMillis should equal(2.hours)
  }

  test("queryStartAndEndTime with no query start and end times") {
    val now = new DateTime("2019-02-21T19:16:00Z")

    DateTimeUtils.setCurrentMillisFixed(now.getMillis)

    val (start, end) = QueryableFinatraWindowStore.queryStartAndEndTime(
      windowSize = 1.hour,
      defaultQueryRange = 2.hours,
      queryParamStartTime = None,
      queryParamEndTime = None)

    val nowWindowEnd = now.hourOfDay().roundCeilingCopy()

    start should equal(nowWindowEnd.getMillis - 2.hours.inMillis)
    end should equal(nowWindowEnd.getMillis)
  }
}
