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
import com.twitter.inject.server.FeatureTest
import com.twitter.io.Buf
import org.apache.commons.io.IOUtils
import org.scalatest.exceptions.TestFailedException

class DoEverythingServerFeatureTest extends FeatureTest {

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

  test("named string") {
    namedExampleString should equal("named")
  }

  /* Admin routes */

  /* Note that tests against the Global Singleton HttpMuxer break when multiple servers are started in process */
  test("GET /admin/foo") {
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

  test("GET /admin/external/filtered") {
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

  test("respond to /example") {
    server.httpGet(
      "/example/routing/always",
      withBody = "always response")

    server.httpGet(
      "/example/routing/always2",
      withBody = "always response")
  }

  test("GET /plaintext") {
    val response1 = server.httpGet(
      "/plaintext",
      withBody = "Hello, World!")

    response1.contentType should equal(Some(MediaType.PLAIN_TEXT_UTF_8.toString))

    val response2 = server.httpGet(
      "/plaintext/",
      withBody = "Hello, World!")

    response2.contentType should equal(Some(MediaType.PLAIN_TEXT_UTF_8.toString))
  }

  test("/plaintext (prefixed)") {
    val response = server.httpGet(
      "/1.1/plaintext",
      withBody = "Hello, World!")

    response.contentType should equal(Some(MediaType.PLAIN_TEXT_UTF_8.toString))
  }

  test("/forbiddenByFilter (prefixed)") {
    server.httpGet(
      "/1.1/forbiddenByFilter",
      andExpect = Forbidden
    )
  }

  test("/forbiddenByFilter (prefixed outer)") {
    server.httpGet(
      "/1.1/forbiddenByFilterPrefilter",
      andExpect = Forbidden
    )
  }

  test("/appendMultiplePrefixed (prefixed)") {
    server.httpGet(
      "/1.1/appendMultiplePrefixed",
      withBody = "12"
    )
  }

  test("/freestyleWithHeader (prefixed)") {
    server.httpGet(
      "/1.1/freestyleWithHeader",
      withBody = "bang"
    )
  }

  test("/1.1/waterfall/users (cascading prefixes)") {
    server.httpGet(
      "/1.1/waterfall/users/",
      andExpect = Ok,
      withBody = "ok!"
    )
  }

  test("GET /bytearray") {
    val response = server.httpGet(
      "/bytearray")

    response.contentType should equal(Some(MediaType.OCTET_STREAM.toString))
  }

  test("GET /inputstream") {
    val response = server.httpGet(
      "/inputstream")

    response.contentType should equal(Some(MediaType.OCTET_STREAM.toString))
  }

  test("GET /useragent") {
    server.httpGet(
      "/useragent",
      headers = Map("User-Agent" -> "Firefox"),
      withBody = "Firefox")
  }

  test("response should contain server/date headers") {
    val response = server.httpGet("/example/routing/always")
    response.statusCode should equal(200)
    response.contentString should equal("always response")

    response.headerMap.get("Date") should not be empty
    response.headerMap.get("Server") should not be empty
  }

  test("json response to /example") {
    val response = server.httpGet("/example/routing/json/1")
    response.statusCode should equal(200)
    jsonDiff(response.contentString, """{"id":"1","name":"bob","magic":"1","module_magic":"2"}""")
  }

  test("GET /stringMap") {
    server.httpGet(
      "/stringMap",
      andExpect = Ok,
      withJsonBody = """{"message":"Hello, World!"}""")
  }

  test("GET /ok") {
    server.httpGet(
      "/ok",
      andExpect = Ok)
  }

  test("GET /created") {
    server.httpGet(
      "/created",
      andExpect = Created)
  }

  test("respond to /accepted") {
    server.httpGet(
      "/accepted",
      andExpect = Accepted,
      withBody = "accepted")

    server.httpGet(
      "/accepted2",
      andExpect = Accepted,
      withBody = "accepted")
  }

  test("GET /json") {
    server.httpGet(
      "/json",
      withJsonBody = "{}")
  }

  test("GET /json2") {
    server.httpGet(
      "/json2",
      withJsonBody = "{}")
  }

  test("GET /none") {
    server.httpGet(
      "/none",
      withBody = "")
  }

  test("GET /bodyunit") {
    server.httpGet(
      "/bodyunit",
      withBody = "")
  }

  test("GET /bodynull") {
    server.httpGet(
      "/bodynull",
      withBody = "")
  }

  test("GET /bodyEmptyString") {
    server.httpGet(
      "/bodyEmptyString",
      withBody = "")
  }

  test("GET /routeParamGetAll/:id") {
    server.httpGet(
      "/routeParamGetAll/1",
      withJsonBody = """
      [
        "1",
        "1"
      ]
                     """)
  }

  test("GET /notfound") {
    server.httpGet(
      "/notfound",
      andExpect = NotFound)
  }

  test("GET /notfound2") {
    server.httpGet(
      "/notfound2",
      andExpect = NotFound)
  }

  test("GET /notfoundexception") {
    server.httpGet(
      "/notfoundexception",
      andExpect = NotFound)
  }

  test("GET /notfoundexception2") {
    server.httpGet(
      "/notfoundexception2",
      andExpect = NotFound)
  }

  test("GET /badrequest") {
    server.httpGet(
      "/badrequest",
      andExpect = BadRequest)
  }

  test("GET /BadRequestException") {
    server.httpGet(
      "/BadRequestException",
      andExpect = BadRequest)
  }

  test("GET /forbidden") {
    server.httpGet(
      "/forbidden",
      andExpect = Forbidden)
  }

  test("GET /ForbiddenException") {
    server.httpGet(
      "/ForbiddenException",
      andExpect = Forbidden)
  }

  test("GET /ForbiddenException2") {
    server.httpGet(
      "/ForbiddenException2",
      andExpect = Forbidden)
  }

  test("GET /methodnotallowed") {
    server.httpGet(
      "/methodnotallowed",
      andExpect = MethodNotAllowed)
  }

  test("GET /unavailable") {
    server.httpGet(
      "/unavailable",
      andExpect = ServiceUnavailable)
  }

  test("GET /unauthorized") {
    server.httpGet(
      "/unauthorized",
      andExpect = Unauthorized)
  }

  test("GET /conflict") {
    server.httpGet(
      "/conflict",
      andExpect = Conflict)
  }

  test("GET /ConflictException") {
    server.httpGet(
      "/ConflictException",
      andExpect = Conflict)
  }

  test("GET /ConflictException2") {
    server.httpGet(
      "/ConflictException2",
      andExpect = Conflict)
  }

  test("GET /ConflictException3") {
    server.httpGet(
      "/ConflictException3",
      andExpect = Conflict)
  }

  test("GET /servererrorexception") {
    server.httpGet(
      "/servererrorexception",
      andExpect = InternalServerError)
  }

  test("GET /serviceunavailableexception") {
    server.httpGet(
      "/serviceunavailableexception",
      andExpect = ServiceUnavailable)
  }

  test("GET /serviceunavailableexception2") {
    server.httpGet(
      "/serviceunavailableexception2",
      andExpect = ServiceUnavailable)
  }

  test("GET /responsebuilder_status_code") {
    server.httpGet(
      "/responsebuilder_status_code",
      andExpect = ServiceUnavailable)
  }

  test("GET /responsebuilder_status") {
    server.httpGet(
      "/responsebuilder_status",
      andExpect = ServiceUnavailable)
  }

  test("GET /redirect") {
    server.httpGet(
      "/redirect",
      andExpect = TemporaryRedirect)
  }

  test("GET /found") {
    server.httpGet(
      "/found",
      andExpect = Found)
  }

  test("GET /future") {
    server.httpGet(
      "/future",
      andExpect = Ok,
      withBody = "future")
  }

  test("POST /foo") {
    server.httpPost(
      "/foo",
      postBody = "",
      andExpect = Ok,
      withBody = "bar")
  }

  test("POST /foo (prefixed)") {
    server.httpPost(
      "/1.1/foo",
      postBody = "",
      andExpect = Ok,
      withBody = "bar")
  }

  test("POST /formPost") {
    server.httpFormPost(
      "/formPost",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Ok,
      withBody = "bob")
  }

  test("POST /multipartParamsEcho") {
    val request = deserializeRequest("/multipart/request-POST-android.bytes")
    request.uri = "/multipartParamsEcho"

    server.httpRequest(
      request = request,
      suppress = true,
      andExpect = Ok,
      withJsonBody = """["banner"]""")
  }

  test("POST /formPostMultipart") {
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

  test("PUT /multipartParamsPutEcho") {
    val request = deserializeRequest("/multipart/request-POST-android.bytes")
    request.uri = "/multipartParamsPutEcho"
    request.method = Method.Put

    server.httpRequest(
      request = request,
      suppress = true,
      andExpect = Ok,
      withJsonBody = """["banner"]""")
  }

  test("POST /formPostView") {
    server.httpFormPost(
      "/formPostView",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Ok,
      withBody = "age:18\nname:bob\nuser1\nuser2\n")
  }

  test("GET /getView") {
    server.httpGet(
      "/getView?age=18&name=bob",
      andExpect = Ok,
      withBody = "age:18\nname:bob\nuser1\nuser2\n")
  }

  test("POST /formPostViewFromBuilderView (from BuilderView with diff template than annotation)") {
    server.httpFormPost(
      "/formPostViewFromBuilderView",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Ok,
      withBody = "age2:18\nname2:bob\nuser1\nuser2\n")
  }

  test("POST /formPostViewFromBuilderHtml") {
    server.httpFormPost(
      "/formPostViewFromBuilderHtml",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Ok,
      withBody = "age:18\nname:bob\nuser1\nuser2\n")
  }

  test("POST /formPostViewFromBuilderCreatedView") {
    val response = server.httpFormPost(
      "/formPostViewFromBuilderCreatedView",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Created,
      withBody = "age2:18\nname2:bob\nuser1\nuser2\n")

    response.location should equal(Some("/foo/1"))
  }

  test("POST /formPostViewFromBuilderCreatedHtml") {
    val response = server.httpFormPost(
      "/formPostViewFromBuilderCreatedHtml",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Created,
      withBody = "age:18\nname:bob\nuser1\nuser2\n")

    response.location should equal(Some("/foo/1"))
  }

  test("POST user with injected group_id from route param") {
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

  test("POST /multipleRouteParams") {
    server.httpPost(
      "/multipleRouteParams",
      "",
      andExpect = InternalServerError)
  }

  test("POST /caseClassWithRequestField") {
    server.httpPost(
      "/caseClassWithRequestField",
      "",
      andExpect = Ok)
  }

  test("GET /null") {
    server.httpGet(
      "/null",
      andExpect = Ok,
      withBody = "")
  }

  test("GET /empty") {
    server.httpGet(
      "/empty",
      andExpect = Ok,
      withBody = "")
  }

  test("GET /unit") {
    val response = server.httpGet(
      "/unit",
      andExpect = Ok,
      withBody = "")

    response.contentLength should equal(Some(0))
    // no content-type as there is no content-body
    response.contentType should equal(None)
  }

  test("GET not found path") {
    server.httpGet(
      "/sdafasdfsadfsadfsafd",
      andExpect = NotFound,
      withBody = "")
  }

  test("GET complex path") {
    server.httpGet(
      "/complexpath/steve",
      andExpect = Ok,
      withBody = "done steve 5000")
  }

  test("GET complex query") {
    server.httpGet(
      "/complexquery?name=fred",
      andExpect = Ok,
      withBody = "done fred 5000")
  }

  test("GET /testfile") {
    server.httpGet(
      "/testfile",
      andExpect = Ok,
      withBody = "testfile123")
  }

  test("GET /testfileWhenNotfound") {
    server.httpGet(
      "/testfileWhenNotfound",
      andExpect = NotFound,
      withBody = "/doesntexist.txt not found")
  }

  test("GET /exception") {
    server.httpGet(
      "/exception",
      andExpect = InternalServerError)
  }

  test("GET /pathUrl") {
    val request = server.httpGet(
      "/pathUrl",
      andExpect = Ok)

    request.contentString should endWith("/pathUrl/")
  }

  test("GET /path") {
    val request = server.httpGet(
      "/path",
      andExpect = Ok)

    request.contentString should endWith("/path/")
  }

  test("GET /put (NotFound)") {
    server.httpGet(
      "/put",
      andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
  }

  test("putJson") {
    server.httpPutJson[JsonNode](
      "/putJson/123",
      putBody = """{"name": "Steve"}""",
      andExpect = Ok,
      withJsonBody = """{"id": 123, "name": "Steve"}""")
  }

  test("putJson without type param") {
    val e = intercept[TestFailedException] {
      server.httpPutJson(
        "/putJson/123",
        putBody = """{"name": "Steve"}""",
        andExpect = Ok,
        withJsonBody = """{"id": 123, "name": "Steve"}""")
    }
    e.getMessage() should include("requires a type-param")
  }

  test("PUT body with multi-byte characters") {
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

  test("PUT") {
    server.httpPut(
      "/put/123",
      putBody = "asdf",
      andExpect = Ok,
      withBody = "123_asdf")
  }

  test("PUT with RouteParam and non-json body") {
    server.httpPut(
      "/put_route_param/123",
      contentType = "plain/text",
      putBody = "asdf",
      andExpect = Ok,
      withBody = "123_asdf")
  }

  test("PUT with RouteParam and json body") {
    server.httpPut(
      "/put_route_param/123",
      putBody = "{}",
      andExpect = Ok,
      withBody = "123_")
  }

  test("PUT with RouteParam and empty body") {
    server.httpPut(
      "/put_route_param/123",
      putBody = "",
      andExpect = Ok,
      withBody = "123_")
  }

  test("PUT /put_route_param_and_name") {
    server.httpPut(
      "/put_route_param_and_name/123",
      putBody = """{"name": "bob"}""",
      andExpect = Ok,
      withBody = "123_bob")
  }

  test("PUT /put_route_param_and_name (with empty PUT body)") {
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

  test("PUT /put_route_param_and_name (with non-json PUT body)") {
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

  test("PUT /put_id_ignoring_body") {
    val response = server.httpPut(
      "/put_id_ignoring_body/42",
      putBody = "invalid JSON",
      andExpect = Ok,
      withJsonBody = "42")

    response.contentType should equal(Some(MediaType.PLAIN_TEXT_UTF_8.toString))
  }

  test("PUT /put_id_not_ignoring_body") {
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

  test("POST /putAndPost") {
    server.httpPost(
      "/putAndPost",
      postBody = "1",
      andExpect = Ok,
      withBody = "POST1")
  }

  test("PUT /putAndPost") {
    server.httpPut(
      "/putAndPost",
      putBody = "2",
      andExpect = Ok,
      withBody = "PUT2")
  }

  test("GET /putAndPost (NotFound)") {
    server.httpGet(
      "/putAndPost",
      andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
  }

  test("POST /postAndPut") {
    server.httpPost(
      "/postAndPut",
      postBody = "1",
      andExpect = Ok,
      withBody = "POST1")
  }

  test("PUT /postAndPut") {
    server.httpPut(
      "/postAndPut",
      putBody = "2",
      andExpect = Ok,
      withBody = "PUT2")
  }

  test("GET /postAndPut (NotFound)") {
    server.httpGet(
      "/postAndPut",
      andExpect = NotFound) //TODO: Should be 405 Method Not Allowed
  }

  test("GET /true") {
    server.httpGet(
      "/true",
      andExpect = Ok,
      withBody = "true")
  }

  test("GET /index (root)") {
    server.httpGet(
      "/index/",
      andExpect = Ok,
      withBody = "testindex")
  }

  test("GET index file without extension") {
    server.httpGet(
      "/index/testfile",
      andExpect = Ok,
      withBody = "testindex")
  }

  test("GET index file with extension") {
    server.httpGet(
      "/index/testfile.txt",
      andExpect = Ok,
      withBody = "testfile123")
  }

  test("GET /implicitOkAndException (when ok)") {
    server.httpGet(
      "/implicitOkAndException?hi",
      andExpect = Ok)
  }

  test("GET /implicitOkAndException (when bad request exception)") {
    server.httpGet(
      "/implicitOkAndException",
      andExpect = BadRequest)
  }

  test("slow") {
    pending // manually run to test fix for go/jira/CSL-565
    server.httpGet(
      "/slow",
      andExpect = Ok)
  }

  test("response builder") {
    val response = server.httpGet(
      "/builderCreatedWithHeader",
      andExpect = Created,
      withLocation = "http://foo.com/1")

    response.headerMap.get("a") should equal(Some("b"))
  }

  test("request injection") {
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

  test("request injections not found") {
    server.httpGet(
      "/requestInjectionsNotFound",
      andExpect = InternalServerError,
      withErrors = Seq(
        "internal server error"))
  }

  test("GET request injections not available") {
    server.httpGet(
      "/requestInjectionsNotAvailable",
      andExpect = InternalServerError,
      withErrors = Seq(
        "internal server error"))
  }

  test("POST request injections not available") {
    server.httpPost(
      "/requestInjectionsNotAvailable",
      "{}",
      andExpect = InternalServerError,
      withErrors = Seq(
        "internal server error"))
  }

  test("POST empty json request injections not available") {
    server.httpPost(
      "/requestInjectionsNotAvailable",
      "",
      andExpect = InternalServerError,
      withErrors = Seq(
        "internal server error"))
  }

  test("POST invalid json request injections not available") {
    server.httpPost(
      "/requestInjectionsNotAvailable",
      "{abc",
      andExpect = BadRequest,
      withErrors = Seq(
        "Unexpected character ('a' (code 97)): was expecting double-quote to start field name"))
  }

  test("GET json user") {
    val response = server.httpGet(
      "/users/mary",
      andExpect = Ok,
      withJsonBody = """{ "name" : "mary" }""")

    response.headerMap("content-type") should equal(MediaType.JSON_UTF_8.toString)
  }

  test("POST json user") {
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

  test("POST json user with missing required field") {
    server.httpPost(
      "/users",
      """
          {
          }
      """,
      andExpect = BadRequest,
      withErrors = Seq("name: field is required"))
  }

  test("POST body with multi-byte characters") {
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

  test("POST json user with failed field validation") {
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

  test("POST json user with null required field") {
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

  test("POST json with failed array element validation") {
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

  test("POST json with null array element") {
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

  test("POST json user with failed method validation") {
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

  test("POST json user with invalid field validation") {
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

  test("POST json user with invalid method validation") {
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

  test("POST json user with invalid content type") {
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

  test("POST json user with missing required field when message body reader uses intermediate JsonNode") {
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

  test("POST json user with method validation error when message body reader uses intermediate JsonNode") {
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

  test("injector test") {
    server.injector.instance[String]("example") should equal("named")

    val exampleService = server.injector.instance[DoEverythingService]
    exampleService should not equal null
    server.injector.instance(classOf[DoEverythingService]) should equal(exampleService)

    val key = Key.get(new TypeLiteral[DoEverythingService]() {})
    server.injector.instance(key) should equal(exampleService)
  }

  test("GET /array") {
    server.httpGet(
      "/array",
      andExpect = Ok,
      withJsonBody = """["a", "b"]""")
  }

  test("GET /set") {
    server.httpGet(
      "/set",
      andExpect = Ok,
      withJsonBody = """["a", "b"]""")
  }

  test("GET /seq") {
    server.httpGet(
      "/seq",
      andExpect = Ok,
      withJsonBody = """["a", "b"]""")
  }

  test("DELETE") {
    server.httpDelete(
      "/delete",
      andExpect = Ok,
      withBody = "delete")
  }

  test("DELETE with body") {
    server.httpDelete(
      "/delete",
      deleteBody = "DELETE BODY",
      contentType = MediaType.PLAIN_TEXT_UTF_8.toString,
      andExpect = Ok,
      withBody = "delete")
  }

  test("DELETE with JSON body") {
    server.httpDelete(
      "/delete",
      deleteBody = "{\"id\": \"11211\"}",
      andExpect = Ok,
      withBody = "delete")
  }

  test("OPTIONS") {
    server.httpOptions(
      "/options",
      andExpect = Ok,
      withBody = "options")
  }

  test("HEAD") {
    server.httpHead(
      "/head",
      andExpect = Conflict,
      withBody = "") //HEAD responses cannot have bodies
  }

  test("PATCH") {
    server.httpPatch(
      "/patch",
      contentType = MediaType.PLAIN_TEXT_UTF_8.toString,
      patchBody = "asdf",
      andExpect = Ok,
      withBody = "patch")
  }

  test("PATCH with JSON body") {
    server.httpPatch(
      "/patch",
      patchBody = "{\"id\": \"11211\"}", // note: this is not json-patch (RFC6902), just PATCH with a JSON content-type.
      andExpect = Ok,
      withBody = "patch")
  }

  test("PATCH body with multi-byte characters") {
    val musicalNote = "\uD83C\uDFB5"
    val body = s"${musicalNote} ${musicalNote}"
    server.httpPatch(
      "/echo",
      contentType = MediaType.PLAIN_TEXT_UTF_8.toString,
      patchBody = body,
      andExpect = Ok,
      withBody = body)
  }

  test("GET /NonGuice") {
    server.httpGet(
      "/NonGuice",
      andExpect = Ok,
      withBody = "pong")
  }

  test("GET with query parameters as string sequence") {
    server.httpGet(
      "/RequestWithQueryParamSeqString?foo=1&foo=2&foo=3",
      andExpect = Ok,
      withJsonBody =
        """
            { "foo": ["11", "21", "31"] }
        """
    )
  }

  test("GET with query parameters as string sequence with no values (required)") {
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

  test("GET defaulted request case class parameters as string sequence") {
    server.httpGet(
      "/RequestWithDefaultedQueryParamSeqString",
      andExpect = Ok,
      withJsonBody =
        """
            { "foo": ["foo", "bar", "baz"] }
        """
    )
  }

  test("GET defaulted request case class parameters as string sequence with query params") {
    server.httpGet(
      "/RequestWithDefaultedQueryParamSeqString?foo=override1&foo=override2",
      andExpect = Ok,
      withJsonBody =
        """
            { "foo": ["override1", "override2"] }
        """
    )
  }

  test("GET with query parameters as empty string sequence") {
    server.httpGet(
      "/RequestWithDefaultedQueryParamSeqString?foo=",
      andExpect = Ok,
      withJsonBody =
        """
            { "foo": [""] }
        """
    )
  }

  test("GET defaulted request case class parameter as string") {
    server.httpGet(
      "/RequestWithDefaultQueryParam",
      andExpect = Ok,
      withJsonBody =
        """
            { "param": "default" }
        """
    )
  }

  test("GET defaulted request case class parameter as string with query param") {
    server.httpGet(
      "/RequestWithDefaultQueryParam?param=set",
      andExpect = Ok,
      withJsonBody =
        """
            { "param": "set" }
        """
    )
  }

  test("GET with query parameters as long sequence") {
    server.httpGet(
      "/RequestWithQueryParamSeqLong?foo=1&foo=2&foo=3",
      andExpect = Ok,
      withJsonBody =
        """
            { "foo": [2, 3, 4] }
        """
    )
  }

  test("GET with query parameters as empty long sequence") {
    server.httpGet(
      "/RequestWithQueryParamSeqLong?foo=",
      andExpect = Ok,
      withJsonBody =
        """
            { "foo": [] }
        """
    )
  }

  test("apply route filter") {
    server.httpGet(
      "/forbiddenByFilter",
      andExpect = Forbidden)
  }

  test("apply multiple route filters added by type and instance") {
    server.httpGet(
      "/multiFilterAppend",
      andExpect = Ok,
      withBody = "014")
  }

  test("apply multiple route filters added by type") {
    server.httpGet(
      "/multiIdentityFilterAppend",
      andExpect = Ok,
      withBody = "ok!")
  }

  test("apply multiple route filters") {
    server.httpGet(
      "/multipleRouteFilters",
      andExpect = Ok,
      withBody = "012345")
  }

  test("anyMethod GET") {
    server.httpGet(
      "/anyMethod",
      andExpect = Ok)
  }

  test("anyMethod TRACE") {
    server.httpRequest(
      Request(Method.Trace, "/anyMethod"),
      andExpect = Ok)
  }

  test("anyMethod HEAD") {
    server.httpHead(
      "/anyMethod",
      andExpect = Ok)
  }

  test("anyMethod POST") {
    server.httpPost(
      "/anyMethod",
      postBody = "",
      andExpect = MethodNotAllowed)
  }

  test("anyMethod PUT") {
    server.httpPut(
      "/anyMethod",
      putBody = "",
      andExpect = MethodNotAllowed)
  }

  test("anyMethod DELETE") {
    server.httpDelete(
      "/anyMethod",
      deleteBody = "",
      andExpect = MethodNotAllowed)
  }

  test("GET /column/:key//:*") {
    server.httpGet(
      "/column/foo//bar?baz=quux",
      andExpect = Ok,
      withBody = "foo/bar")
  }

  test("POST /SomethingStreamedRequest") {
    server.httpPost(
      "/SomethingStreamedRequest.json?something_id=FOO&field1=BAR&field2=3",
      postBody = "",
      andExpect = Ok,
      withBody = "FOO/BAR/3")
  }

  test("POST /SomethingStreamedRequestAsJsonResponse") {
    val response = server.httpPostJson[SomethingStreamedResponse](
      "/SomethingStreamedRequestAsJsonResponse.json?something_id=FOO&field1=BAR&field2=3",
      postBody = "",
      andExpect = Ok)
    assert(response.somethingId == "FOO")
    assert(response.field1.get == "BAR")
    assert(response.field2.get == 3)
  }

  test("GET /forwarded") {
    server.httpGet(
      "/forwarded",
      andExpect = Ok,
      withBody =  "This works.")
  }

  test("GET /forwardToForbidden") {
    server.httpGet(
      "/forwardToForbidden",
      andExpect = Forbidden)
  }

  test("GET /forwardCaseClass") {
    server.httpGet(
      "/forwardCaseClass",
      andExpect = Ok,
      withBody =  "This works.")
  }

  test("POST /forwarded") {
    server.httpPost(
      "/forwarded",
      postBody = "",
      andExpect = Ok,
      withBody =  "This works.")
  }

  test("HttpResponseException") {
    server.httpGet(
      "/HttpResponseException",
      andExpect = Conflict,
      withBody = "conflicted")
  }

  test("toFutureException") {
    server.httpGet(
      "/toFutureException",
      andExpect = Conflict,
      withBody = "conflicted")
  }

  test("HttpExceptionPlain") {
    server.httpGet(
      "/HttpExceptionPlain",
      andExpect = Created,
      withBody = "foo")
  }

  test("HttpExceptionErrors") {
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

  test("NotFoundException") {
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

  test("ConflictException") {
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

  test("InternalServerErrorExceptionPlain") {
    server.httpGet(
      "/InternalServerErrorExceptionPlain",
      andExpect = InternalServerError,
      withBody = "foo1")
  }

  test("NotAcceptableException") {
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

  test("NotAcceptableException2") {
    server.httpGet(
      "/NotAcceptableException2",
      andExpect = NotAcceptable)
  }

  test("Unserializable class field") {
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

  test("FooException") {
    val response = server.httpGet(
      "/FooException/42",
      andExpect = Forbidden,
      withBody = "foo")
    response.headerMap("Foo-ID") should equal("42")
  }

  test("BarException") {
    val response = server.httpGet(
      "/BarException",
      andExpect = Unauthorized,
      withBody = "bar")
    response.headerMap.contains("Foo-ID") should equal(false)
    response.headerMap("Bar-ID") should equal("123")
  }

  test("BazException") {
    val response = server.httpGet(
      "/BazException",
      andExpect = Forbidden,
      withBody = "foo")
    response.headerMap("Foo-ID") should equal("321")
  }

  test("FooBarBazException") {
    val response = server.httpGet(
      "/FooBarBazException",
      andExpect = Forbidden,
      withBody = "foo")
    response.headerMap("Foo-ID") should equal("321-123")
  }

  test("NoSuchMethodException") {
    server.httpGet(
      "/NoSuchMethodException",
      andExpect = InternalServerError)
  }

  test("UsersRequest") {
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

  test("CaseClassWithIntQueryParam") {
    server.httpGet(
      "/CaseClassWithIntQueryParam?param=123",
      andExpect = Ok,
      withJsonBody = "[123]")
  }

  test("CaseClassWithShortQueryParam") {
    server.httpGet(
      "/CaseClassWithShortQueryParam?param=123",
      andExpect = Ok,
      withJsonBody = "[123]")
  }

  test("RequestWithBooleanQueryParams") {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=true",
      andExpect = Ok,
      withJsonBody = "[true]")
  }

  test("RequestWithBooleanQueryParams which is a true number") {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=1",
      andExpect = Ok,
      withJsonBody = "[true]")
  }

  test("RequestWithBooleanQueryParams which is a false number") {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=0",
      andExpect = Ok,
      withJsonBody = "[false]")
  }

  test("RequestWithBooleanQueryParam which is a true number") {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=1",
      andExpect = Ok,
      withJsonBody = "true")
  }

  test("RequestWithBooleanQueryParam which is a false number") {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=0",
      andExpect = Ok,
      withJsonBody = "false")
  }

  test("RequestWithBooleanQueryParam which is a true represented as t") {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=t",
      andExpect = Ok,
      withJsonBody = "true"
    )
  }

  test("RequestWithBooleanQueryParam which is a false represented as f") {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=f",
      andExpect = Ok,
      withJsonBody = "false"
    )
  }

  test("RequestWithBooleanQueryParam with incorrect param") {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=FOO",
      andExpect = BadRequest,
      withJsonBody = """{
        "errors" : [
          "param: 'FOO' is not a valid Boolean"
        ]
      }""")
  }

  test("RequestWithBooleanQueryParams with multiple params") {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=true&param=0&param=t",
      andExpect = Ok,
      withJsonBody = "[true, false, true]")
  }

  test("RequestWithBooleanQueryParams with incorrect params") {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=true&param=FOO",
      andExpect = BadRequest,
      withJsonBody = """{
        "errors" : [
          "param: 'FOO' is not a valid Boolean"
        ]
      }""")
  }

  test("RequestWithBooleanQueryParam which is a true") {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=true",
      andExpect = Ok,
      withJsonBody = "true")
  }

  test("RequestWithBooleanQueryParam which is empty") {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=",
      andExpect = BadRequest,
      withJsonBody = """{
        "errors" : [
          "param: queryParam is required"
        ]
      }""")
  }

  test("RequestWithOptionBooleanQueryParam which is a true") {
    server.httpGet(
      "/RequestWithOptionBooleanQueryParam?param=true",
      andExpect = Ok,
      withJsonBody = "Hi Some(true)")
  }

  test("RequestWithOptionBooleanQueryParam which is empty") {
    server.httpGet(
      "/RequestWithOptionBooleanQueryParam?param=",
      andExpect = Ok,
      withJsonBody = "Hi None")
  }

  test("RequestWithOptionBooleanQueryParam which is a false") {
    server.httpGet(
      "/RequestWithOptionBooleanQueryParam?param=false",
      andExpect = Ok,
      withJsonBody = "Hi Some(false)")
  }

  test("RequestWithBooleanQueryParam which is a false") {
    server.httpGet(
      "/RequestWithBooleanQueryParam?param=false",
      andExpect = Ok,
      withJsonBody = "false")
  }

  test("RequestWithBooleanQueryParams bad request") {
    server.httpGet(
      "/RequestWithBooleanQueryParams?param=foo",
      andExpect = BadRequest)
  }

  test("CaseClassWithCaseClassQueryParam") {
    server.httpGet(
      "/CaseClassWithCaseClassQueryParam?param=true",
      andExpect = BadRequest)
  }

  test("TestCaseClassWithHtml") {
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

  test("TRACE") {
    val request = Request(Trace, "/trace")
    server.httpRequest(
      request,
      andExpect = Ok,
      withBody = "trace 123")
  }

  test("bad method") {
    val request = Request(Method("foo"), "/trace")
    server.httpRequest(
      request,
      andExpect = BadRequest)
  }

  test("camelCaseJson") {
    server.httpGet(
      "/camelCaseJson",
      withJsonBody = """
      {
        "firstName": "Bob"
      }
                     """)
  }

  test("per-route stats") {
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

  test("per-route forwarded stats") {
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

  test("ports") {
    server.httpExternalPort should be > 0

    // no https server configured
    intercept[Exception] {
      server.httpsExternalPort
    }
  }

  test("Bad request for missing header") {
    server.httpPost(
      "/createUser",
      postBody = """{"name":"bob", "age":50}""",
      andExpect = BadRequest,
      withBody = """{"errors":["request_id: header is required"]}"""
    )
  }

  test("Bad request for missing form param") {
    server.httpFormPost(
      "/formPost",
      params = Map("name" -> "bob"),
      andExpect = BadRequest,
      withBody = """{"errors":["age: formParam is required"]}""")
  }

  test("accepts request with header and body") {
    server.httpPost(
      "/createUser",
      headers = Map("request_id" -> "732647326473"),
      postBody = """{"name":"bob", "age":50}""",
      andExpect = Created,
      withLocation = "/users/732647326473")
  }

  test("non case class returned") {
    server.httpGet(
      "/non_case_class",
      withJsonBody = """
      {
        "name" : "Bob",
        "age" : 21
      }
                     """)
  }

  test("bytes returned") {
    val response = server.httpGet(
      "/bytes",
      withBody = "Steve")

    response.contentType should equal(Some(MediaType.OCTET_STREAM.toString))
  }

  test("Bad request for deserialization of wrapped value from a json object") {
    server.httpPost(
      "/RequestWithSeqWrappedString",
      postBody = """{"value" : [{"foo" : "foo"}]}""",
      andExpect = BadRequest)
  }

  test("Bad request for deserialization of an invalid joda LocalDate") {
    server.httpPost(
      "/localDateRequest",
      postBody = """{"date" : "2016-11-32"}""",
      andExpect = BadRequest)
  }

  test("JsonPatch") {
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

  test("JsonPatch with nested path") {
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

  test("JsonPatch with nested 4 levels path") {
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

  test("JsonPatch operating with non-leaf node") {
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

  test("JsonPatch for string") {
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
  test("JsonPatch fails when a path does not begin with a slash") {
    val request = RequestBuilder.patch("/jsonPatch/nested")
      .body(
        """[{"op":"add","path":"fruit","value":"orange"}]""",
        contentType = Message.ContentTypeJsonPatch)

    server.httpRequestJson[JsonNode](
      request = request,
      andExpect = BadRequest)
  }

  test("JsonPatch fails when a path is empty") {
    val request = RequestBuilder.patch("/jsonPatch/nested")
      .body(
        """[{"op":"add","path":"","value":"orange"}]""",
        contentType = Message.ContentTypeJsonPatch)

    server.httpRequestJson[JsonNode](
      request = request,
      andExpect = BadRequest,
      withJsonBody = """{"errors":["invalid path - empty path"]}""")
  }

  test("JsonPatch fails when the patch operation specs are invalid") {
    val request = RequestBuilder.patch("/jsonPatch")
      .body(
        """[{"op":"copy","path":"/hello","value":"/shouldbefrom"}]""",
        contentType = Message.ContentTypeJsonPatch)

    server.httpRequestJson[JsonNode](
      request = request,
      andExpect = BadRequest,
      withJsonBody = """{"errors":["invalid from for copy operation"]}""")
  }

  test("JsonPatch move operation with same from and path - jsonNode") {
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

  test("JsonPatch fails when Content-Type is not consistent") {
    val request = RequestBuilder.patch("/jsonPatch/nested")
      .body(
        """[{"op":"add","path":"/fruit","value":"orange"}]""",
        contentType = Message.ContentTypeJson)

    server.httpRequestJson[JsonNode](
      request = request,
      andExpect = BadRequest,
      withJsonBody = """{"errors":["incorrect Content-Type, should be application/json-patch+json"]}""")
  }

  test("/millis") {
    val inMillis = 1489719177279L
    val request = RequestBuilder.get(s"/millis?date_time=$inMillis")

    server.httpRequest(
      request,
      andExpect = Ok,
      withBody = s"$inMillis")
  }
}
