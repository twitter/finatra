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
  override val printStats = false

  private val httpClientId: String = "http-server"

  private val thriftServer = new EmbeddedThriftServer(
    twitterServer = new TestThriftServer(
      new ServiceBThriftService(httpClientId).toThriftService)
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
    /* assert counters added by ThriftServiceIface#statsFilter */
    server.assertCounter("clnt/serviceB-thrift-client/ServiceA/echo/requests", 1)
    server.assertCounter("clnt/serviceB-thrift-client/ServiceA/echo/success", 1)
    server.assertCounter("clnt/serviceB-thrift-client/ServiceA/echo/failures", 0)
    /* assert MethodBuilder stats exist */
    server.getStat("clnt/serviceB-thrift-client/echo/logical/request_latency_ms") should not be Seq()
    server.getStat("clnt/serviceB-thrift-client/echo/retries") should be(Seq(0.0))
    /* assert MethodBuilder counters */
    server.assertCounter("clnt/serviceB-thrift-client/echo/logical/requests", 1)
    server.assertCounter("clnt/serviceB-thrift-client/echo/logical/success", 1)
  }

  test("ping") {
    server.httpGet(path = "/ping", andExpect = Ok, withBody = "pong")

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServiceIface#statsFilter */
    server.assertCounter("clnt/serviceB-thrift-client/ServiceB/ping/requests", 1)
    server.assertCounter("clnt/serviceB-thrift-client/ServiceB/ping/success", 1)
    server.assertCounter("clnt/serviceB-thrift-client/ServiceB/ping/failures", 0)
    /* assert MethodBuilder stats exist */
    server.getStat("clnt/serviceB-thrift-client/ping/logical/request_latency_ms") should not be Seq()
    // retries are disabled, thus no "clnt/serviceB-thrift-client/ping/retries" stat
    /* assert MethodBuilder counters */
    server.assertCounter("clnt/serviceB-thrift-client/ping/logical/requests", 1)
    server.assertCounter("clnt/serviceB-thrift-client/ping/logical/success", 1)
  }
}
