package com.twitter.inject.thrift

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.Test
import com.twitter.inject.thrift.filtered_integration.http_server.GreeterHttpServer
import com.twitter.inject.thrift.filtered_integration.thrift_server.GreeterThriftServer

class GreeterHttpServerFeatureTest extends Test with HttpTest {

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

  test("GreeterHttpServer#Say hi") {
    httpServer.httpGet(path = "/hi?name=Bob", andExpect = Ok, withBody = "Hi Bob")
  }

  test("GreeterHttpServer#Say bye") {
    httpServer.httpGet(
      path = "/bye?name=Bob&age=18",
      andExpect = Ok,
      withBody = "Bye Bob of 18 years!"
    )
  }
}
