package com.twitter.finatra.multiserver.test

import com.twitter.adder.thriftscala.Adder
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.multiserver.CombinedServer.{AdminAdd1Request, DoEverythingCombinedServer}
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class DoEverythingCombinedServerFeatureTest extends FeatureTest {

  val server = new EmbeddedHttpServer(
    twitterServer = new DoEverythingCombinedServer,
    flags = Map("https.port" -> ":0") // for testing `EmbeddedHttpServer.logStartup` method
  )
  with ThriftClient

  lazy val client = server.thriftClient[Adder[Future]](clientId = "client123")

  test("bind thrift external port") {
    server.thriftExternalPort should not be 0
  }

  test("ping") {
    server.httpGet("/ping")
  }

  test("add1 http") {
    server.httpGet("/add1?num=5", andExpect = Status.Ok, withBody = "6")
  }

  test("add1 admin") {
    server.httpPost(
      "/admin/finatra/add1",
      routeToAdminServer = true,
      postBody = server.mapper.writeValueAsString(AdminAdd1Request(num = 5)),
      andExpect = Status.Ok,
      withBody = "6"
    )
  }

  test("admin foo") {
    server.httpGet(
      "/admin/foo",
      routeToAdminServer = true,
      andExpect = Status.Ok,
      withBody = "Bar"
    )
  }

  test("add1 thrift") {
    Await.result {
      client.add1(5)
    } should be(6)
  }
}
