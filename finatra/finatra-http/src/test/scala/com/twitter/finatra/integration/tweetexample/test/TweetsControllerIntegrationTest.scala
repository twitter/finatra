package com.twitter.finatra.integration.tweetexample.test

import com.twitter.finagle.http.Status
import com.twitter.finatra.integration.tweetexample.main.TweetsEndpointServer
import com.twitter.finatra.test.{EmbeddedTwitterServer, Test}
import com.twitter.util.{Await, Future, FuturePool}

class TweetsControllerIntegrationTest extends Test {

  val server = EmbeddedTwitterServer(new TweetsEndpointServer)

  "get tweet 20" in {
    val tweet =
      server.httpGetJson[Map[String, Long]](
        "/tweets/20",
        headers = Map("X-UserId" -> "123"),
        andExpect = Status.Ok)

    tweet("idonly") should equal(20) //confirm response was transformed by registered TweetMessageBodyWriter
  }

  "get admin yo" in {
    server.httpGet(
      "/admin/finatra/yo",
      andExpect = Status.Ok,
      withBody = "yo yo")
  }

  val pool = FuturePool.unboundedPool
  "get hello in parallel" in {
    Await.result {
      Future.collect {
        for (i <- 1 to 500) yield {
          pool {
            sayHello()
          }
        }
      }
    }
  }

  "get admin users" in {
    server.httpGet(
      "/admin/finatra/users/123",
      withBody = "123 from data://prod, 123 from data://staging")
  }

  "get ping" in {
    server.httpGet(
      "/admin/ping",
      withBody = "pong")
  }

  "get health" in {
    server.httpGet(
      "/health",
      routeToAdminServer = true,
      withBody = "OK\n")
  }

  def sayHello() = {
    server.httpGet(
      "/tweets/hello",
      headers = Map("X-UserId" -> "123"),
      andExpect = Status.Ok,
      withBody = "hello world",
      suppress = true)
  }
}
