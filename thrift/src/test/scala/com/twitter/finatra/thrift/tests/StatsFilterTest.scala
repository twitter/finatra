package com.twitter.finatra.thrift.tests

import com.twitter.conversions.DurationOps._
import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.finagle.{Failure, Service}
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

  test("ignorables are ignored") {
    val statsFilter = newFilter(ThriftResponseClassifier(ResponseClassifier.Default))
    val service = statsFilter.andThen(Service.mk[String, String] {
      case "hi" => Future.const(Throw(Failure.ignorable("meh")))
      case _ => Future.exception(new Exception("oops"))
    })

    withService(service) {
      Await.ready(service("hi"), 2.seconds)
      statsReceiver.counters(List("exceptions")) should equal(1)
      statsReceiver.counters(List("per_method_stats", "echo", "ignored")) should equal(1)
      statsReceiver.counters(
        List("per_method_stats", "echo", "ignored", "com.twitter.finagle.Failure")
      ) should equal(1)
      statsReceiver.counters(List("per_method_stats", "echo", "failures")) should equal(0)
      statsReceiver.counters(List("per_method_stats", "echo", "success")) should equal(0)
      Await.ready(service("hello"), 2.seconds)
      statsReceiver.counters(List("exceptions")) should equal(2)
      statsReceiver.counters(List("per_method_stats", "echo", "failures")) should equal(1)
      statsReceiver.counters(List("per_method_stats", "echo", "success")) should equal(0)
      statsReceiver.stats.get(List("per_method_stats", "echo", "latency_ms")) should not be None
    }
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
      // per method ignored
      statsReceiver.counters(List("per_method_stats", "echo", "ignored")) should equal(0)
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
      // per method ignored
      statsReceiver.counters(List("per_method_stats", "echo", "ignored")) should equal(0)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "echo", "failures")) should equal(1)
      statsReceiver.counters(List("per_method_stats", "echo", "failures", "java.lang.Exception")) should equal(
        1)
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
      statsReceiver.counters(List("exceptions", "java.lang.IllegalArgumentException")) should equal(
        1)
      // per method ignored
      statsReceiver.counters(List("per_method_stats", "echo", "ignored")) should equal(0)
      // per method failures
      statsReceiver.counters(List("per_method_stats", "echo", "failures")) should equal(0)
      // per method success
      statsReceiver.counters(List("per_method_stats", "echo", "success")) should equal(1)
      statsReceiver.counters(List(
        "per_method_stats",
        "echo",
        "success",
        "java.lang.IllegalArgumentException")) should equal(1)
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
      // per method ignored
      statsReceiver.counters(List("per_method_stats", "echo", "ignored")) should equal(0)
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
