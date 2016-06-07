package com.twitter.finatra.http.tests.filters

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.http.filters.StatsFilter
import com.twitter.inject.Test
import com.twitter.util.{Await, Future}

class StatsFilterTest extends Test {

  val statsReceiver = new InMemoryStatsReceiver
  val statsFilter = new StatsFilter[Request](statsReceiver)

  "test failed request" in {
    val request = Request()
    val service = Service.mk[Request, Response] { request =>
      Future.exception(new Exception("oops"))
    }

    try {
      Await.ready(statsFilter.apply(request, service))
      statsReceiver.counters.get(List("status", "5XX")) should equal(Some(1))
      statsReceiver.counters.get(List("status", "500")) should equal(Some(1))
    }
    finally {
      service.close()
    }
  }
}
