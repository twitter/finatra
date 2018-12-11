package com.twitter.finatra.thrift.tests

import com.twitter.conversions.time._
import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.finagle.Service
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.finagle.stats.InMemoryStatsReceiver
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

  def newFilter(responseClassifier: ThriftResponseClassifier) = {
    new StatsFilter(statsReceiver, responseClassifier)
  }

  test("successful request") {
    val statsFilter = newFilter(
      ThriftResponseClassifier.ThriftExceptionsAsFailures
    )

    val request = "Hello, world!"
    val service = statsFilter.andThen(Service.mk[String, String] { _ =>
      Future.value("Hello, world!")
    })

    withService(service) {
      Await.ready(service(request), 2.seconds)
      // top-level exceptions
      statsReceiver.counters(List("exceptions")) should equal(0)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "echo", "failures")) should equal(0)
      // per method success
      statsReceiver.counters(List("per_method_stats", "echo", "success")) should equal(1)
      // per method latency_ms
      statsReceiver.stats.get(List("per_method_stats", "echo", "latency_ms")) should not be None
    }
  }

  test("failed request") {
    val statsFilter = newFilter(
      ThriftResponseClassifier.ThriftExceptionsAsFailures
    )

    val request = "Hello, world!"
    val service = statsFilter.andThen(Service.mk[String, String] { _ =>
      Future.exception(new Exception("oops"))
    })

    withService(service) {
      Await.ready(service(request), 2.seconds)
      // top-level exceptions
      statsReceiver.counters(List("exceptions")) should equal(1)
      statsReceiver.counters(List("exceptions", "java.lang.Exception")) should equal(1)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "echo", "failures")) should equal(1)
      statsReceiver.counters(List("per_method_stats", "echo", "failures", "java.lang.Exception")) should equal(1)
      // per method success
      statsReceiver.counters(List("per_method_stats", "echo", "success")) should equal(0)
      // per method latency_ms
      statsReceiver.stats.get(List("per_method_stats", "echo", "latency_ms")) should not be None
    }
  }

  test("failed request classified as success") {
    val thriftResponseClassifier = ThriftResponseClassifier(
      ResponseClassifier.named("TestClassifier") {
        case ReqRep(_, Throw(t)) if t.isInstanceOf[IllegalArgumentException] =>
          ResponseClass.Success
      }
    )

    val statsFilter = newFilter(
      thriftResponseClassifier
    )

    val request = "Hello, world!"
    val service = statsFilter.andThen(Service.mk[String, String] { _ =>
      Future.exception(new IllegalArgumentException("oops"))
    })

    withService(service) {
      Await.ready(service(request), 2.seconds)
      // top-level exceptions
      statsReceiver.counters(List("exceptions")) should equal(1)
      statsReceiver.counters(List("exceptions", "java.lang.IllegalArgumentException")) should equal(1)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "echo", "failures")) should equal(0)
      // per method success
      statsReceiver.counters(List("per_method_stats", "echo", "success")) should equal(1)
      statsReceiver.counters(List("per_method_stats", "echo", "success", "java.lang.IllegalArgumentException")) should equal(1)
      // per method latency_ms
      statsReceiver.stats.get(List("per_method_stats", "echo", "latency_ms")) should not be None
    }
  }

  test("successful request classified as failed") {
    val thriftResponseClassifier = ThriftResponseClassifier(
      ResponseClassifier.named("TestClassifier") {
        case ReqRep(_, Return(r)) if r.isInstanceOf[String] && r.asInstanceOf[String] == "oops" =>
          ResponseClass.NonRetryableFailure
      }
    )

    val statsFilter = newFilter(
      thriftResponseClassifier
    )

    val request = "Hello, world!"
    val service = statsFilter.andThen(Service.mk[String, String] { _ =>
      Future.value("oops")
    })

    withService(service) {
      Await.ready(service(request), 2.seconds)
      // top-level exceptions -- no exception is recorded
      statsReceiver.counters(List("exceptions")) should equal(0)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "echo", "failures")) should equal(1)
      // per method success
      statsReceiver.counters(List("per_method_stats", "echo", "success")) should equal(0)
      // per method latency_ms
      statsReceiver.stats.get(List("per_method_stats", "echo", "latency_ms")) should not be None
    }
  }

  private[this] val methodMeta = MethodMetadata(DoEverything.Echo)

  private[this] def withService[T](service: Service[T, T])(fn: => Unit): Unit = {
    try {
      methodMeta.asCurrent(fn)
    } finally {
      service.close()
    }
  }
}
