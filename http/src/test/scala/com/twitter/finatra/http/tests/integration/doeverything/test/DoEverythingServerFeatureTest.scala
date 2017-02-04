package com.twitter.finatra.http.tests.integration.doeverything.test

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.net.MediaType
import com.google.inject.{Key, TypeLiteral}
import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.integration.doeverything.main.DoEverythingServer
import com.twitter.finatra.http.tests.integration.doeverything.main.domain.SomethingStreamedResponse
import com.twitter.finatra.http.tests.integration.doeverything.main.services.DoEverythingService
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.finatra.json.JsonDiff._
import com.twitter.inject.server.WordSpecFeatureTest
import com.twitter.io.Buf
import org.apache.commons.io.IOUtils
import org.scalatest.exceptions.TestFailedException

class DoEverythingServerFeatureTest extends WordSpecFeatureTest {

  override val server = new EmbeddedHttpServer(
    args = Array("-magicNum=1", "-moduleMagicNum=2"),
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

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    server.inMemoryStatsReceiver.clear()
  }

  "DoEverythingServer" should {
    "named string" in {
      namedExampleString should equal("named")
    }

    /* Admin routes */

    "GET /admin/finatra/foo" in {
      // on the admin
      server.httpGet(
        "/admin/finatra/foo",
        routeToAdminServer = true,
        andExpect = Status.Ok,
        withBody = "on the admin interface")
      // on the admin and will be routed even if routeToAdminServer flag isn't set because route starts with /admin/finatra
      server.httpGet(
        "/admin/finatra/foo",
        andExpect = Status.Ok,
        withBody = "on the admin interface")
    }

    "GET /admin/finatra/implied" in {
      // on the admin
      server.httpGet(
        "/admin/finatra/implied",
        routeToAdminServer = true,
        andExpect = Status.Ok,
        withBody = "on the admin interface")
      // on the admin and will be routed even if routeToAdminServer flag isn't set because route starts with /admin/finatra
      server.httpGet(
        "/admin/finatra/implied",
        andExpect = Status.Ok,
        withBody = "on the admin interface")
    }

    "GET /admin/bar" in {
      // on the admin
      server.httpGet(
        "/admin/bar",
        routeToAdminServer = true,
        andExpect = Status.Ok,
        withBody = "on the admin interface")
      // on the admin and will be routed even if routeToAdminServer flag isn't set as it's in the list of admin http routes on the server
      server.httpGet(
        "/admin/bar",
        andExpect = Status.Ok,
        withBody = "on the admin interface")
    }

    "GET /admin/foo" in {
      // NOT on the admin
      server.httpGet(
        "/admin/foo",
        routeToAdminServer = true,
        andExpect = Status.NotFound)
      // on the external
      server.httpGet(
        "/admin/foo",
        routeToAdminServer = false,
        andExpect = Status.Ok,
        withBody = "on the external interface")
    }

    "GET /admin/external/filtered" in {
      // NOT on the admin
      server.httpGet(
        "/admin/external/filtered",
        routeToAdminServer = true,
        andExpect = Status.NotFound)
      // on the external and reads headers appended by external filters
      server.httpGet(
        "/admin/external/filtered",
        routeToAdminServer = false,
        andExpect = Ok,
        withBody = "01")
    }

    "respond to /example" in {
      server.httpGet(
        "/example/routing/always",
        withBody = "always response")

      server.httpGet(
        "/example/routing/always2",
        withBody = "always response")
    }

    "GET /plaintext" in {
      val response1 = server.httpGet(
        "/plaintext",
        withBody = "Hello, World!")

      response1.contentType should equal(Some(MediaType.PLAIN_TEXT_UTF_8.toString))

      val response2 = server.httpGet(
        "/plaintext/",
        withBody = "Hello, World!")

      response2.contentType should equal(Some(MediaType.PLAIN_TEXT_UTF_8.toString))
    }

    "/plaintext (prefixed)" in {
      val response = server.httpGet(
          "/1.1/plaintext",
          withBody = "Hello, World!")

      response.contentType should equal(Some(MediaType.PLAIN_TEXT_UTF_8.toString))
    }

    "GET /bytearray" in {
      val response = server.httpGet(
        "/bytearray")

      response.contentType should equal(Some(MediaType.OCTET_STREAM.toString))
    }

    "GET /inputstream" in {
      val response = server.httpGet(
        "/inputstream")

      response.contentType should equal(Some(MediaType.OCTET_STREAM.toString))
    }

    "GET /useragent" in {
      server.httpGet(
        "/useragent",
        headers = Map("User-Agent" -> "Firefox"),
        withBody = "Firefox")
    }

    "response should contain server/date headers" in {
      val response = server.httpGet("/example/routing/always")
      response.statusCode should equal(200)
      response.contentString should equal("always response")

      response.headerMap.get("Date") should not be empty
      response.headerMap.get("Server") should not be empty
    }

    "json response to /example" in {
      val response = server.httpGet("/example/routing/json/1")
      response.statusCode should equal(200)
      jsonDiff(response.contentString, """{"id":"1","name":"bob","magic":"1","module_magic":"2"}""")
    }

    "GET /stringMap" in {
      server.httpGet(
        "/stringMap",
        andExpect = Ok,
        withJsonBody = """{"message":"Hello, World!"}""")
    }

    "GET /ok" in {
      server.httpGet(
        "/ok",
        andExpect = Ok)
    }

    "GET /created" in {
      server.httpGet(
        "/created",
        andExpect = Created)
    }

    "respond to /accepted" in {
      server.httpGet(
        "/accepted",
        andExpect = Accepted,
        withBody = "accepted")

      server.httpGet(
        "/accepted2",
        andExpect = Accepted,
        withBody = "accepted")
    }

    "GET /json" in {
      server.httpGet(
        "/json",
        withJsonBody = "{}")
    }

    "GET /json2" in {
      server.httpGet(
        "/json2",
        withJsonBody = "{}")
    }

    "GET /none" in {
      server.httpGet(
        "/none",
        withBody = "")
    }

    "GET /bodyunit" in {
      server.httpGet(
        "/bodyunit",
        withBody = "")
    }

    "GET /bodynull" in {
      server.httpGet(
        "/bodynull",
        withBody = "")
    }

    "GET /bodyEmptyString" in {
      server.httpGet(
        "/bodyEmptyString",
        withBody = "")
    }

    "GET /routeParamGetAll/:id" in {
      server.httpGet(
        "/routeParamGetAll/1",
        withJsonBody = """
        [
          "1",
          "1"
        ]
        """)
    }

    "GET /notfound" in {
      server.httpGet(
        "/notfound",
        andExpect = NotFound)
    }

    "GET /notfound2" in {
      server.httpGet(
        "/notfound2",
        andExpect = NotFound)
    }

    "GET /notfoundexception" in {
      server.httpGet(
        "/notfoundexception",
        andExpect = NotFound)
    }

    "GET /notfoundexception2" in {
      server.httpGet(
        "/notfoundexception2",
        andExpect = NotFound)
    }

    "GET /badrequest" in {
      server.httpGet(
        "/badrequest",
        andExpect = BadRequest)
    }

    "GET /BadRequestException" in {
      server.httpGet(
        "/BadRequestException",
        andExpect = BadRequest)
    }

    "GET /forbidden" in {
      server.httpGet(
        "/forbidden",
        andExpect = Forbidden)
    }

    "GET /ForbiddenException" in {
      server.httpGet(
        "/ForbiddenException",
        andExpect = Forbidden)
    }

    "GET /ForbiddenException2" in {
      server.httpGet(
        "/ForbiddenException2",
        andExpect = Forbidden)
    }

    "GET /methodnotallowed" in {
      server.httpGet(
        "/methodnotallowed",
        andExpect = MethodNotAllowed)
    }

    "GET /unavailable" in {
      server.httpGet(
        "/unavailable",
        andExpect = ServiceUnavailable)
    }

    "GET /unauthorized" in {
      server.httpGet(
        "/unauthorized",
        andExpect = Unauthorized)
    }

    "GET /conflict" in {
      server.httpGet(
        "/conflict",
        andExpect = Conflict)
    }

    "GET /ConflictException" in {
      server.httpGet(
        "/ConflictException",
        andExpect = Conflict)
    }

    "GET /ConflictException2" in {
      server.httpGet(
        "/ConflictException2",
        andExpect = Conflict)
    }

    "GET /ConflictException3" in {
      server.httpGet(
        "/ConflictException3",
        andExpect = Conflict)
    }

    "GET /servererrorexception" in {
      server.httpGet(
        "/servererrorexception",
        andExpect = InternalServerError)
    }

    "GET /serviceunavailableexception" in {
      server.httpGet(
        "/serviceunavailableexception",
        andExpect = ServiceUnavailable)
    }

    "GET /serviceunavailableexception2" in {
      server.httpGet(
        "/serviceunavailableexception2",
        andExpect = ServiceUnavailable)
    }

    "GET /responsebuilder_status_code" in {
      server.httpGet(
        "/responsebuilder_status_code",
        andExpect = ServiceUnavailable)
    }

    "GET /responsebuilder_status" in {
      server.httpGet(
        "/responsebuilder_status",
        andExpect = ServiceUnavailable)
    }

    "GET /redirect" in {
      server.httpGet(
        "/redirect",
        andExpect = TemporaryRedirect)
    }

    "GET /found" in {
      server.httpGet(
        "/found",
        andExpect = Found)
    }

    "GET /future" in {
      server.httpGet(
        "/future",
        andExpect = Ok,
        withBody = "future")
    }

    "POST /foo" in {
      server.httpPost(
        "/foo",
        postBody = "",
        andExpect = Ok,
        withBody = "bar")
    }

    "POST /foo (prefixed)" in {
      server.httpPost(
        "/1.1/foo",
        postBody = "",
        andExpect = Ok,
        withBody = "bar")
    }

    "POST /formPost" in {
      server.httpFormPost(
        "/formPost",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "bob")
    }

    "POST /multipartParamsEcho" in {
      val request = deserializeRequest("/multipart/request-POST-android.bytes")
      request.uri = "/multipartParamsEcho"

      server.httpRequest(
        request = request,
        suppress = true,
        andExpect = Ok,
        withJsonBody = """["banner"]""")
    }

    "POST /formPostMultipart" in {
      server.httpMultipartFormPost(
        "/formPostMultipart",
        params = Seq(
          FileElement(
            "file",
            Buf.ByteArray.Owned("hi".getBytes()),
            Some("text/plain"),
            Some("hi.txt"))),
        andExpect = Ok,
        withBody = "text/plain")
    }

    "PUT /multipartParamsPutEcho" in {
      val request = deserializeRequest("/multipart/request-POST-android.bytes")
      request.uri = "/multipartParamsPutEcho"
      request.method = Method.Put

      server.httpRequest(
        request = request,
        suppress = true,
        andExpect = Ok,
        withJsonBody = """["banner"]""")
    }

    "POST /formPostView" in {
      server.httpFormPost(
        "/formPostView",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "GET /getView" in {
      server.httpGet(
        "/getView?age=18&name=bob",
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "POST /formPostViewFromBuilderView (from BuilderView with diff template than annotation)" in {
      server.httpFormPost(
        "/formPostViewFromBuilderView",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "age2:18\nname2:bob\nuser1\nuser2\n")
    }

    "POST /formPostViewFromBuilderHtml" in {
      server.httpFormPost(
        "/formPostViewFromBuilderHtml",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "POST /formPostViewFromBuilderCreatedView" in {
      val response = server.httpFormPost(
        "/formPostViewFromBuilderCreatedView",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Created,
        withBody = "age2:18\nname2:bob\nuser1\nuser2\n")

      response.location should equal(Some("/foo/1"))
    }

    "POST /formPostViewFromBuilderCreatedHtml" in {
      val response = server.httpFormPost(
        "/formPostViewFromBuilderCreatedHtml",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Created,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")

      response.location should equal(Some("/foo/1"))
    }

    "POST user with injected group_id from route param" in {
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

    "POST /multipleRouteParams" in {
      server.httpPost(
        "/multipleRouteParams",
        "",
        andExpect = InternalServerError)
    }

    "POST /caseClassWithRequestField" in {
      server.httpPost(
        "/caseClassWithRequestField",
        "",
        andExpect = Ok)
    }

    "GET /null" in {
      server.httpGet(
        "/null",
        andExpect = Ok,
        withBody = "")
    }

    "GET /empty" in {
      server.httpGet(
        "/empty",
        andExpect = Ok,
        withBody = "")
    }

    "GET /unit" in {
      val response = server.httpGet(
        "/unit",
        andExpect = Ok,
        withBody = "")

      response.contentLength should equal(Some(0))
      // no content-type as there is no content-body
      response.contentType should equal(None)
    }

    "GET not found path" in {
      server.httpGet(
        "/sdafasdfsadfsadfsafd",
        andExpect = NotFound,
        withBody = "")
    }

    "GET complex path" in {
      server.httpGet(
        "/complexpath/steve",
        andExpect = Ok,
        withBody = "done steve 5000")
    }

    "GET complex query" in {
      server.httpGet(
        "/complexquery?name=fred",
        andExpect = Ok,
        withBody = "done fred 5000")
    }

    "GET /testfile" in {
      server.httpGet(
        "/testfile",
        andExpect = Ok,
        withBody = "testfile123")
    }

    "GET /testfileWhenNotfound" in {
      server.httpGet(
        "/testfileWhenNotfound",
        andExpect = NotFound,
        withBody = "/doesntexist.txt not found")
    }

    "GET /exception" in {
      server.httpGet(
        "/exception",
        andExpect = InternalServerError)
    }

    "GET /pathUrl" in {
      val request = server.httpGet(
        "/pathUrl",
        andExpect = Ok)

      request.contentString should endWith("/pathUrl/")
    }

    "GET /path" in {
      val request = server.httpGet(
        "/path",
        andExpect = Ok)

      request.contentString should endWith("/path/")
    }

    "GET /put (NotFound)" in {
      server.httpGet(
        "/put",
        andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
    }

    "putJson" in {
      server.httpPutJson[JsonNode](
        "/putJson/123",
        putBody = """{"name": "Steve"}""",
        andExpect = Ok,
        withJsonBody = """{"id": 123, "name": "Steve"}""")
    }

    "putJson without type param" in {
      val e = intercept[TestFailedException] {
        server.httpPutJson(
          "/putJson/123",
          putBody = """{"name": "Steve"}""",
          andExpect = Ok,
          withJsonBody = """{"id": 123, "name": "Steve"}""")
      }
      e.getMessage() should include("requires a type-param")
    }

    "PUT body with multi-byte characters" in {
      val musicalNote = "\uD83C\uDFB5"
      val body = s"${musicalNote} ${musicalNote}"
      server.httpPut(
        "/echo",
        putBody = body,
        contentType = MediaType.PLAIN_TEXT_UTF_8.toString,
        andExpect = Ok,
        withBody = body
      )
    }

    "PUT" in {
      server.httpPut(
        "/put/123",
        putBody = "asdf",
        andExpect = Ok,
        withBody = "123_asdf")
    }

    "PUT with RouteParam and non-json body" in {
      server.httpPut(
        "/put_route_param/123",
        contentType = "plain/text",
        putBody = "asdf",
        andExpect = Ok,
        withBody = "123_asdf")
    }

    "PUT with RouteParam and json body" in {
      server.httpPut(
        "/put_route_param/123",
        putBody = "{}",
        andExpect = Ok,
        withBody = "123_")
    }

    "PUT with RouteParam and empty body" in {
      server.httpPut(
        "/put_route_param/123",
        putBody = "",
        andExpect = Ok,
        withBody = "123_")
    }

    "PUT /put_route_param_and_name" in {
      server.httpPut(
        "/put_route_param_and_name/123",
        putBody = """{"name": "bob"}""",
        andExpect = Ok,
        withBody = "123_bob")
    }

    "PUT /put_route_param_and_name (with empty PUT body)" in {
      server.httpPut(
        "/put_route_param_and_name/123",
        putBody = "",
        andExpect = BadRequest,
        withJsonBody = """
        {
          "errors" : [
            "name: field is required"
          ]
        }""")
    }

    "PUT /put_route_param_and_name (with non-json PUT body)" in {
      server.httpPut(
        "/put_route_param_and_name/123",
        putBody = "foo",
        contentType = "plain/text",
        andExpect = BadRequest,
        withJsonBody = """
        {
          "errors" : [
            "name: field is required"
          ]
        }""")
    }

    "PUT /put_id_ignoring_body" in {
      val response = server.httpPut(
        "/put_id_ignoring_body/42",
        putBody = "invalid JSON",
        andExpect = Ok,
        withJsonBody = "42")

      response.contentType should equal(Some(MediaType.PLAIN_TEXT_UTF_8.toString))
    }

    "PUT /put_id_not_ignoring_body" in {
      server.httpPut(
        "/put_id_not_ignoring_body/42",
        putBody = "invalid JSON",
        andExpect = BadRequest,
        withJsonBody = """
        {
          "errors":[
            "Unrecognized token 'invalid': was expecting ('true', 'false' or 'null')"
          ]
        }""")
    }

    "POST /putAndPost" in {
      server.httpPost(
        "/putAndPost",
        postBody = "1",
        andExpect = Ok,
        withBody = "POST1")
    }

    "PUT /putAndPost" in {
      server.httpPut(
        "/putAndPost",
        putBody = "2",
        andExpect = Ok,
        withBody = "PUT2")
    }

    "GET /putAndPost (NotFound)" in {
      server.httpGet(
        "/putAndPost",
        andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
    }

    "POST /postAndPut" in {
      server.httpPost(
        "/postAndPut",
        postBody = "1",
        andExpect = Ok,
        withBody = "POST1")
    }

    "PUT /postAndPut" in {
      server.httpPut(
        "/postAndPut",
        putBody = "2",
        andExpect = Ok,
        withBody = "PUT2")
    }

    "GET /postAndPut (NotFound)" in {
      server.httpGet(
        "/postAndPut",
        andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
    }

    "GET /true" in {
      server.httpGet(
        "/true",
        andExpect = Ok,
        withBody = "true")
    }

    "GET /index (root)" in {
      server.httpGet(
        "/index/",
        andExpect = Ok,
        withBody = "testindex")
    }

    "GET index file without extension" in {
      server.httpGet(
        "/index/testfile",
        andExpect = Ok,
        withBody = "testindex")
    }

    "GET index file with extension" in {
      server.httpGet(
        "/index/testfile.txt",
        andExpect = Ok,
        withBody = "testfile123")
    }

    "GET /implicitOkAndException (when ok)" in {
      server.httpGet(
        "/implicitOkAndException?hi",
        andExpect = Ok)
    }

    "GET /implicitOkAndException (when bad request exception)" in {
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

      response.headerMap.get("a") should equal(Some("b"))
    }

    "request injection" in {
      server.httpGet(
        "/requestInjection?id=5&id2=6&id3&id4=7",
        andExpect = Ok,
        withJsonBody = """
        {
          "id" : 5,
          "id2" : 6,
          "id4" : 7,
          "default_string" : "default string",
          "default_prod_string" : "prod string",
          "default_opt_string" : "default option string",
          "default_opt_prod_string" : "prod option string"
        }""")
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

      response.headerMap("content-type") should equal(MediaType.JSON_UTF_8.toString)
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
        withErrors = Seq("name: field is required"))
    }

    "POST body with multi-byte characters" in {
      val musicalNote = "\uD83C\uDFB5"
      val body = s"${musicalNote} ${musicalNote}"
      server.httpPost(
        "/echo",
        postBody = body,
        contentType = MediaType.PLAIN_TEXT_UTF_8.toString,
        andExpect = Ok,
        withBody = body
      )
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
        withErrors = Seq("name: size [1] is not between 2 and 20"))
    }

    "POST json user with null required field" in {
      server.httpPost(
        "/users",
        """
          {
            "name": null
          }
        """,
        andExpect = BadRequest,
        withErrors = Seq("name: field is required"))
    }

    "POST json with failed array element validation" in {
      server.httpPost(
        "/arrayElementValidation",
        """
          {
            "seq": [0]
          }
        """,
        andExpect = BadRequest,
        withErrors = Seq("seq.value: [0] is not greater than or equal to 1"))
    }

    "POST json with null array element" in {
      server.httpPost(
        "/arrayElementValidation",
        """
          {
            "seq": [null]
          }
        """,
        andExpect = BadRequest,
        withErrors = Seq("seq: Literal null values are not allowed as json array elements."))
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
      exampleService should not equal null
      server.injector.instance(classOf[DoEverythingService]) should equal(exampleService)

      val key = Key.get(new TypeLiteral[DoEverythingService]() {})
      server.injector.instance(key) should equal(exampleService)
    }

    "GET /array" in {
      server.httpGet(
        "/array",
        andExpect = Ok,
        withJsonBody = """["a", "b"]""")
    }

    "GET /set" in {
      server.httpGet(
        "/set",
        andExpect = Ok,
        withJsonBody = """["a", "b"]""")
    }

    "GET /seq" in {
      server.httpGet(
        "/seq",
        andExpect = Ok,
        withJsonBody = """["a", "b"]""")
    }

    "DELETE" in {
      server.httpDelete(
        "/delete",
        andExpect = Ok,
        withBody = "delete")
    }

    "DELETE with body" in {
      server.httpDelete(
        "/delete",
        deleteBody = "DELETE BODY",
        contentType = MediaType.PLAIN_TEXT_UTF_8.toString,
        andExpect = Ok,
        withBody = "delete")
    }

    "DELETE with JSON body" in {
      server.httpDelete(
        "/delete",
        deleteBody = "{\"id\": \"11211\"}",
        andExpect = Ok,
        withBody = "delete")
    }

    "OPTIONS" in {
      server.httpOptions(
        "/options",
        andExpect = Ok,
        withBody = "options")
    }

    "HEAD" in {
      server.httpHead(
        "/head",
        andExpect = Conflict,
        withBody = "") //HEAD responses cannot have bodies
    }

    "PATCH" in {
      server.httpPatch(
        "/patch",
        contentType = MediaType.PLAIN_TEXT_UTF_8.toString,
        patchBody = "asdf",
        andExpect = Ok,
        withBody = "patch")
    }

    "PATCH with JSON body" in {
      server.httpPatch(
        "/patch",
        patchBody = "{\"id\": \"11211\"}", // note: this is not json-patch (RFC6902), just PATCH with a JSON content-type.
        andExpect = Ok,
        withBody = "patch")
    }

    "PATCH body with multi-byte characters" in {
      val musicalNote = "\uD83C\uDFB5"
      val body = s"${musicalNote} ${musicalNote}"
      server.httpPatch(
        "/echo",
        contentType = MediaType.PLAIN_TEXT_UTF_8.toString,
        patchBody = body,
        andExpect = Ok,
        withBody = body)
    }

    "GET /NonGuice" in {
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
            { "foo": ["11", "21", "31"] }
          """
      )
    }

    "GET with query parameters as string sequence with no values (required)" in {
      server.httpGet(
        "/RequestWithQueryParamSeqString",
        andExpect = BadRequest,
        withJsonBody =
          """{
              "errors": [
                "foo: queryParam is required"
              ]
            }"""
      )
    }

    "GET defaulted request case class parameters as string sequence" in {
      server.httpGet(
        "/RequestWithDefaultedQueryParamSeqString",
        andExpect = Ok,
        withJsonBody =
          """
            { "foo": ["foo", "bar", "baz"] }
          """
      )
    }

    "GET defaulted request case class parameters as string sequence with query params" in {
      server.httpGet(
        "/RequestWithDefaultedQueryParamSeqString?foo=override1&foo=override2",
        andExpect = Ok,
        withJsonBody =
          """
            { "foo": ["override1", "override2"] }
          """
      )
    }

    "GET with query parameters as empty string sequence" in {
      server.httpGet(
        "/RequestWithDefaultedQueryParamSeqString?foo=",
        andExpect = Ok,
        withJsonBody =
          """
            { "foo": [""] }
          """
      )
    }

    "GET defaulted request case class parameter as string" in {
      server.httpGet(
        "/RequestWithDefaultQueryParam",
        andExpect = Ok,
        withJsonBody =
          """
            { "param": "default" }
          """
      )
    }

    "GET defaulted request case class parameter as string with query param" in {
      server.httpGet(
        "/RequestWithDefaultQueryParam?param=set",
        andExpect = Ok,
        withJsonBody =
          """
            { "param": "set" }
          """
      )
    }

    "GET with query parameters as long sequence" in {
      server.httpGet(
        "/RequestWithQueryParamSeqLong?foo=1&foo=2&foo=3",
        andExpect = Ok,
        withJsonBody =
          """
            { "foo": [2, 3, 4] }
          """
      )
    }

    "GET with query parameters as empty long sequence" in {
      server.httpGet(
        "/RequestWithQueryParamSeqLong?foo=",
        andExpect = Ok,
        withJsonBody =
          """
            { "foo": [] }
          """
      )
    }

    "apply route filter" in {
      server.httpGet(
        "/forbiddenByFilter",
        andExpect = Forbidden)
    }

    "apply multiple route filters added by type and instance" in {
      server.httpGet(
        "/multiFilterAppend",
        andExpect = Ok,
        withBody = "014")
    }

    "apply multiple route filters added by type" in {
      server.httpGet(
        "/multiIdentityFilterAppend",
        andExpect = Ok,
        withBody = "ok!")
    }

    "apply multiple route filters" in {
      server.httpGet(
        "/multipleRouteFilters",
        andExpect = Ok,
        withBody = "012345")
    }

    "anyMethod GET" in {
      server.httpGet(
        "/anyMethod",
        andExpect = Ok)
    }

    "anyMethod TRACE" in {
      server.httpRequest(
        Request(Method.Trace, "/anyMethod"),
        andExpect = Ok)
    }

    "anyMethod HEAD" in {
      server.httpHead(
        "/anyMethod",
        andExpect = Ok)
    }

    "anyMethod POST" in {
      server.httpPost(
        "/anyMethod",
        postBody = "",
        andExpect = MethodNotAllowed)
    }

    "anyMethod PUT" in {
      server.httpPut(
        "/anyMethod",
        putBody = "",
        andExpect = MethodNotAllowed)
    }

    "anyMethod DELETE" in {
      server.httpDelete(
        "/anyMethod",
        deleteBody = "",
        andExpect = MethodNotAllowed)
    }

    "GET /column/:key//:*" in {
      server.httpGet(
        "/column/foo//bar?baz=quux",
        andExpect = Ok,
        withBody = "foo/bar")
    }

    "POST /SomethingStreamedRequest" in {
      server.httpPost(
        "/SomethingStreamedRequest.json?something_id=FOO&field1=BAR&field2=3",
        postBody = "",
        andExpect = Ok,
        withBody = "FOO/BAR/3")
    }

    "POST /SomethingStreamedRequestAsJsonResponse" in {
      val response = server.httpPostJson[SomethingStreamedResponse](
        "/SomethingStreamedRequestAsJsonResponse.json?something_id=FOO&field1=BAR&field2=3",
        postBody = "",
        andExpect = Ok)
      assert(response.somethingId == "FOO")
      assert(response.field1.get == "BAR")
      assert(response.field2.get == 3)
    }

    "GET /forwarded" in {
      server.httpGet(
        "/forwarded",
        andExpect = Ok,
        withBody =  "This works.")
    }

    "GET /forwardToForbidden" in {
      server.httpGet(
        "/forwardToForbidden",
        andExpect = Forbidden)
    }

    "GET /forwardCaseClass" in {
      server.httpGet(
        "/forwardCaseClass",
        andExpect = Ok,
        withBody =  "This works.")
    }

    "POST /forwarded" in {
      server.httpPost(
        "/forwarded",
        postBody = "",
        andExpect = Ok,
        withBody =  "This works.")
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

  "NotAcceptableException2" in {
    server.httpGet(
      "/NotAcceptableException2",
      andExpect = NotAcceptable)
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

  "FooBarBazException" in {
    val response = server.httpGet(
      "/FooBarBazException",
      andExpect = Forbidden,
      withBody = "foo")
    response.headerMap("Foo-ID") should equal("321-123")
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
          "max: queryParam is required",
          "verbose: '5' is not a valid Boolean"
        ]
      }
      """)
  }

  "CaseClassWithIntQueryParam" in {
    server.httpGet(
      "/CaseClassWithIntQueryParam?param=123",
      andExpect = Ok,
      withJsonBody = "[123]")
  }

  "CaseClassWithShortQueryParam" in {
    server.httpGet(
      "/CaseClassWithShortQueryParam?param=123",
      andExpect = Ok,
      withJsonBody = "[123]")
  }

  "RequestWithBooleanQueryParams" in {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=true",
      andExpect = Ok,
      withJsonBody = "[true]")
  }

  "RequestWithBooleanQueryParams which is a true number" in {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=1",
      andExpect = Ok,
      withJsonBody = "[true]")
  }

  "RequestWithBooleanQueryParams which is a false number" in {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=0",
      andExpect = Ok,
      withJsonBody = "[false]")
  }

  "RequestWithBooleanQueryParam which is a true number" in {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=1",
      andExpect = Ok,
      withJsonBody = "true")
  }

  "RequestWithBooleanQueryParam which is a false number" in {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=0",
      andExpect = Ok,
      withJsonBody = "false")
  }

  "RequestWithBooleanQueryParam which is a true represented as t" in {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=t",
      andExpect = Ok,
      withJsonBody = "true"
    )
  }

  "RequestWithBooleanQueryParam which is a false represented as f" in {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=f",
      andExpect = Ok,
      withJsonBody = "false"
    )
  }

  "RequestWithBooleanQueryParam with incorrect param" in {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=FOO",
      andExpect = BadRequest,
      withJsonBody = """{
        "errors" : [
          "param: 'FOO' is not a valid Boolean"
        ]
      }""")
  }

  "RequestWithBooleanQueryParams with multiple params" in {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=true&param=0&param=t",
      andExpect = Ok,
      withJsonBody = "[true, false, true]")
  }

