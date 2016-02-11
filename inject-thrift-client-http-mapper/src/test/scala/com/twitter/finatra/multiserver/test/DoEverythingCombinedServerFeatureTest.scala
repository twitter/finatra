package com.twitter.finatra.multiserver.test

import com.twitter.adder.thriftscala.Adder
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.finatra.multiserver.CombinedServer.DoEverythingCombinedServer
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class DoEverythingCombinedServerFeatureTest extends FeatureTest {

  val server = new EmbeddedHttpServer(
    twitterServer = new DoEverythingCombinedServer)
    with ThriftClient

  lazy val client = server.thriftClient[Adder[Future]](clientId = "client123")

  "server" should {

    "bind thrift external port" in {
      server.thriftExternalPort should not be 0
    }

    "ping" in {
      server.httpGet(
        "/ping")
    }

    "add1 http" in {
      server.httpGet(
        "/add1?num=5",
        andExpect = Status.Ok,
        withBody = "6")
    }

    "add1 thrift" in {
      Await.result {
        client.add1(5)
      } should be(6)
    }
  }
}
