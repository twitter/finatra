package com.twitter.finatra.http.integration.tweetexample.test

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.integration.tweetexample.main.TweetsEndpointServer
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future, FuturePool}

class TweetsControllerIntegrationTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    new TweetsEndpointServer)

  "get tweet 1" in {
    val tweet =
      server.httpGetJson[Map[String, Long]](
        "/tweets/1",
        headers = Map("X-UserId" -> "123"),
        andExpect = Status.Ok)

    tweet("idonly") should equal(1) //confirm response was transformed by registered TweetMessageBodyWriter
  }

  "post valid tweet" in {
    server.httpPost(
      "/tweets/",
      """
      {
        "custom_id": 5,
        "username": "bob",
        "tweet_msg": "hello"
      }""",
      headers = Map("X-UserId" -> "123"),
      andExpect = Status.Ok,
      withBody = "tweet with id 5 is valid")
  }

  "post tweet with missing field" in {
    server.httpPost(
      "/tweets/",
      """
      {
        "custom_id": 5,
        "tweet_msg": "hello"
      }""",
      headers = Map("X-UserId" -> "123"),
      andExpect = Status.BadRequest,
      withErrors = Seq("username is a required field"))
  }

  "post tweet with field validation issue" in {
    server.httpPost(
      "/tweets/",
      """
      {
        "custom_id": 0,
        "username": "foo",
        "tweet_msg": "hello"
      }""",
      headers = Map("X-UserId" -> "123"),
      andExpect = Status.BadRequest,
      withErrors = Seq("custom_id [0] is not greater than or equal to 1"))
  }

  "post tweet with method validation issue" in {
    server.httpPost(
      "/tweets/",
      """
      {
        "custom_id": 5,
        "username": "foo",
        "tweet_msg": "hello"
      }""",
      headers = Map("X-UserId" -> "123"),
      andExpect = Status.BadRequest,
      withErrors = Seq("username cannot be foo"))
  }

  "get streaming json" in {
    server.httpGet(
      "/tweets/streaming_json",
      headers = Map("X-UserId" -> "123"),
      andExpect = Status.Ok,
      withJsonBody =
        """
          [
            {
              "id" : 1,
              "user" : "Bob",
              "msg" : "whats up"
            },
            {
              "id" : 2,
              "user" : "Sally",
              "msg" : "yo"
            },
            {
              "id" : 3,
              "user" : "Fred",
              "msg" : "hey"
            }
          ]
        """)
  }

  "get streaming custom toBuf" in {
    server.httpGet(
      "/tweets/streaming_custom_tobuf",
      headers = Map("X-UserId" -> "123"),
      andExpect = Status.Ok,
      withBody = "ABC")
  }

  "get streaming manual writes" in {
    server.httpGet(
        "/tweets/streaming_manual_writes",
      headers = Map("X-UserId" -> "123"),
      andExpect = Status.Ok,
      withBody = "helloworld")
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

  "verify max request size overridden" in {
    val registry = server.httpGetJson[JsonNode](
      "/admin/registry.json")

    val maxRequestSize = registry.get("registry").get("flags").get("maxRequestSize").textValue()
    maxRequestSize should equal("10485760.bytes")
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
