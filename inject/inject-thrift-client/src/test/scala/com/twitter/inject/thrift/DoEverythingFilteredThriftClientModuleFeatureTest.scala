package com.twitter.inject.thrift

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.thrift.integration.filtered.{GreeterFilteredThriftClientModule, GreeterHttpController, GreeterThriftService}
import com.twitter.inject.thrift.integration.{TestHttpServer, TestThriftServer}

class DoEverythingFilteredThriftClientModuleFeatureTest
  extends FeatureTest
  with HttpTest {
  override val printStats = false

  private val thriftServer = new EmbeddedThriftServer(
    twitterServer = new TestThriftServer(new GreeterThriftService)
  )

  override val server = new EmbeddedHttpServer(
    twitterServer =
      new TestHttpServer[GreeterHttpController](
        "greeter-server",
        GreeterFilteredThriftClientModule),
    args = Seq(
      "-thrift.clientId=greeter-http-service",
      resolverMap(
        "greeter-thrift-service" -> thriftServer.thriftHostAndPort)
    )
  )

  override def afterAll(): Unit = {
    thriftServer.close()
    super.afterAll()
  }

  override protected def afterEach(): Unit = {
    server.clearStats()
    thriftServer.clearStats()
    super.afterEach()
  }

  test("Say hi") {
    server.httpGet(path = "/hi?name=Bob", andExpect = Ok, withBody = "Hi Bob")

    // per-method -- all the requests in this test were to the same method
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hi/invocations", 1)
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hi/requests", 4)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hi/success", 2)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hi/failures", 2)
    /* assert latency stat exists */
    server.getStat("clnt/greeter-thrift-client/Greeter/hi/latency_ms") should not be Seq()
  }

  test("Say bye") {
    server.httpGet(
      path = "/bye?name=Bob&age=18",
      andExpect = Ok,
      withBody = "Bye Bob of 18 years!"
    )

    // per-method -- all the requests in this test were to the same method
    server.assertCounter("clnt/greeter-thrift-client/Greeter/bye/invocations", 1)
    /* assert counters added by StatsFilter */
    server.assertCounter("clnt/greeter-thrift-client/Greeter/bye/requests", 3)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/bye/success", 1)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/bye/failures", 2)
    /* assert latency stat exists */
    server.getStat("clnt/greeter-thrift-client/Greeter/bye/latency_ms") should not be Seq()
    server.getStat("clnt/greeter-thrift-client/Greeter/bye/request_latency_ms") should not be Seq()
  }
}
