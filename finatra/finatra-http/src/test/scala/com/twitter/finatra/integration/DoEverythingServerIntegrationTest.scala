package com.twitter.finatra.integration

import com.google.common.net.MediaType.JSON_UTF_8
import com.google.inject.{Key, TypeLiteral}
import com.twitter.finagle.http.Status._
import com.twitter.finatra.integration.internal.{DoEverythingServer, DoEverythingService}
import com.twitter.finatra.json.JsonDiff._
import com.twitter.finatra.test.{EmbeddedTwitterServer, HttpTest}

class DoEverythingServerIntegrationTest extends HttpTest {

  val server = EmbeddedTwitterServer(
    extraArgs = Array("-magicNum=1", "-moduleMagicNum=2"),
    twitterServer = new DoEverythingServer)

  val doEverythingService = server.injector.instance[DoEverythingService]
  val namedExampleString = server.injector.instance[String]("example")

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
        multipart = false,
        andExpect = Ok,
        withBody = "bob")
    }

    "formPostView" in {
      server.httpFormPost(
        "/formPostView",
        params = Map("name" -> "bob", "age" -> "18"),
        multipart = false,
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
        multipart = false,
        andExpect = Ok,
        withBody = "age2:18\nname2:bob\nuser1\nuser2\n")
    }

    "formPostViewFromBuilderHtml" in {
      server.httpFormPost(
        "/formPostViewFromBuilderHtml",
        params = Map("name" -> "bob", "age" -> "18"),
        multipart = false,
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "formPostViewFromBuilderCreatedView" in {
      val response = server.httpFormPost(
        "/formPostViewFromBuilderCreatedView",
        params = Map("name" -> "bob", "age" -> "18"),
        multipart = false,
        andExpect = Created,
        withBody = "age2:18\nname2:bob\nuser1\nuser2\n")

      response.location should equal(Some("/foo/1"))
    }

    "formPostViewFromBuilderCreatedHtml" in {
      val response = server.httpFormPost(
        "/formPostViewFromBuilderCreatedHtml",
        params = Map("name" -> "bob", "age" -> "18"),
        multipart = false,
        andExpect = Created,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")

      response.location should equal(Some("/foo/1"))
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
        withBody = "/sdafasdfsadfsadfsafd route not found")
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
      server.httpGet(
        "/pathUrl",
        andExpect = Ok,
        withBody = "http://localhost.twitter.com/pathUrl/")
    }

    "path" in {
      server.httpGet(
        "/path",
        andExpect = Ok,
        withBody = "http://localhost.twitter.com/path/")
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
        andExpect = Ok, //TODO: Should be 405 Method Not Allowed
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

    "finatra request" in {
      server.httpGet(
        "/finatraRequest",
        andExpect = Ok)
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
        andExpect = Ok,
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
        "/legacy",
        andExpect = Ok,
        withBody = "pong")
    }
  }
}
