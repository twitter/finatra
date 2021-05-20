package com.twitter.finatra.multiserver.test

import com.twitter.adder.thriftscala.Adder
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.{EmbeddedHttpServer, RouteHint}
import com.twitter.finatra.multiserver.CombinedServer.{AdminAdd1Request, DoEverythingCombinedServer}
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class DoEverythingCombinedServerFeatureTest extends FeatureTest {

  val server = new EmbeddedHttpServer(
    twitterServer = new DoEverythingCombinedServer,
    disableTestLogging = true,
    flags = Map("https.port" -> ":0") // for testing `EmbeddedHttpServer.logStartup` method
  ) with ThriftClient

  lazy val client: Adder[Future] = server.thriftClient[Adder[Future]](clientId = "client123")

  test("bind thrift external port") {
    server.thriftExternalPort should not be 0
  }

  test("ping") {
    server.httpGet("/ping")
  }

  test("add1 http") {
    server.httpGet("/add1?num=5", andExpect = Status.Ok, withBody = "6")
  }

  test("GET /admin/registry.json") {
    val json = server.httpGetJson[Map[String, Any]](
      "/admin/registry.json",
      andExpect = Status.Ok,
      routeHint = RouteHint.AdminServer)

    val registry = json("registry").asInstanceOf[Map[String, Any]]
    registry.contains("library") should be(true)
    registry("library").asInstanceOf[Map[String, String]].contains("finatra") should be(true)

    val finatra = registry("library")
      .asInstanceOf[Map[String, Any]]("finatra")
      .asInstanceOf[Map[String, Any]]

    finatra.contains("http") should be(true)
    val http = finatra("http").asInstanceOf[Map[String, Any]]
    http.contains("filters") should be(true)
    http.contains("routes") should be(true)

    val routes = http("routes").asInstanceOf[Map[String, Any]]
    routes.size should be > 0

    finatra.contains("thrift") should be(true)
    val thrift = finatra("thrift").asInstanceOf[Map[String, Any]]
    thrift.contains("filters") should be(true)
    thrift.contains("methods") should be(true)

    val methods = thrift("methods").asInstanceOf[Map[String, Any]]
    methods.size should be > 0
  }

  test("add1 admin") {
    server.httpPost(
      "/admin/finatra/add1",
      routeHint = RouteHint.AdminServer,
      postBody = server.mapper.writeValueAsString(AdminAdd1Request(num = 5)),
      andExpect = Status.Ok,
      withBody = "6"
    )
  }

  test("admin foo") {
    server.httpGet(
      "/admin/foo",
      routeHint = RouteHint.AdminServer,
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