  "RequestWithBooleanQueryParams with incorrect params" in {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=true&param=FOO",
      andExpect = BadRequest,
      withJsonBody = """{
        "errors" : [
          "param: 'FOO' is not a valid Boolean"
        ]
      }""")
  }

  "RequestWithBooleanQueryParam which is a true" in {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=true",
      andExpect = Ok,
      withJsonBody = "true")
  }

  "RequestWithBooleanQueryParam which is empty" in {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=",
      andExpect = BadRequest,
      withJsonBody = """{
        "errors" : [
          "param: queryParam is required"
        ]
      }""")
  }

  "RequestWithOptionBooleanQueryParam which is a true" in {
    server.httpGet(
      "/RequestWithOptionBooleanQueryParam?param=true",
      andExpect = Ok,
      withJsonBody = "Hi Some(true)")
  }

  "RequestWithOptionBooleanQueryParam which is empty" in {
    server.httpGet(
      "/RequestWithOptionBooleanQueryParam?param=",
      andExpect = Ok,
      withJsonBody = "Hi None")
  }

  "RequestWithOptionBooleanQueryParam which is a false" in {
    server.httpGet(
      "/RequestWithOptionBooleanQueryParam?param=false",
      andExpect = Ok,
      withJsonBody = "Hi Some(false)")
  }

  "RequestWithBooleanQueryParam which is a false" in {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=false",
      andExpect = Ok,
      withJsonBody = "false")
  }

  "RequestWithBooleanQueryParams bad request" in {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=foo",
      andExpect = BadRequest)
  }

  "CaseClassWithCaseClassQueryParam" in {
    server.httpGet(
      "/CaseClassWithCaseClassQueryParam?param=true",
      andExpect = BadRequest)
  }

  "TestCaseClassWithHtml" in {
    server.httpGet(
      "/testClassWithHtml",
      andExpect = Ok,
      withJsonBody =
        """
          |{
          |  "address" : "123 Main St. Anywhere, CA US 90210",
          |  "phone" : "+12221234567",
          |  "rendered_html" : "&lt;div class=&quot;nav&quot;&gt;\n  &lt;table cellpadding=&quot;0&quot; cellspacing=&quot;0&quot;&gt;\n    &lt;tr&gt;\n        &lt;th&gt;Name&lt;/th&gt;\n        &lt;th&gt;Age&lt;/th&gt;\n        &lt;th&gt;Friends&lt;/th&gt;\n    &lt;/tr&gt;\n    &lt;tr&gt;\n        &lt;td&gt;age2:28&lt;/td&gt;\n        &lt;td&gt;name:Bob Smith&lt;/td&gt;\n        &lt;td&gt;\n            user1\n            user2\n        &lt;/td&gt;\n    &lt;/tr&gt;\n  &lt;/table&gt;\n&lt;/div&gt;"
          |}
        """.stripMargin)
  }

  "TRACE" in {
    val request = Request(Trace, "/trace")
    server.httpRequest(
      request,
      andExpect = Ok,
      withBody = "trace 123")
  }

  "bad method" in {
    val request = Request(Method("foo"), "/trace")
    server.httpRequest(
      request,
      andExpect = BadRequest)
  }

  "camelCaseJson" in {
    server.httpGet(
      "/camelCaseJson",
      withJsonBody = """
      {
        "firstName": "Bob"
      }
      """)
  }

  "per-route stats" in {
    server.httpGet(
      "/ok",
      andExpect = Ok)

    server.httpPost(
      "/longer/post/path/foo",
      postBody = "",
      andExpect = Ok)

    server.httpPost(
      "/longer/post/path/with/name/:capture",
      postBody = "",
      andExpect = Ok)

    // global stats
    // compatible with com.twitter.finagle.http.filter.StatsFilter
    counter("status/200") should be(3)
    counter("status/2XX") should be(3)
    stat("response_size") should be(List(2.0, 3.0, 3.0))
    stat("time/200") should have size (3)
    stat("time/2XX") should have size (3)

    // don't record stats that finagle already has
    counter("requests") should be(0)
    stat("time") should be('empty)
    counter("http/requests") should be(3)
    stat("http/request_latency_ms") should have size (3)

    // per-route stats
    counter("route/ok/GET/requests") should be(1)
    counter("route/ok/GET/status/200") should be(1)
    counter("route/ok/GET/status/2XX") should be(1)
    stat("route/ok/GET/response_size") should contain(2.0)
    stat("route/ok/GET/time") should have size (1)
    stat("route/ok/GET/time/200") should have size (1)
    stat("route/ok/GET/time/2XX") should have size (1)

    counter("route/longer_post_path_capture/POST/requests") should be(1)
    counter("route/longer_post_path_capture/POST/status/200") should be(1)
    counter("route/longer_post_path_capture/POST/status/2XX") should be(1)
    stat("route/longer_post_path_capture/POST/response_size") should contain(3.0)
    stat("route/longer_post_path_capture/POST/time") should have size (1)
    stat("route/longer_post_path_capture/POST/time/200") should have size (1)
    stat("route/longer_post_path_capture/POST/time/2XX") should have size (1)

    counter("route/my_cool_endpoint/POST/requests") should be(1)
    counter("route/my_cool_endpoint/POST/status/200") should be(1)
    counter("route/my_cool_endpoint/POST/status/2XX") should be(1)
    stat("route/my_cool_endpoint/POST/response_size") should contain(3.0)
    stat("route/my_cool_endpoint/POST/time") should have size (1)
    stat("route/my_cool_endpoint/POST/time/200") should have size (1)
    stat("route/my_cool_endpoint/POST/time/2XX") should have size (1)
  }

  "per-route forwarded stats" in {
    server.httpGet(
      "/ok",
      andExpect = Ok)

    server.httpGet(
      "/forwarded",
      andExpect = Ok,
      withBody =  "This works.")

    server.httpPost(
      "/longer/post/path/foo",
      postBody = "",
      andExpect = Ok)

    server.httpPost(
      "/longer/post/path/with/name/:capture",
      postBody = "",
      andExpect = Ok)

    // global stats
    // compatible with com.twitter.finagle.http.filter.StatsFilter
    counter("status/200") should be(4)
    counter("status/2XX") should be(4)
    stat("response_size") should be(List(2.0, 11.0, 3.0, 3.0))
    stat("time/200") should have size (4)
    stat("time/2XX") should have size (4)

    // don't record stats that finagle already has
    counter("requests") should be(0)
    stat("time") should be('empty)
    counter("http/requests") should be(4)
    stat("http/request_latency_ms") should have size (4)

    // per-route stats
    counter("route/ok/GET/requests") should be(1)
    counter("route/ok/GET/status/200") should be(1)
    counter("route/ok/GET/status/2XX") should be(1)
    stat("route/ok/GET/response_size") should contain(2.0)
    stat("route/ok/GET/time") should have size (1)
    stat("route/ok/GET/time/200") should have size (1)
    stat("route/ok/GET/time/2XX") should have size (1)

    counter("route/forwarded_get/GET/requests") should be(1)
    counter("route/forwarded_get/GET/status/200") should be(1)
    counter("route/forwarded_get/GET/status/2XX") should be(1)
    stat("route/forwarded_get/GET/response_size") should contain(11.0)
    stat("route/forwarded_get/GET/time") should have size (1)
    stat("route/forwarded_get/GET/time/200") should have size (1)
    stat("route/forwarded_get/GET/time/2XX") should have size (1)

    counter("route/longer_post_path_capture/POST/requests") should be(1)
    counter("route/longer_post_path_capture/POST/status/200") should be(1)
    counter("route/longer_post_path_capture/POST/status/2XX") should be(1)
    stat("route/longer_post_path_capture/POST/response_size") should contain(3.0)
    stat("route/longer_post_path_capture/POST/time") should have size (1)
    stat("route/longer_post_path_capture/POST/time/200") should have size (1)
    stat("route/longer_post_path_capture/POST/time/2XX") should have size (1)

    counter("route/my_cool_endpoint/POST/requests") should be(1)
    counter("route/my_cool_endpoint/POST/status/200") should be(1)
    counter("route/my_cool_endpoint/POST/status/2XX") should be(1)
    stat("route/my_cool_endpoint/POST/response_size") should contain(3.0)
    stat("route/my_cool_endpoint/POST/time") should have size (1)
    stat("route/my_cool_endpoint/POST/time/200") should have size (1)
    stat("route/my_cool_endpoint/POST/time/2XX") should have size (1)
  }

  "ports" in {
    server.httpExternalPort should be > 0

    // no https server configured
    intercept[Exception] {
      server.httpsExternalPort
    }
  }

  "Bad request for missing header" in {
    server.httpPost(
      "/createUser",
      postBody = """{"name":"bob", "age":50}""",
      andExpect = BadRequest,
      withBody = """{"errors":["request_id: header is required"]}"""
    )
  }

  "Bad request for missing form param" in {
    server.httpFormPost(
      "/formPost",
      params = Map("name" -> "bob"),
      andExpect = BadRequest,
      withBody = """{"errors":["age: formParam is required"]}""")
  }

  "accepts request with header and body" in {
    server.httpPost(
      "/createUser",
      headers = Map("request_id" -> "732647326473"),
      postBody = """{"name":"bob", "age":50}""",
      andExpect = Created,
      withLocation = "/users/732647326473")
  }

  "non case class returned" in {
    server.httpGet(
      "/non_case_class",
      withJsonBody = """
      {
        "name" : "Bob",
        "age" : 21
      }
      """)
  }

  "bytes returned" in {
    val response = server.httpGet(
      "/bytes",
      withBody = "Steve")

    response.contentType should equal(Some(MediaType.OCTET_STREAM.toString))
  }

  "Bad request for deserialization of wrapped value from a json object" in {
    server.httpPost(
      "/RequestWithSeqWrappedString",
      postBody = """{"value" : [{"foo" : "foo"}]}""",
      andExpect = BadRequest)
  }

  "Bad request for deserialization of an invalid joda LocalDate" in {
    server.httpPost(
      "/localDateRequest",
      postBody = """{"date" : "2016-11-32"}""",
      andExpect = BadRequest)
  }

  "PATCH with JsonPatch" should {
    "JsonPatch" in {
      val request = RequestBuilder.patch("/jsonPatch")
        .body(
          """[
            |{"op":"add","path":"/fruit","value":"orange"},
            |{"op":"remove","path":"/hello"},
            |{"op":"copy","from":"/fruit","path":"/veggie"},
            |{"op":"replace","path":"/veggie","value":"bean"},
            |{"op":"move","from":"/fruit","path":"/food"},
            |{"op":"test","path":"/food","value":"orange"}
            |]""".stripMargin,
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = Ok,
        withJsonBody = """{"food":"orange","veggie":"bean"}""")
    }

    "JsonPatch with nested path" in {
      val request = RequestBuilder.patch("/jsonPatch/nested")
        .body(
          """[
            |{"op":"add","path":"/level1/level2/fruit","value":"orange"},
            |{"op":"remove","path":"/level1/level2/hello"},
            |{"op":"copy","from":"/level1/level2/fruit","path":"/level1/level2/veggie"},
            |{"op":"replace","path":"/level1/level2/veggie","value":"bean"},
            |{"op":"move","from":"/level1/level2/fruit","path":"/level1/level2/food"},
            |{"op":"test","path":"/level1/level2/food","value":"orange"}
            |]""".stripMargin,
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = Ok,
        withJsonBody = """{"level1":{"level2":{"food":"orange","veggie":"bean"}}}""")
    }

    "JsonPatch with nested 4 levels path" in {
      val request = RequestBuilder.patch("/jsonPatch/level3")
        .body(
          """[
            |{"op":"add","path":"/level0/level1/level2/fruit","value":"orange"},
            |{"op":"remove","path":"/level0/level1/level2/hello"},
            |{"op":"copy","from":"/level0/level1/level2/fruit","path":"/level0/level1/level2/veggie"},
            |{"op":"replace","path":"/level0/level1/level2/veggie","value":"bean"},
            |{"op":"move","from":"/level0/level1/level2/fruit","path":"/level0/level1/level2/food"},
            |{"op":"test","path":"/level0/level1/level2/food","value":"orange"}
            |]""".stripMargin,
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = Ok,
        withJsonBody = """{"level0":{"level1":{"level2":{"food":"orange","veggie":"bean"}}}}""")
    }

    "JsonPatch operating with non-leaf node" in {
      val request = RequestBuilder.patch("/jsonPatch/nonleaf")
        .body(
          """[
            |{"op":"add","path":"/root/middle","value":{"left":"middle-left"}},
            |{"op":"remove","path":"/root/left"},
            |{"op":"copy","from":"/root/right","path":"/root/left"},
            |{"op":"replace","path":"/root/left","value":{"left":"left-left-2","right":"left-right-2"}},
            |{"op":"move","from":"/root/left","path":"/root/left2"},
            |{"op":"test","path":"/root/left2","value":{"left":"left-left-2","right":"left-right-2"}}
            |]""".stripMargin,
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = Ok,
        withJsonBody =
          """
            |{"root":{
            |"left2":{"left":"left-left-2","right":"left-right-2"},
            |"middle":{"left":"middle-left"},
            |"right":{"left":"right-left","right":"right-right"}}
            |}""".stripMargin)
    }

    "JsonPatch for string" in {
      val request = RequestBuilder.patch("/jsonPatch/string")
        .body(
          """[
            |{"op":"add","path":"/fruit","value":"orange"},
            |{"op":"remove","path":"/hello"},
            |{"op":"copy","from":"/fruit","path":"/veggie"},
            |{"op":"replace","path":"/veggie","value":"bean"},
            |{"op":"move","from":"/fruit","path":"/food"},
            |{"op":"test","path":"/food","value":"orange"}
            |]""".stripMargin,
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = Ok,
        withJsonBody = """{"food":"orange","veggie":"bean"}""")
    }

    // error message for a path does not begin with a slash: CaseClassMappingException,
    // "path: Can not construct instance of com.fasterxml.jackson.core.JsonPointer,
    // problem: Invalid input: JSON Pointer expression must start with '/'.
    "JsonPatch fails when a path does not begin with a slash" in {
      val request = RequestBuilder.patch("/jsonPatch/nested")
        .body(
          """[{"op":"add","path":"fruit","value":"orange"}]""",
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = BadRequest)
    }

    "JsonPatch fails when a path is empty" in {
      val request = RequestBuilder.patch("/jsonPatch/nested")
        .body(
          """[{"op":"add","path":"","value":"orange"}]""",
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = BadRequest,
        withJsonBody = """{"errors":["invalid path - empty path"]}""")
    }

    "JsonPatch fails when the patch operation specs are invalid" in {
      val request = RequestBuilder.patch("/jsonPatch")
        .body(
          """[{"op":"copy","path":"/hello","value":"/shouldbefrom"}]""",
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = BadRequest,
        withJsonBody = """{"errors":["invalid from for copy operation"]}""")
    }

    "JsonPatch move operation with same from and path - jsonNode" in {
      val request = RequestBuilder.patch("/jsonPatch")
        .body(
          """[
            |{"op":"move","from":"/hello","path":"/hello"},
            |{"op":"test","path":"/hello","value":"world"}
            |]""".stripMargin,
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = Ok,
        withJsonBody = """{"hello":"world"}""")
    }

    "JsonPatch fails when Content-Type is not consistent" in {
      val request = RequestBuilder.patch("/jsonPatch/nested")
        .body(
          """[{"op":"add","path":"/fruit","value":"orange"}]""",
          contentType = Message.ContentTypeJson)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = BadRequest,
        withJsonBody = """{"errors":["incorrect Content-Type, should be application/json-patch+json"]}""")
    }
  }
}
