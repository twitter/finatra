package com.twitter.finatra.multiserver.test

import com.twitter.finagle.http.Status
import com.twitter.finagle.tracing.Annotation
import com.twitter.finagle.tracing.Record
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.HttpTest
import com.twitter.finatra.multiserver.Add1HttpServer.Add1Server
import com.twitter.finatra.multiserver.AdderThriftServer.AdderThriftServer
import com.twitter.finatra.multiserver.Add2HttpServer.Add2Server
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.ThriftTest
import com.twitter.inject.Test

class MultiServerFeatureTest extends Test with HttpTest with ThriftTest {

  // topology (caller[protocol] --> receiver[protocol]):
  // add2Server[http] --> add1HttpServer[http] --> add1ThriftServer[thrift]

  val add1ThriftServer = new EmbeddedThriftServer(new AdderThriftServer, disableTestLogging = true)
  val add1HttpServer = new EmbeddedHttpServer(
    new Add1Server,
    disableTestLogging = true,
    flags = Map(resolverMap("adder-thrift-server", add1ThriftServer))
  )
  val add2Server = new EmbeddedHttpServer(
    new Add2Server,
    disableTestLogging = true,
    flags = Map(resolverMap("add1-http-server", add1HttpServer))
  )

  test("add2") {
    add2Server.httpGet("/add2?num=5", andExpect = Status.Ok, withBody = "7")

    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/Adder/add1/requests", 4)
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/Adder/add1/success", 2)
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/Adder/add1/failures", 2)
    add1HttpServer.inMemoryStats.counters.assert(
      "clnt/adder-thrift/Adder/add1/failures/org.apache.thrift.TApplicationException",
      2
    )
    add1HttpServer.inMemoryStats.counters.assert("route/add1/GET/status/200", 2)

    add1HttpServer.inMemoryTracer.serviceNames("http")
    add1HttpServer.inMemoryTracer.binaryAnnotations("srv/finagle.label", "http")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.method", "GET")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.uri", "/add1")
    add1HttpServer.inMemoryTracer.serviceNames("adder-thrift")
    add1HttpServer.inMemoryTracer.messages("Retried Request")
    add1HttpServer.inMemoryTracer.binaryAnnotations("clnt/finagle.protocol", "thriftmux")
    add1HttpServer.inMemoryTracer.groupBy(_.traceId).size shouldBe 6
    add1HttpServer.inMemoryTracer.count {
      case Record(_, _, Annotation.Message("Retried Request"), _) => true
      case _ => false
    } shouldBe 2
  }

  test("add1") {
    add1HttpServer.httpGet("/add1String?num=10", andExpect = Status.ServiceUnavailable)

    add1HttpServer.inMemoryTracer.binaryAnnotations("http.status_code", 503)
    add1HttpServer.inMemoryTracer.clear()

    add1HttpServer.httpGet("/add1String?num=10", andExpect = Status.Ok, withBody = "11")

    // client stats
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/Adder/add1String/requests", 6)
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/Adder/add1String/success", 1)
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/Adder/add1String/failures", 5)
    add1HttpServer.inMemoryStats.counters.assert(
      "clnt/adder-thrift/Adder/add1String/failures/org.apache.thrift.TApplicationException",
      5
    )

    // per-route stats
    // two total requests to the route
    add1HttpServer.inMemoryStats.counters.assert("route/add1String/GET/requests", 2)
    // the first resulted failed request
    add1HttpServer.inMemoryStats.counters.assert("route/add1String/GET/status/503", 1)
    add1HttpServer.inMemoryStats.counters
      .assert("route/add1String/GET/status/503/mapped/ThriftClientException", 1)
    // the second an eventual 200 OK
    add1HttpServer.inMemoryStats.counters.assert("route/add1String/GET/status/200", 1)

    // service failure stats
    add1HttpServer.inMemoryStats.counters.assert("service/failure", 1)
    add1HttpServer.inMemoryStats.counters.assert("service/failure/adder-thrift", 1)

    add1HttpServer.inMemoryTracer.serviceNames("http")
    add1HttpServer.inMemoryTracer.binaryAnnotations("srv/finagle.label", "http")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.method", "GET")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.uri", "/add1String")
    add1HttpServer.inMemoryTracer.serviceNames("adder-thrift")
    add1HttpServer.inMemoryTracer.messages("Retried Request")
    add1HttpServer.inMemoryTracer.binaryAnnotations("clnt/finagle.protocol", "thriftmux")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.status_code", 200)
  }

  test("add1 always error") {
    val numHttpRequests = 10
    val maxThriftRequestsPerHttpRequest = 4
    val expectedFailedThriftRequests = numHttpRequests * maxThriftRequestsPerHttpRequest

    for (_ <- 1 to numHttpRequests) {
      add1HttpServer.httpGet(
        "/errorAdd1?num=10",
        andExpect = Status.ServiceUnavailable,
        suppress = true
      )
    }

    // client stats
    add1HttpServer.inMemoryStats.counters.assert(
      "clnt/adder-thrift/Adder/add1AlwaysError/requests",
      expectedFailedThriftRequests
    )
    add1HttpServer.inMemoryStats.counters
      .get("clnt/adder-thrift/Adder/add1AlwaysError/success") should be(None)
    add1HttpServer.inMemoryStats.counters.assert(
      "clnt/adder-thrift/Adder/add1AlwaysError/failures",
      expectedFailedThriftRequests
    )
    add1HttpServer.inMemoryStats.counters.assert(
      "clnt/adder-thrift/Adder/add1AlwaysError/failures/org.apache.thrift.TApplicationException",
      expectedFailedThriftRequests
    )

    // per-route stats
    add1HttpServer.inMemoryStats.counters.assert(
      "route/errorAdd1/GET/failure/adder-thrift/Adder/add1AlwaysError/org.apache.thrift.TApplicationException",
      numHttpRequests
    )
    add1HttpServer.inMemoryStats.counters.assert("route/errorAdd1/GET/status/503", numHttpRequests)
    add1HttpServer.inMemoryStats.counters.assert(
      "route/errorAdd1/GET/status/503/mapped/ThriftClientException",
      numHttpRequests
    )

    // service failure stats
    add1HttpServer.inMemoryStats.counters.assert("service/failure", numHttpRequests)
    add1HttpServer.inMemoryStats.counters.assert("service/failure/adder-thrift", numHttpRequests)
    add1HttpServer.inMemoryStats.counters.assert(
      "service/failure/adder-thrift/Adder/add1AlwaysError/org.apache.thrift.TApplicationException",
      numHttpRequests
    )

    add1HttpServer.inMemoryTracer.serviceNames("http")
    add1HttpServer.inMemoryTracer.binaryAnnotations("srv/finagle.label", "http")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.method", "GET")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.uri", "/errorAdd1")
    add1HttpServer.inMemoryTracer.serviceNames("adder-thrift")
    add1HttpServer.inMemoryTracer.messages("Retried Request")
    add1HttpServer.inMemoryTracer.binaryAnnotations("clnt/finagle.protocol", "thriftmux")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.status_code", 503)
  }

  test("slow add resulting in request timeouts") {
    val numHttpRequests = 1
    val maxThriftRequestsPerHttpRequest = 4
    val expectedFailedThriftRequests = numHttpRequests * maxThriftRequestsPerHttpRequest

    for (_ <- 1 to numHttpRequests) {
      add1HttpServer.httpGet(
        "/slowAdd1?num=10",
        andExpect = Status.ServiceUnavailable,
        suppress = true
      )
    }

    // client stats
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/Adder/add1Slowly/requests")(
      _ <= expectedFailedThriftRequests
    ) // our requests should total at most the expected but may be less.
    add1HttpServer.inMemoryStats.counters
      .get("clnt/adder-thrift/Adder/add1Slowly/success") should be(None)
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/Adder/add1Slowly/failures")(
      _ <= expectedFailedThriftRequests
    ) // our failures should total at most the expected but may be less.
    add1HttpServer.inMemoryStats.counters.assert(
      "clnt/adder-thrift/add1Slowly/logical/failures/com.twitter.finagle.IndividualRequestTimeoutException"
    )(
      _ <= expectedFailedThriftRequests
    ) // our failures should total at most the expected but may be less.
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/requests")(
      _ <= expectedFailedThriftRequests
    ) // our requests should total at most the expected but may be less.
    add1HttpServer.inMemoryStats.counters.assert("clnt/adder-thrift/failures")(
      _ <= expectedFailedThriftRequests
    ) // our requests should total at most the expected but may be less.
    add1HttpServer.inMemoryStats.counters
      .assert("clnt/adder-thrift/failures/com.twitter.finagle.IndividualRequestTimeoutException")(
        _ <= expectedFailedThriftRequests
      ) // our requests should total at most the expected but may be less.

    // per-route stats
    add1HttpServer.inMemoryStats.counters.assert(
      "route/slowAdd1/GET/failure/adder-thrift/Adder/add1Slowly/com.twitter.finagle.IndividualRequestTimeoutException",
      numHttpRequests
    )
    add1HttpServer.inMemoryStats.counters.assert(
      "route/slowAdd1/GET/failures",
      numHttpRequests
    )
    add1HttpServer.inMemoryStats.counters.assert(
      "route/slowAdd1/GET/requests",
      numHttpRequests
    )
    add1HttpServer.inMemoryStats.counters.assert(
      "route/slowAdd1/GET/status/503",
      numHttpRequests
    )
    add1HttpServer.inMemoryStats.counters.assert(
      "route/slowAdd1/GET/status/503/mapped/ThriftClientException",
      numHttpRequests
    )
    add1HttpServer.inMemoryStats.counters.assert(
      "route/slowAdd1/GET/status/5XX",
      numHttpRequests
    )
    // no success but there is a counter which is 0
    add1HttpServer.inMemoryStats.counters.assert(
      "route/slowAdd1/GET/success",
      0
    )

    // service failure stats
    add1HttpServer.inMemoryStats.counters.assert("service/failure", 1)
    add1HttpServer.inMemoryStats.counters.assert("service/failure/adder-thrift", 1)
    add1HttpServer.inMemoryStats.counters.assert(
      "service/failure/adder-thrift/Adder/add1Slowly/com.twitter.finagle.IndividualRequestTimeoutException",
      1
    )

    add1HttpServer.inMemoryTracer.serviceNames("http")
    add1HttpServer.inMemoryTracer.binaryAnnotations("srv/finagle.label", "http")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.method", "GET")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.uri", "/slowAdd1")
    add1HttpServer.inMemoryTracer.serviceNames("adder-thrift")
    add1HttpServer.inMemoryTracer.messages("Retried Request")
    add1HttpServer.inMemoryTracer.binaryAnnotations("clnt/finagle.protocol", "thriftmux")
    add1HttpServer.inMemoryTracer.binaryAnnotations("http.status_code", 503)
  }

  override protected def afterEach(): Unit = {
    add1HttpServer.clearStats()
    add1HttpServer.clearTraces()
  }

  override def afterAll(): Unit = {
    add1ThriftServer.close()
    add1HttpServer.close()
    add2Server.close()
  }
}
