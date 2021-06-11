package com.twitter.finatra.http.tests.integration.tweetexample.test

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.tests.integration.tweetexample.main.TweetsEndpointServer
import com.twitter.finatra.http.tests.integration.tweetexample.main.domain.Tweet
import com.twitter.finatra.http.{EmbeddedHttpServer, RouteHint, StreamingJsonTestHelper}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future
import scala.collection.mutable

class TweetsControllerIntegrationTest extends FeatureTest {

  val onWriteLog: mutable.ArrayBuffer[String] = new mutable.ArrayBuffer[String]()

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new TweetsEndpointServer,
    defaultRequestHeaders = Map("X-UserId" -> "123"),
    // Set client flags to also start on HTTPS port
    flags = Map("https.port" -> ":0", "cert.path" -> "", "key.path" -> ""),
    disableTestLogging = true
  ).bind[mutable.ArrayBuffer[String]].toInstance(onWriteLog)

  lazy val streamingJsonHelper =
    new StreamingJsonTestHelper(server.mapper)

  override def beforeEach(): Unit = {
    super.beforeEach()
    onWriteLog.clear()
  }

  test("get tweet 1") {
    val tweet =
      server.httpGetJson[Map[String, Long]]("/tweets/1", andExpect = Status.Ok)

    tweet("idonly") should equal(
      1
    ) //confirm response was transformed by registered TweetMessageBodyWriter
  }

  test("post valid tweet") {
    server.httpPost(
      "/tweets/",
      """
      {
        "custom_id": 5,
        "username": "bob",
        "tweet_msg": "hello"
      }""",
      andExpect = Status.Ok,
      withBody = "tweet with id 5 is valid"
    )
  }

  test("post tweet with missing field") {
    server.httpPost(
      "/tweets/",
      """
      {
        "custom_id": 5,
        "tweet_msg": "hello"
      }""",
      andExpect = Status.BadRequest,
      withErrors = Seq("username: field is required")
    )
  }

  test("post tweet with field validation issue") {
    server.httpPost(
      "/tweets/",
      """
      {
        "custom_id": 0,
        "username": "foo",
        "tweet_msg": "hello"
      }""",
      andExpect = Status.BadRequest,
      withErrors = Seq("custom_id: [0] is not greater than or equal to 1")
    )
  }

  test("post tweet with method validation issue") {
    server.httpPost(
      "/tweets/",
      """
      {
        "custom_id": 5,
        "username": "foo",
        "tweet_msg": "hello"
      }""",
      andExpect = Status.BadRequest,
      withErrors = Seq("username cannot be foo")
    )
  }

  test("post streaming json with Reader") {
    val request = RequestBuilder
      .post("/tweets/streaming")
      .header("X-UserId", "123")
      .chunked

    val ids = (1 to 5)

    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, ids, delayMs = 10)
    }

    server.httpRequest(
      request = request,
      andExpect = Status.Ok,
      withJsonBody = """
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
        """
    )
  }

  test("post Reader[Buf] to Reader[String]") {
    server.httpPost(
      "/tweets/reader_buf_to_string",
      postBody = "[1,2,3,4,5]",
      andExpect = Status.Ok,
      withBody = """["[1,2,3,4,5]"]""")
  }

  test("post Reader[Int] to Reader[String]") {
    server.httpPost(
      "/tweets/reader_int_to_string",
      postBody = "[1,2,3,4,5]",
      andExpect = Status.Ok,
      withBody = """["1","2","3","4","5"]""")
  }

  test("post Reader[Buf] to Reader[Buf]") {
    server.httpPost(
      "/tweets/reader_buf",
      postBody = "[1,2,3,4,5]",
      andExpect = Status.Ok,
      withBody = "[1,2,3,4,5]")
  }

  test("post AsyncStream[Buf] to AsyncStream[String]") {
    server.httpPost(
      "/tweets/asyncStream_buf_to_string",
      postBody = "[1,2,3,4,5]",
      andExpect = Status.Ok,
      withBody = """["[1,2,3,4,5]"]""")
  }

  test("post AsyncStream[Int] to AsyncStream[String]") {
    server.httpPost(
      "/tweets/asyncStream_int_to_string",
      postBody = "[1,2,3,4,5]",
      andExpect = Status.Ok,
      withBody = """["1","2","3","4","5"]""")
  }

  test("post AsyncStream[Buf] to AsyncStream[Buf]") {
    server.httpPost(
      "/tweets/asyncStream_buf",
      postBody = "[1,2,3,4,5]",
      andExpect = Status.Ok,
      withBody = "[1,2,3,4,5]")
  }

  test("post streaming json without chunks") {
    server.httpPost("/tweets/streaming", """
      [1,2,3,4,5]
      """)
  }

  test("String to String") {
    server.httpPost(
      "/tweets/request_to_string",
      postBody = "[1,2,3,4,5]",
      andExpect = Status.Ok,
      withBody = """[1,2,3,4,5]""")
  }

  test("String to Future[String]") {
    server.httpPost(
      "/tweets/request_to_futurestring",
      postBody = "[1,2,3,4,5]",
      andExpect = Status.Ok,
      withBody = """[1,2,3,4,5]""")
  }

  test("get streaming json") {
    server.httpGet(
      "/tweets/streaming_json",
      andExpect = Status.Ok,
      withJsonBody = """
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
        """
    )
  }

  test("get StreamingResponse with AsyncStream") {
    server.httpGet(
      "/tweets/streamingRep_with_asyncStream",
      andExpect = Status.Ok,
      withJsonBody = """["A","B","C"]""")
  }

  test("get StreamingResponse with Reader") {
    val response = server.httpGet(
      "/tweets/streamingRep_with_reader",
      andExpect = Status.Accepted,
      withBody = "[1,2,3]")
    response.headerMap.getAll("key1") should equal(Seq("value1", "value2", "value3"))
    response.headerMap.getAll("key2") should equal(Seq("v4", "v5", "v6"))
  }

  test("get StreamingResponse with AsyncStream with transformer ") {
    server.httpGet(
      "/tweets/streamingRep_with_transformer_asyncStream",
      andExpect = Status.Ok,
      withJsonBody = """["a","b","c"]""")
  }

  test("get StreamingResponse with Reader with transformer") {
    server.httpGet(
      "/tweets/streamingRep_with_transformer_reader",
      andExpect = Status.Ok,
      withJsonBody = """["a","b","c"]""")
  }

  test("get streaming manual writes") {
    server.httpGet(
      "/tweets/streaming_manual_writes",
      andExpect = Status.Ok,
      withBody = "helloworld"
    )
  }

  test("post streaming json with StreamingRequest") {
    val request = RequestBuilder
      .post("/tweets/streaming_with_streamingRequest")
      .header("X-UserId", "123")
      .chunked

    val ids = (1 to 5)

    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, ids, delayMs = 10)
    }

    server.httpRequest(
      request = request,
      andExpect = Status.Ok,
      withJsonBody = """
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
        """
    )
  }

  test("get tweet 1 via streamingRequest") {
    val request = RequestBuilder
      .get("/tweets/not_streaming_with_streamingRequest/1")
      .header("X-UserId", "123")
      .chunked

    server.httpRequest(
      request = request,
      andExpect = Status.Ok,
      withJsonBody = """{ "idonly" : 1 }"""
    )
  }

  test("post tweets") {
    val request = RequestBuilder
      .post("/tweets/streaming_req_over_json")
      .header("X-UserId", "123")
      .chunked

    val tweets =
      List(Tweet(1L, "Bob", "whats up"), Tweet(2L, "Sally", "yo"), Tweet(3L, "Fred", "hey"))

    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, tweets, delayMs = 10)
    }
    server.httpRequest(
      request = request,
      andExpect = Status.Ok,
      withJsonBody = "[1,2,3]"
    )
  }

  test("get streaming json with StreamingResponse") {
    val request = RequestBuilder
      .get("/tweets/streaming_rep_over_json")
      .header("X-UserId", "123")
      .chunked

    val ids = (1 to 5)

    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, ids, delayMs = 10)
    }

    server.httpRequest(
      request = request,
      andExpect = Status.Ok,
      withJsonBody = """
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
        """
    )
  }

  test("get admin yo") {
    server.httpGet("/admin/finatra/yo", andExpect = Status.Ok, withBody = "yo yo")
  }

  test("get hello in parallel") {
    pending
    await {
      Future.collect {
        for (_ <- 1 to 500) yield {
          pool {
            sayHello()
          }
        }
      }
    }
  }

  test("get admin users") {
    server.httpGet(
      "/admin/finatra/users/123",
      withBody = "123 from data://prod, 123 from data://staging"
    )
  }

  test("get admin user from admin route without admin path") {
    val response = server.httpGetJson[JsonNode]("/bestuser", routeHint = RouteHint.AdminServer)
    assert(response.get("userName").textValue() == "123 from data://prod")
  }

  test("get ping") {
    server.httpGet("/admin/ping", withBody = "pong")
  }

  test("get health") {
    server.httpGet("/health", routeHint = RouteHint.AdminServer, withBody = "OK\n")
  }

  test("verify max request size overridden") {
    val registry = server.httpGetJson[JsonNode]("/admin/registry.json")

    val maxRequestSize = registry.get("registry").get("flags").get("maxRequestSize").textValue()
    maxRequestSize should equal("10485760.bytes")
  }

  def sayHello(): Unit = {
    server.httpGet(
      "/tweets/hello",
      andExpect = Status.Ok,
      withBody = "hello world",
      suppress = true
    )
  }
}
