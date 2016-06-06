package com.twitter.finatra.http.tests.integration.tweetexample.test

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finagle.http.{Fields, Status}
import com.twitter.finatra.http.tests.integration.tweetexample.main.TweetsEndpointServer
import com.twitter.finatra.http.{EmbeddedHttpServer, StreamingJsonTestHelper}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class TweetsControllerIntegrationTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    new TweetsEndpointServer,
    defaultRequestHeaders = Map("X-UserId" -> "123"),
    // Set client flags to also start on HTTPS port
    flags = Map(
      "https.port" -> ":0",
      "cert.path" -> "",
      "key.path" -> ""))

  lazy val streamingJsonHelper =
    new StreamingJsonTestHelper(server.mapper)

  "get tweet 1" in {
    val tweet =
      server.httpGetJson[Map[String, Long]](
        "/tweets/1",
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
      andExpect = Status.BadRequest,
      withErrors = Seq("username: field is required"))
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
      andExpect = Status.BadRequest,
      withErrors = Seq("custom_id: [0] is not greater than or equal to 1"))
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
      andExpect = Status.BadRequest,
      withErrors = Seq("username cannot be foo"))
  }

  "post streaming json" in {
    val request = RequestBuilder
      .post("/tweets/streaming")
      .header("X-UserId", "123")
      .chunked

    val ids = (1 to 5).toSeq

    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, ids, delayMs = 10)
    }

    server.httpRequest(request)
  }

  "post streaming json without chunks" in {
    server.httpPost(
      "/tweets/streaming",
      """
      [1,2,3,4,5]
      """)
  }

  "get streaming json" in {
    server.httpGet(
      "/tweets/streaming_json",
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
      andExpect = Status.Ok,
      withBody = "ABC")
  }

  "get streaming custom toBuf with custom headers" in {
    val response = server.httpGet(
      "/tweets/streaming_custom_tobuf_with_custom_headers",
      andExpect = Status.Created,
      withBody = "ABC")

      response.headerMap.contains(Fields.ContentType) should equal(true)
      response.headerMap.get(Fields.ContentType).get should equal("text/event-stream;charset=UTF-8")
      response.headerMap.contains(Fields.Pragma) should equal(true)
      response.headerMap.get(Fields.Pragma).get should equal("no-cache")
      response.headerMap.contains(Fields.CacheControl) should equal(true)
      response.headerMap.get(Fields.CacheControl).get should equal("no-cache, no-store, max-age=0, must-revalidate")

  }

  "get streaming manual writes" in {
    server.httpGet(
      "/tweets/streaming_manual_writes",
      andExpect = Status.Ok,
      withBody = "helloworld")
  }

  "get admin yo" in {
    server.httpGet(
      "/admin/finatra/yo",
      andExpect = Status.Ok,
      withBody = "yo yo")
  }

  "get hello in parallel" in {
    pending // disabling until pool shutdown added
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
      andExpect = Status.Ok,
      withBody = "hello world",
      suppress = true)
  }
}
