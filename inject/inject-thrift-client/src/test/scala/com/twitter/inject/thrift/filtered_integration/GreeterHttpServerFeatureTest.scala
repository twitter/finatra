package com.twitter.inject.thrift.filtered_integration

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.thrift.filtered_integration.http_server.GreeterHttpServer
import com.twitter.inject.thrift.filtered_integration.thrift_server.GreeterThriftServer

class GreeterHttpServerFeatureTest extends HttpTest {

  val thriftServer = new EmbeddedThriftServer(
    twitterServer = new GreeterThriftServer)

  val httpServer = new EmbeddedHttpServer(
    twitterServer = new GreeterHttpServer,
    args = Seq(
      "-thrift.clientId=greeter-http-service",
      resolverMap("greeter-thrift-service" -> thriftServer.thriftHostAndPort)))

  override def afterAll() {
    super.afterAll()
    httpServer.close()
    thriftServer.close()
  }

  "GreeterHttpServer" should {
    "Say hi" in {
      httpServer.httpGet(
        path = "/hi?name=Bob",
        andExpect = Ok,
        withBody = "Hi Bob")
    }

    "Say bye" in {
      httpServer.httpGet(
        path = "/bye?name=Bob&age=18",
        andExpect = Ok,
        withBody = "Bye Bob of 18 years!")
    }
  }
}
