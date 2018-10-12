package com.twitter.finatra.thrift.tests

import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.tracing.Trace
import com.twitter.finatra.thrift.ThriftRequest
import com.twitter.finatra.thrift.filters.StatsFilter
import com.twitter.finatra.thrift.response.ThriftResponseClassifier
import com.twitter.inject.Test
import com.twitter.util.{Await, Future, Return, Throw}

class StatsFilterTest extends Test {

  private[this] val statsReceiver = new InMemoryStatsReceiver

  override protected def afterEach(): Unit = {
    statsReceiver.clear()
    super.afterEach()
  }

  test("successful request") {
    val statsFilter = new StatsFilter(
      statsReceiver,
      ThriftResponseClassifier.ThriftExceptionsAsFailures
    )

    val request = ThriftRequest("foo", Trace.nextId, None, "Hello, world!")
    val service = Service.mk[ThriftRequest[String], String] { _ =>
      Future.value("Hello, world!")
    }

    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      // top-level exceptions
      statsReceiver.counters(List("exceptions")) should equal(0)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "foo", "failures")) should equal(0)
      // per method success
      statsReceiver.counters(List("per_method_stats", "foo", "success")) should equal(1)
      // per method latency_ms
      statsReceiver.stats.get(List("per_method_stats", "foo", "latency_ms")) should not be None
    }
  }

  test("failed request") {
    val statsFilter = new StatsFilter(
      statsReceiver,
      ThriftResponseClassifier.ThriftExceptionsAsFailures
    )

    val request = ThriftRequest("foo", Trace.nextId, None, "Hello, world!")
    val service = Service.mk[ThriftRequest[String], String] { _ =>
      Future.exception(new Exception("oops"))
    }

    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      // top-level exceptions
      statsReceiver.counters(List("exceptions")) should equal(1)
      statsReceiver.counters(List("exceptions", "java.lang.Exception")) should equal(1)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "foo", "failures")) should equal(1)
      statsReceiver.counters(List("per_method_stats", "foo", "failures", "java.lang.Exception")) should equal(1)
      // per method success
      statsReceiver.counters(List("per_method_stats", "foo", "success")) should equal(0)
      // per method latency_ms
      statsReceiver.stats.get(List("per_method_stats", "foo", "latency_ms")) should not be None
    }
  }

  test("failed request classified as success") {
    val thriftResponseClassifier = ThriftResponseClassifier(
      ResponseClassifier.named("TestClassifier") {
        case ReqRep(_, Throw(t)) if t.isInstanceOf[IllegalArgumentException] =>
          ResponseClass.Success
      }
    )

    val statsFilter = new StatsFilter(
      statsReceiver,
      thriftResponseClassifier
    )

    val request = ThriftRequest("foo", Trace.nextId, None, "Hello, world!")
    val service = Service.mk[ThriftRequest[String], String] { _ =>
      Future.exception(new IllegalArgumentException("oops"))
    }

    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      // top-level exceptions
      statsReceiver.counters(List("exceptions")) should equal(1)
      statsReceiver.counters(List("exceptions", "java.lang.IllegalArgumentException")) should equal(1)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "foo", "failures")) should equal(0)
      // per method success
      statsReceiver.counters(List("per_method_stats", "foo", "success")) should equal(1)
      statsReceiver.counters(List("per_method_stats", "foo", "success", "java.lang.IllegalArgumentException")) should equal(1)
      // per method latency_ms
      statsReceiver.stats.get(List("per_method_stats", "foo", "latency_ms")) should not be None
    }
  }

  test("successful request classified as failed") {
    val thriftResponseClassifier = ThriftResponseClassifier(
      ResponseClassifier.named("TestClassifier") {
        case ReqRep(_, Return(r)) if r.isInstanceOf[String] && r.asInstanceOf[String] == "oops" =>
          ResponseClass.NonRetryableFailure
      }
    )

    val statsFilter = new StatsFilter(
      statsReceiver,
      thriftResponseClassifier
    )

    val request = ThriftRequest("foo", Trace.nextId, None, "Hello, world!")
    val service = Service.mk[ThriftRequest[String], String] { _ =>
      Future.value("oops")
    }

    withService(service) {
      Await.ready(statsFilter.apply(request, service), 2.seconds)
      // top-level exceptions -- no exception is recorded
      statsReceiver.counters(List("exceptions")) should equal(0)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "foo", "failures")) should equal(1)
      // per method success
      statsReceiver.counters(List("per_method_stats", "foo", "success")) should equal(0)
      // per method latency_ms
      statsReceiver.stats.get(List("per_method_stats", "foo", "latency_ms")) should not be None
    }
  }

  private[this] def withService[T](service: Service[ThriftRequest[T], T])(fn: => Unit): Unit = {
    try {
      fn
    } finally {
      service.close()
    }
  }
}
