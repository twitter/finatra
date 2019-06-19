package com.twitter.finatra.http.tests.filters

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{Failure, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.filters.StatsFilter
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.response.{HttpResponseClassifier, ResponseBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.{Mockito, Test}
import com.twitter.util.{Await, Future, Return, Throw}

class StatsFilterTest extends Test with Mockito {

  private[this] val statsReceiver = new InMemoryStatsReceiver
  private[this] val routeInfo = RouteInfo("foo", "/foo/123")

  private[this] lazy val responseBuilder = new ResponseBuilder(
    objectMapper = FinatraObjectMapper.create(),
    fileResolver = new FileResolver(localDocRoot = "src/main/webapp/", docRoot = ""),
    messageBodyManager = mock[MessageBodyManager],
    statsReceiver = mock[StatsReceiver],
    includeContentTypeCharset = true
  )

  override protected def afterEach(): Unit = {
    statsReceiver.clear()
    super.afterEach()
  }

  test("ignorables are ignored") {
    val statsFilter = new StatsFilter[Request](
      statsReceiver,
      HttpResponseClassifier(ResponseClassifier.Default)
    )

    val request = Request()
    val service = Service.mk[Request, Response] { _ =>
      Future.const(Throw(Failure.ignorable("oops")))
    }

    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      /* only global counters and histos */
      assert(statsReceiver.counters.get(Seq("status", "5XX")).isEmpty)
      assert(statsReceiver.counters.get(Seq("status", "2XX")).isEmpty)
      assert(statsReceiver.stats.get(Seq("time", "5XX")).isEmpty)
      assert(statsReceiver.stats.get(Seq("time", "2XX")).isEmpty)
    }
  }

  test("failed request") {
    val statsFilter = new StatsFilter[Request](
      statsReceiver,
      HttpResponseClassifier.ServerErrorsAsFailures
    )

    val request = Request()
    val service = Service.mk[Request, Response] { _ =>
      Future.exception(new Exception("oops"))
    }

    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      /* only global counters and histos */
      statsReceiver.counters(Seq("status", "5XX")) should equal(1)
      statsReceiver.counters(Seq("status", "500")) should equal(1)
      statsReceiver.stats.get(Seq("time", "5XX")) should not be None
      statsReceiver.stats.get(Seq("time", "500")) should not be None
    }
  }

  test("successful request") {
    val statsFilter = new StatsFilter[Request](
      statsReceiver,
      HttpResponseClassifier.ServerErrorsAsFailures
    )

    val request = Request()
    val service = Service.mk[Request, Response] { _ =>
      Future.value(responseBuilder.ok("Hello, world!"))
    }

    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      /* only global counters and histos */
      statsReceiver.counters(Seq("status", "2XX")) should equal(1)
      statsReceiver.counters(Seq("status", "200")) should equal(1)
      statsReceiver.stats.get(Seq("time", "2XX")) should not be None
      statsReceiver.stats.get(Seq("time", "200")) should not be None
    }
  }

  test("per route successful request") {
    val statsFilter = new StatsFilter[Request](
      statsReceiver,
      HttpResponseClassifier.ServerErrorsAsFailures
    )

    val request = Request()
    val service = Service.mk[Request, Response] { _ =>
      Future.value(responseBuilder.ok("Hello, world!"))
    }

    RouteInfo.set(request, routeInfo)
    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      /* global counters and histos */
      statsReceiver.counters(List("status", "2XX")) should equal(1)
      statsReceiver.counters(List("status", "200")) should equal(1)
      statsReceiver.stats.get(Seq("time", "2XX")) should not be None
      statsReceiver.stats.get(Seq("time", "200")) should not be None

      /* per-route counters */
      statsReceiver.counters(List("route", "foo", "GET", "requests")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "2XX")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "200")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "failures")) should equal(0)
      statsReceiver.counters(List("route", "foo", "GET", "success")) should equal(1)

      /* per-route histos */
      statsReceiver.stats.get(List("route", "foo", "GET", "time")) should not be None
      statsReceiver.stats.get(List("route", "foo", "GET", "time", "2XX")) should not be None
      statsReceiver.stats.get(List("route", "foo", "GET", "time", "200")) should not be None
    }
  }

  test("per route failed request") {
    val statsFilter = new StatsFilter[Request](
      statsReceiver,
      HttpResponseClassifier.ServerErrorsAsFailures
    )

    val request = Request()
    val service = Service.mk[Request, Response] { _ =>
      Future.exception(new Exception("oops"))
    }

    RouteInfo.set(request, routeInfo)
    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      /* global counters and histos */
      statsReceiver.counters(List("status", "5XX")) should equal(1)
      statsReceiver.counters(List("status", "500")) should equal(1)
      statsReceiver.stats.get(Seq("time", "5XX")) should not be None
      statsReceiver.stats.get(Seq("time", "500")) should not be None

      /* per-route counters */
      statsReceiver.counters(List("route", "foo", "GET", "requests")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "5XX")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "500")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "failures")) should equal(1) // server errors as failures
      statsReceiver.counters(List("route", "foo", "GET", "success")) should equal(0)

      /* per-route histos */
      statsReceiver.stats.get(List("route", "foo", "GET", "time")) should not be None
      statsReceiver.stats.get(List("route", "foo", "GET", "time", "5XX")) should not be None
      statsReceiver.stats.get(List("route", "foo", "GET", "time", "500")) should not be None
    }
  }

  test("per route IllegalArgumentException classified as success") {
    val httpResponseClassifier = HttpResponseClassifier(
      ResponseClassifier.named("TestClassifier") {
        case ReqRep(_, Throw(t)) if t.isInstanceOf[IllegalArgumentException] =>
          ResponseClass.Success
      }
    )

    val statsFilter = new StatsFilter[Request](
      statsReceiver,
      httpResponseClassifier
    )

    val request = Request()
    val service = Service.mk[Request, Response] { _ =>
      Future.exception(new IllegalArgumentException("oops"))
    }

    RouteInfo.set(request, routeInfo)
    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      statsReceiver.counters(List("status", "5XX")) should equal(1)
      statsReceiver.counters(List("status", "500")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "requests")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "5XX")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "500")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "failures")) should equal(0) // IllegalArgumentException classified as a success
      statsReceiver.counters(List("route", "foo", "GET", "success")) should equal(1)
    }
  }

  test("per route 200 response classified as failed") {
    val httpResponseClassifier = HttpResponseClassifier(
      ResponseClassifier.named("TestClassifier") {
        case ReqRep(_, Return(r: Response)) if r.contentString == "oops" =>
          ResponseClass.NonRetryableFailure
      }
    )

    val statsFilter = new StatsFilter[Request](
      statsReceiver,
      httpResponseClassifier
    )

    val request = Request()
    val service = Service.mk[Request, Response] { _ =>
      Future.value(responseBuilder.ok("oops"))
    }

    RouteInfo.set(request, routeInfo)
    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      statsReceiver.counters(List("status", "2XX")) should equal(1)
      statsReceiver.counters(List("status", "200")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "requests")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "2XX")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "200")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "failures")) should equal(1) // 200 OK response but classified as a "failure"
      statsReceiver.counters(List("route", "foo", "GET", "success")) should equal(0)
    }
  }

  test("per route 500 response classified as success") {
    val httpResponseClassifier = HttpResponseClassifier(
      ResponseClassifier.named("TestClassifier") {
        case ReqRep(_, Return(r: Response)) if r.statusCode >= 500 && r.statusCode <= 599 =>
          ResponseClass.Success
      }
    )

    val statsFilter = new StatsFilter[Request](
      statsReceiver,
      httpResponseClassifier
    )

    val request = Request()
    val service = Service.mk[Request, Response] { _ =>
      Future.value(responseBuilder.internalServerError("oops"))
    }

    RouteInfo.set(request, routeInfo)
    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      statsReceiver.counters(List("status", "5XX")) should equal(1)
      statsReceiver.counters(List("status", "500")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "requests")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "5XX")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "status", "500")) should equal(1)
      statsReceiver.counters(List("route", "foo", "GET", "failures")) should equal(0) // Any 5xx is a success
      statsReceiver.counters(List("route", "foo", "GET", "success")) should equal(1)
    }
  }

  private[this] def withService(service: Service[Request, Response])(fn: => Unit): Unit = {
    try {
      fn
    } finally {
      service.close()
    }
  }
}
