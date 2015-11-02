package com.twitter.inject.thrift.integration

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.test.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.inject.thrift.integration.http_server.EchoHttpServer
import com.twitter.inject.thrift.integration.thrift_server.EchoThriftServer

class EchoHttpServerFeatureTest extends HttpTest {

  val thriftServer = new EmbeddedThriftServer(
    twitterServer = new EchoThriftServer)

  val httpServer = new EmbeddedHttpServer(
    twitterServer = new EchoHttpServer,
    extraArgs = Seq(
      "-thrift.clientId=echo-http-service",
      resolverMap("thrift-echo-service" -> thriftServer.thriftHostAndPort)))

  "EchoHttpServer" should {
    "Echo 3 times" in {
      httpServer.httpPost(
        path = "/config?timesToEcho=2",
        postBody = "",
        andExpect = Ok,
        withBody = "2")

      httpServer.httpPost(
        path = "/config?timesToEcho=3",
        postBody = "",
        andExpect = Ok,
        withBody = "3")

      httpServer.httpGet(
        path = "/echo?msg=Bob",
        andExpect = Ok,
        withBody = "BobBobBob")

      httpServer.printStats()

      httpServer.close()
      thriftServer.close()
    }
  }
}
