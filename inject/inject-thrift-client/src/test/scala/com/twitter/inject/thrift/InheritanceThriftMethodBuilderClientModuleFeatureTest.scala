package com.twitter.inject.thrift

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.thrift.integration.TestThriftServer
import com.twitter.inject.thrift.integration.inheritance.{ServiceBHttpServer, ServiceBThriftService}
import com.twitter.serviceB.thriftscala.ServiceB

class InheritanceThriftMethodBuilderClientModuleFeatureTest
  extends FeatureTest
  with HttpTest {

  private val httpClientId: String = "http-server"

  private val thriftServer = new EmbeddedThriftServer(
    twitterServer = new TestThriftServer(
      new ServiceBThriftService(httpClientId).toThriftService),
    disableTestLogging = true
  )

  val server = new EmbeddedHttpServer(
    twitterServer =
      new ServiceBHttpServer,
    args = Seq(
      s"-thrift.clientId=$httpClientId",
      resolverMap(
        "serviceB-thrift-service" -> thriftServer.thriftHostAndPort)
    )
  )

  override def afterAll(): Unit = {
    thriftServer.close()
    super.afterAll()
  }

  test("ServiceB.ServicePerEndpoint is available from the injector") {
    server.injector.instance[ServiceB.ServicePerEndpoint] should not be null
  }

  test("ServiceB.MethodPerEndpoint is available from the injector") {
    server.injector.instance[ServiceB.MethodPerEndpoint] should not be null
  }

  test("echo") {
    server.httpGet(path = "/echo?msg=Hello!", andExpect = Ok, withBody = "Hello!")

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.inMemoryStats.counters.assert("clnt/serviceB-thrift-client/ServiceA/echo/requests", 1)
    server.inMemoryStats.counters.assert("clnt/serviceB-thrift-client/ServiceA/echo/success", 1)
    server.inMemoryStats.counters.get("clnt/serviceB-thrift-client/ServiceA/echo/failures") should be(None)
    /* assert MethodBuilder stats exist */
    server.inMemoryStats.stats.get("clnt/serviceB-thrift-client/echo/logical/request_latency_ms") should not be None
    server.inMemoryStats.stats.assert("clnt/serviceB-thrift-client/echo/retries", Seq(0.0f))
    /* assert MethodBuilder counters */
    server.inMemoryStats.counters.assert("clnt/serviceB-thrift-client/echo/logical/requests", 1)
    server.inMemoryStats.counters.assert("clnt/serviceB-thrift-client/echo/logical/success", 1)
  }

  test("ping") {
    server.httpGet(path = "/ping", andExpect = Ok, withBody = "pong")

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.inMemoryStats.counters.assert("clnt/serviceB-thrift-client/ServiceB/ping/requests", 1)
    server.inMemoryStats.counters.assert("clnt/serviceB-thrift-client/ServiceB/ping/success", 1)
    server.inMemoryStats.counters.get("clnt/serviceB-thrift-client/ServiceB/ping/failures") should be(None)
    /* assert MethodBuilder stats exist */
    server.inMemoryStats.stats.get("clnt/serviceB-thrift-client/ping/logical/request_latency_ms") should not be None
    // retries are disabled, thus no "clnt/serviceB-thrift-client/ping/retries" stat
    /* assert MethodBuilder counters */
    server.inMemoryStats.counters.assert("clnt/serviceB-thrift-client/ping/logical/requests", 1)
    server.inMemoryStats.counters.assert("clnt/serviceB-thrift-client/ping/logical/success", 1)
  }
}
