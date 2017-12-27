package com.twitter.inject.thrift

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.Test
import com.twitter.inject.thrift.filtered_integration.http_server.GreeterHttpServer
import com.twitter.inject.thrift.filtered_integration.thrift_server.GreeterThriftServer

class DoEverythingFilteredThriftClientModuleFeatureTest extends Test with HttpTest {

  val thriftServer = new EmbeddedThriftServer(twitterServer = new GreeterThriftServer)

  val httpServer = new EmbeddedHttpServer(
    twitterServer = new GreeterHttpServer,
    args = Seq(
      "-thrift.clientId=greeter-http-service",
      resolverMap("greeter-thrift-service" -> thriftServer.thriftHostAndPort)
    )
  )

  override def afterAll() {
    super.afterAll()
    httpServer.close()
    thriftServer.close()
  }

  override protected def afterEach(): Unit = {
    httpServer.clearStats()
    thriftServer.clearStats()
    super.afterEach()
  }

  test("GreeterHttpServer#Say hi") {
    httpServer.httpGet(path = "/hi?name=Bob", andExpect = Ok, withBody = "Hi Bob")

    // per-method -- all the requests in this test were to the same method
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/hi/invocations", 1)
    /* assert counters added by ThriftServiceIface#statsFilter */
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/hi/requests", 4)
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/hi/success", 2)
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/hi/failures", 2)
    /* assert latency stat exists */
    httpServer.getStat("clnt/greeter-thrift-client/Greeter/hi/latency_ms") should not be Seq()
  }

  test("GreeterHttpServer#Say bye") {
    httpServer.httpGet(
      path = "/bye?name=Bob&age=18",
      andExpect = Ok,
      withBody = "Bye Bob of 18 years!"
    )

    // per-method -- all the requests in this test were to the same method
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/bye/invocations", 1)
    /* assert counters added by StatsFilter */
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/bye/requests", 3)
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/bye/success", 1)
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/bye/failures", 2)
    /* assert latency stat exists */
    httpServer.getStat("clnt/greeter-thrift-client/Greeter/bye/latency_ms") should not be Seq()
    httpServer.getStat("clnt/greeter-thrift-client/Greeter/bye/request_latency_ms") should not be Seq()
  }
}
