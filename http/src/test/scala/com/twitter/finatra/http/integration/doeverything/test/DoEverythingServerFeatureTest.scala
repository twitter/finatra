package com.twitter.finatra.http.integration.doeverything.test

import com.google.common.net.MediaType.JSON_UTF_8
import com.google.inject.{Key, TypeLiteral}
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.integration.doeverything.main.DoEverythingServer
import com.twitter.finatra.http.integration.doeverything.main.services.DoEverythingService
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.finatra.json.JsonDiff._
import com.twitter.inject.server.FeatureTest
import org.apache.commons.io.IOUtils
import org.jboss.netty.handler.codec.http.HttpMethod

class DoEverythingServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    extraArgs = Array("-magicNum=1", "-moduleMagicNum=2"),
    twitterServer = new DoEverythingServer)

  val doEverythingService = server.injector.instance[DoEverythingService]
  val namedExampleString = server.injector.instance[String]("example")

  def deserializeRequest(name: String) = {
    val requestBytes = IOUtils.toByteArray(getClass.getResourceAsStream(name))
    Request.decodeBytes(requestBytes)
  }

  def counter(key: String): Int = {
    server.inMemoryStatsReceiver.counter(key.split("/"): _*)()
  }

  def stat(key: String): Seq[Float] = {
    server.inMemoryStatsReceiver.stat(key.split("/"): _*)()
  }

  "ExampleServer" should {
    "named string" in {
      namedExampleString should equal("named")
    }

    "response to /example" in {
      val response = server.httpGet("/example/routing/always")
      response.statusCode should equal(200)
      response.contentString should equal("always response")
    }

    "json response to /example" in {
      val response = server.httpGet("/example/routing/json/1")
      response.statusCode should equal(200)
      println(response.contentString)
      jsonDiff(response.contentString, """{"id":"1","name":"bob","magic":"1","module_magic":"2"}""")
    }

    "ok" in {
      server.httpGet(
        "/ok",
        andExpect = Ok)
    }

    "created" in {
      server.httpGet(
        "/created",
        andExpect = Created)
    }

    "accepted" in {
      server.httpGet(
        "/accepted",
        andExpect = Accepted,
        withBody = "accepted")
    }

    "notfound" in {
      server.httpGet(
        "/notfound",
        andExpect = NotFound)
    }

    "notfound exc" in {
      server.httpGet(
        "/notfoundexception",
        andExpect = NotFound)
    }

    "badrequest" in {
      server.httpGet(
        "/badrequest",
        andExpect = BadRequest)
    }

    "forbidden" in {
      server.httpGet(
        "/forbidden",
        andExpect = Forbidden)
    }

    "methodnotallowed" in {
      server.httpGet(
        "/methodnotallowed",
        andExpect = MethodNotAllowed)
    }

    "unavailable" in {
      server.httpGet(
        "/unavailable",
        andExpect = ServiceUnavailable)
    }

    "unauthorized" in {
      server.httpGet(
        "/unauthorized",
        andExpect = Unauthorized)
    }

    "conflict" in {
      server.httpGet(
        "/conflict",
        andExpect = Conflict)
    }

    "servererror exc" in {
      server.httpGet(
        "/servererrorexception",
        andExpect = InternalServerError)
    }

    "serviceunavailable exception" in {
      server.httpGet(
        "/serviceunavailableexception",
        andExpect = ServiceUnavailable)
    }

    "serviceunavailable exception builder status code" in {
      server.httpGet(
        "/responsebuilder_status_code",
        andExpect = ServiceUnavailable)
    }

    "serviceunavailable exception builder" in {
      server.httpGet(
        "/responsebuilder_status",
        andExpect = ServiceUnavailable)
    }

    "redirect" in {
      server.httpGet(
        "/redirect",
        andExpect = TemporaryRedirect)
    }

    "found" in {
      server.httpGet(
        "/found",
        andExpect = Found)
    }

    "future" in {
      server.httpGet(
        "/future",
        andExpect = Ok,
        withBody = "future")
    }

    "post" in {
      server.httpPost(
        "/foo",
        postBody = "",
        andExpect = Ok,
        withBody = "bar")
    }

    "post form" in {
      server.httpFormPost(
        "/formPost",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "bob")
    }

    "post multipart" in {
      val request = deserializeRequest("/multipart/request-POST-android.bytes")
      request.uri = "/multipartParamsEcho"

      server.httpRequest(
        request = request,
        suppress = true,
        andExpect = Ok,
        withJsonBody = """["banner"]""")
    }

    "put multipart" in {
      val request = deserializeRequest("/multipart/request-POST-android.bytes")
      request.uri = "/multipartParamsPutEcho"
      request.method = HttpMethod.PUT

      server.httpRequest(
        request = request,
        suppress = true,
        andExpect = Ok,
        withJsonBody = """["banner"]""")
    }

    "formPostView" in {
      server.httpFormPost(
        "/formPostView",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "getView" in {
      server.httpGet(
        "/getView?age=18&name=bob",
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "formPostViewFromBuilderViewWithDiffTemplateThanAnnotation" in {
      server.httpFormPost(
        "/formPostViewFromBuilderView",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "age2:18\nname2:bob\nuser1\nuser2\n")
    }

    "formPostViewFromBuilderHtml" in {
      server.httpFormPost(
        "/formPostViewFromBuilderHtml",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "formPostViewFromBuilderCreatedView" in {
      val response = server.httpFormPost(
        "/formPostViewFromBuilderCreatedView",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Created,
        withBody = "age2:18\nname2:bob\nuser1\nuser2\n")

      response.location should equal(Some("/foo/1"))
    }

    "formPostViewFromBuilderCreatedHtml" in {
      val response = server.httpFormPost(
        "/formPostViewFromBuilderCreatedHtml",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Created,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")

      response.location should equal(Some("/foo/1"))
    }

    "post user with injected group_id from route param" in {
      server.httpPost(
        "/groups/123/users",
        postBody =
          """
          {
            "name" : "Bob"
          }
          """",
        andExpect = Created,
        withJsonBody =
          """
            {
              "group_id":123,
              "name":"Bob"
            }
          """)
    }

    "null" in {
      pending
      server.httpGet(
        "/null",
        andExpect = Ok,
        withBody = "")
    }

    "empty" in {
      server.httpGet(
        "/empty",
        andExpect = Ok,
        withBody = "")
    }

    "unit" in {
      server.httpGet(
        "/unit",
        andExpect = Ok,
        withBody = "")
    }

    "not found path" in {
      server.httpGet(
        "/sdafasdfsadfsadfsafd",
        andExpect = NotFound,
        withBody = "")
    }

    "complex path" in {
      server.httpGet(
        "/complexpath/steve",
        andExpect = Ok,
        withBody = "done steve 5000")
    }

    "complex query" in {
      server.httpGet(
        "/complexquery?name=fred",
        andExpect = Ok,
        withBody = "done fred 5000")
    }

    "testfile" in {
      server.httpGet(
        "/testfile",
        andExpect = Ok,
        withBody = "testfile123")
    }

    "testfile when not found" in {
      server.httpGet(
        "/testfileWhenNotfound",
        andExpect = NotFound,
        withBody = "/doesntexist.txt not found")
    }

    "exception" in {
      server.httpGet(
        "/exception",
        andExpect = InternalServerError)
    }

    "pathUrl" in {
      val request = server.httpGet(
        "/pathUrl",
        andExpect = Ok)

      request.contentString should endWith("/pathUrl/")
    }

    "path" in {
      val request = server.httpGet(
        "/path",
        andExpect = Ok)

      request.contentString should endWith("/path/")
    }

    "get to put" in {
      server.httpGet(
        "/put",
        andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
    }

    "put to put" in {
      server.httpPut(
        "/put",
        putBody = "asdf",
        andExpect = Ok,
        withBody = "asdf")
    }

    "post to putAndPost" in {
      server.httpPost(
        "/putAndPost",
        postBody = "1",
        andExpect = Ok,
        withBody = "POST1")
    }

    "put to putAndPost" in {
      server.httpPut(
        "/putAndPost",
        putBody = "2",
        andExpect = Ok,
        withBody = "PUT2")
    }

    "get to putAndPost" in {
      server.httpGet(
        "/putAndPost",
        andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
    }

    "post to postAndPut" in {
      server.httpPost(
        "/postAndPut",
        postBody = "1",
        andExpect = Ok,
        withBody = "POST1")
    }

    "put to postAndPut" in {
      server.httpPut(
        "/postAndPut",
        putBody = "2",
        andExpect = Ok,
        withBody = "PUT2")
    }

    "get to postAndPut" in {
      server.httpGet(
        "/postAndPut",
        andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
    }

    "true" in {
      server.httpGet(
        "/true",
        andExpect = Ok,
        withBody = "true")
    }

    "index root" in {
      server.httpGet(
        "/index/",
        andExpect = Ok,
        withBody = "testindex")
    }

    "index file without extension" in {
      server.httpGet(
        "/index/testfile",
        andExpect = Ok,
        withBody = "testindex")
    }

    "index file with extension" in {
      server.httpGet(
        "/index/testfile.txt",
        andExpect = Ok,
        withBody = "testfile123")
    }

    "implicitOkAndException when ok" in {
      server.httpGet(
        "/implicitOkAndException?hi",
        andExpect = Ok)
    }

    "implicitOkAndException when bad request exception" in {
      server.httpGet(
        "/implicitOkAndException",
        andExpect = BadRequest)
    }

    "slow" in {
      pending // manually run to test fix for go/jira/CSL-565
      server.httpGet(
        "/slow",
        andExpect = Ok)
    }

    "response builder" in {
      val response = server.httpGet(
        "/builderCreatedWithHeader",
        andExpect = Created,
        withLocation = "http://foo.com/1")

      response.headers().get("a") should equal("b")
    }

    "request injection" in {
      server.httpGet(
        "/requestInjection?id=5&id2=6&id4=7",
        andExpect = Ok,
        withBody = "18")
    }

    "request injections not found" in {
      server.httpGet(
        "/requestInjectionsNotFound",
        andExpect = InternalServerError,
        withErrors = Seq(
          "internal server error"))
    }

    "GET request injections not available" in {
      server.httpGet(
        "/requestInjectionsNotAvailable",
        andExpect = InternalServerError,
        withErrors = Seq(
          "internal server error"))
    }

    "POST request injections not available" in {
      server.httpPost(
        "/requestInjectionsNotAvailable",
        "{}",
        andExpect = InternalServerError,
        withErrors = Seq(
          "internal server error"))
    }

    "POST empty json request injections not available" in {
      server.httpPost(
        "/requestInjectionsNotAvailable",
        "",
        andExpect = InternalServerError,
        withErrors = Seq(
          "internal server error"))
    }

    "POST invalid json request injections not available" in {
      server.httpPost(
        "/requestInjectionsNotAvailable",
        "{abc",
        andExpect = BadRequest,
        withErrors = Seq(
          "Unexpected character ('a' (code 97)): was expecting double-quote to start field name"))
    }

    "GET json user" in {
      val response = server.httpGet(
        "/users/mary",
        andExpect = Ok,
        withJsonBody = """{ "name" : "mary" }""")

      response.headerMap("content-type") should equal(JSON_UTF_8.toString)
    }

    "POST json user" in {
      server.httpPost(
        "/users",
        """
          {
            "name" : "bob"
          }
        """,
        andExpect = Ok,
        withBody = "bob")
    }

    "POST json user with missing required field" in {
      server.httpPost(
        "/users",
        """
          {
          }
        """,
        andExpect = BadRequest,
        withErrors = Seq("name is a required field"))
    }

    "POST json user with failed field validation" in {
      server.httpPost(
        "/users",
        """
          {
            "name": "a"
          }
        """,
        andExpect = BadRequest,
        withErrors = Seq("name size [1] is not between 2 and 20"))
    }

    "POST json user with failed method validation" in {
      server.httpPost(
        "/users",
        """
          {
            "name": "foo"
          }
        """,
        andExpect = BadRequest,
        withErrors = Seq("name cannot be foo"))
    }

    "POST json user with invalid field validation" in {
      server.httpPost(
        "/userWithInvalidFieldValidation",
        """
          {
            "name": "a"
          }
        """,
        andExpect = InternalServerError,
        withErrors = Seq("internal server error"))
    }

    "POST json user with invalid method validation" in {
      server.httpPost(
        "/userWithInvalidMethodValidation",
        """
          {
            "name": "foo"
          }
        """,
        andExpect = InternalServerError,
        withErrors = Seq("internal server error"))
    }

    "POST json user with invalid content type" in {
      server.httpPost(
        "/users",
        """
          {
            "name" : "bob"
          }
        """,
        contentType = "foo",
        andExpect = BadRequest)
    }

    "POST json user with missing required field when message body reader uses intermediate JsonNode" in {
      pending //IllegalArgumentException (ObjectMapper.java:2774)
      server.httpPost(
        "/userWithMessageBodyReader",
        """
          {
          }
        """,
        andExpect = BadRequest,
        withErrors = Seq("name is a required field"))
    }

    "POST json user with method validation error when message body reader uses intermediate JsonNode" in {
      pending //IllegalArgumentException (ObjectMapper.java:2774)
      server.httpPost(
        "/userWithMessageBodyReader",
        """
          {
            "name": "foo"
          }
        """,
        andExpect = BadRequest,
        withErrors = Seq("name cannot be foo"))
    }

    "injector test" in {
      server.injector.instance[String]("example") should equal("named")

      val exampleService = server.injector.instance[DoEverythingService]
      exampleService should not equal (null)
      server.injector.instance(classOf[DoEverythingService]) should equal(exampleService)

      val key = Key.get(new TypeLiteral[DoEverythingService]() {})
      server.injector.instance(key) should equal(exampleService)
    }

    "array" in {
      server.httpGet(
        "/array",
        andExpect = Ok,
        withJsonBody = """["a", "b"]""")
    }

    "set" in {
      server.httpGet(
        "/set",
        andExpect = Ok,
        withJsonBody = """["a", "b"]""")
    }

    "seq" in {
      server.httpGet(
        "/seq",
        andExpect = Ok,
        withJsonBody = """["a", "b"]""")
    }

    "delete" in {
      server.httpDelete(
        "/delete",
        andExpect = Ok,
        withBody = "delete")
    }

    "options" in {
      server.httpOptions(
        "/options",
        andExpect = Ok,
        withBody = "options")
    }

    "head" in {
      server.httpHead(
        "/head",
        andExpect = Conflict,
        withBody = "") //HEAD requests cannot have bodies
    }

    "patch" in {
      server.httpPatch(
        "/patch",
        andExpect = Ok,
        withBody = "patch")
    }

    "non guice controller" in {
      server.httpGet(
        "/NonGuice",
        andExpect = Ok,
        withBody = "pong")
    }

    "GET with query parameters as string sequence" in {
      server.httpGet(
        "/RequestWithQueryParamSeqString?foo=1&foo=2&foo=3",
        andExpect = Ok,
        withJsonBody =
          """
            |{ "foo": ["11", "21", "31"] }
          """.stripMargin
      )
    }

    "GET with query parameters as long sequence" in {
      server.httpGet(
        "/RequestWithQueryParamSeqLong?foo=1&foo=2&foo=3",
        andExpect = Ok,
        withJsonBody =
          """
            |{ "foo": [2, 3, 4] }
          """.stripMargin
      )
    }
  }

  "HttpResponseException" in {
    server.httpGet(
      "/HttpResponseException",
      andExpect = Conflict,
      withBody = "conflicted")
  }

  "toFutureException" in {
    server.httpGet(
      "/toFutureException",
      andExpect = Conflict,
      withBody = "conflicted")
  }

  "HttpExceptionPlain" in {
    server.httpGet(
      "/HttpExceptionPlain",
      andExpect = Created,
      withBody = "foo")
  }

  "HttpExceptionErrors" in {
    server.httpGet(
      "/HttpExceptionErrors",
      andExpect = Created,
      withJsonBody =
        """
        {
          "errors" : [ "foo1", "foo2" ]
        }
        """)
  }

  "NotFoundException" in {
    server.httpGet(
      "/NotFoundException",
      andExpect = NotFound,
      withJsonBody =
        """
        {
          "errors" : [ "foo1" ]
        }
        """)
  }

  "ConflictException" in {
    server.httpGet(
      "/ConflictException",
      andExpect = Conflict,
      withJsonBody =
        """
        {
          "errors" : [ "foo1" ]
        }
        """)
  }

  "InternalServerErrorExceptionPlain" in {
    server.httpGet(
      "/InternalServerErrorExceptionPlain",
      andExpect = InternalServerError,
      withBody = "foo1")
  }

  "NotAcceptableException" in {
    server.httpGet(
      "/NotAcceptableException",
      andExpect = NotAcceptable,
      withJsonBody =
        """
        {
          "errors" : [ "foo1" ]
        }
        """)
  }

  "Unserializable class field" in {
    server.httpGet(
      "/UnserializableClassField",
      andExpect = InternalServerError,
      withJsonBody =
        """
        {
          "errors" : [ "internal server error" ]
        }
        """)
  }

  "FooException" in {
    val response = server.httpGet(
      "/FooException/42",
      andExpect = Forbidden,
      withBody = "foo")
    response.headerMap("Foo-ID") should equal("42")
  }

  "BarException" in {
    val response = server.httpGet(
      "/BarException",
      andExpect = Unauthorized,
      withBody = "bar")
    response.headerMap.contains("Foo-ID") should equal(false)
    response.headerMap("Bar-ID") should equal("123")
  }

  "BazException" in {
    val response = server.httpGet(
      "/BazException",
      andExpect = Forbidden,
      withBody = "foo")
    response.headerMap("Foo-ID") should equal("321")
  }

  "NoSuchMethodException" in {
    server.httpGet(
      "/NoSuchMethodException",
      andExpect = InternalServerError)
  }

  "UsersRequest" in {
    server.httpGet(
      path = "/users?start_date=2013&max=10&verbose=true",
      andExpect = Ok,
      withJsonBody = """
      {
        "start_date": "2013-01-01T00:00:00.000Z",
        "max": 10,
        "verbose": true
      }
                     """)

    server.httpGet(
      path = "/users?max=10",
      andExpect = Ok,
      withJsonBody = """
      {
        "max": 10,
        "verbose": false
      }
                     """)

    server.httpGet(
      path = "/users?max=10&verbose=true",
      andExpect = Ok,
      withJsonBody = """
      {
        "max": 10,
        "verbose": true
      }
                     """)

    server.httpGet(
      path = "/users?verbose=5",
      andExpect = BadRequest,
      withJsonBody = """
      {
        "errors": [
          "max is a required field",
          "verbose's value '5' is not a valid boolean"
        ]
      }
                     """)
  }

  "per-route stats" in {
    server.httpGet(
      "/ok",
      andExpect = Ok)

    server.httpPost(
      "/foo",
      postBody = "",
      andExpect = Ok)

    // global stats
    // compatible with com.twitter.finagle.http.filter.StatsFilter
    counter("status/200")              should be(2)
    counter("status/2XX")              should be(2)
       stat("response_size")           should contain inOrder(2.0, 3.0)
       stat("time/200")                should have size(2)
       stat("time/2XX")                should have size(2)

    // don't record stats that finagle already has
    counter("requests")                should be(0)
       stat("time")                    should be('empty)
    counter("http/requests")           should be(2)
       stat("http/request_latency_ms") should have size(2)

    // per-route stats
    counter("route/do-everything/GET/requests")      should be(1)
    counter("route/do-everything/GET/status/200")    should be(1)
    counter("route/do-everything/GET/status/2XX")    should be(1)
       stat("route/do-everything/GET/response_size") should contain(2.0)
       stat("route/do-everything/GET/time")      should have size(1)
       stat("route/do-everything/GET/time/200")      should have size(1)
       stat("route/do-everything/GET/time/2XX")      should have size(1)

    counter("route/do-everything/POST/requests")      should be(1)
    counter("route/do-everything/POST/status/200")    should be(1)
    counter("route/do-everything/POST/status/2XX")    should be(1)
       stat("route/do-everything/POST/response_size") should contain(3.0)
       stat("route/do-everything/POST/time")      should have size(1)
       stat("route/do-everything/POST/time/200")      should have size(1)
       stat("route/do-everything/POST/time/2XX")      should have size(1)
  }

  "ports" in {
    server.twitterServer.httpExternalPort should not be None
    server.twitterServer.httpExternalPort.get should be > 0

    server.twitterServer.httpExternalSocketAddress should not be None

    // no https server configured
    server.twitterServer.httpsExternalPort shouldBe None

    // not a thrift server so, port should be None
    server.twitterServer.thriftPort shouldBe None
  }
}
