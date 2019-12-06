package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.{Method, Request, Status}
import com.twitter.finagle.{ChannelClosedException, ChannelWriteException, Failure}
import com.twitter.finatra.annotations.CamelCaseMapper
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.exceptions._
import com.twitter.finatra.http.jsonpatch.{JsonPatch, JsonPatchOperator, JsonPatchUtility}
import com.twitter.finatra.http.marshalling.mapper._
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.finatra.http.response._
import com.twitter.finatra.http.routing.HttpForward
import com.twitter.finatra.http.tests.integration.doeverything.main.domain._
import com.twitter.finatra.http.tests.integration.doeverything.main.exceptions._
import com.twitter.finatra.http.tests.integration.doeverything.main.filters.{AppendToHeaderFilter, ForbiddenFilter, IdentityFilter}
import com.twitter.finatra.http.tests.integration.doeverything.main.jsonpatch._
import com.twitter.finatra.http.tests.integration.doeverything.main.services.{ComplexServiceFactory, DoEverythingService, MultiService}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.http.tests.integration.doeverything.main.domain.TestCaseClassWithLocalDate
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.inject.annotations.Flag
import com.twitter.util.Future
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import org.joda.time.DateTime
import scala.collection.SortedSet

class DoEverythingController @Inject()(
  @Flag("magicNum") magicNum: String,
  @Flag("moduleMagicNum") moduleMagicNum: String,
  exampleService: DoEverythingService,
  multiService: Set[MultiService],
  complexServiceFactory: ComplexServiceFactory,
  objectMapper: FinatraObjectMapper,
  @CamelCaseMapper camelCaseObjectMapper: FinatraObjectMapper,
  jsonPatchOperator: JsonPatchOperator,
  forward: HttpForward,
  httpClient: HttpClient)
    extends Controller {
  assert(exampleService != null)
  assert(multiService != null)

  private val flakyCount = new AtomicInteger()
  private val helloWorldText = "Hello, World!"

  get("/httpclient") { _: Request =>
    val request = RequestBuilder.get("/search?&q=sports")
    httpClient.execute(request)
  }

  get("/example/routing/always") { _: Request =>
    response.ok.body("always response")
  }

  get("/example/routing/always2") { _: Request =>
    response.ok("always response")
  }

  get("/plaintext/?") { _: Request =>
    "Hello, World!"
  }

  prefix("/1.1") {
    get("/plaintext") { _: Request =>
      "Hello, World!"
    }

    post("/foo") { _: Request =>
      "bar"
    }

    filter[ForbiddenFilter].get("/forbiddenByFilter") { _: Request =>
      "ok!"
    }

    filter(new AppendToHeaderFilter("appended", "1"))
      .filter(new AppendToHeaderFilter("appended", "2"))
      .get("/appendMultiplePrefixed") { request: Request =>
        request.headerMap("appended")
      }

    filter(new AppendToHeaderFilter("freestyle", "bang")) {
      get("/freestyleWithHeader") { request: Request =>
        request.headerMap("freestyle")
      }
    }
  }

  prefix("/1.1") {
    prefix("/waterfall") {
      prefix("/users") {
        get("/") { _: Request =>
          "ok!"
        }
      }
    }
  }

  filter[ForbiddenFilter].prefix("/1.1") {
    get("/forbiddenByFilterPrefilter") { _: Request =>
      "ok!"
    }
  }

  filter[ForbiddenFilter] {
    prefix("/1.1") {
      get("/forbiddenByFilterPrefilter-nested") { _: Request =>
        "ok!"
      }
    }
  }

  get("/forwarded") { request: Request =>
    forward(request, "/forwarded/get")
  }

  post("/forwarded") { request: Request =>
    forward(request, "/forwarded/post")
  }

  get("/bytearray") { _: Request =>
    val b = new Array[Byte](20)
    ThreadLocalRandom.current().nextBytes(b)
    b
  }

  get("/inputstream") { _: Request =>
    val b = new Array[Byte](20)
    ThreadLocalRandom.current().nextBytes(b)
    response.ok.body(new ByteArrayInputStream(b))
  }

  get("/useragent") { request: UserAgentRequest =>
    request.agent
  }

  get("/acceptHeaders") { request: AcceptsHeaderRequest =>
    Map(
      "Accept" -> request.accept,
      "Accept-Charset" -> request.acceptCharset,
      "Accept-Charset-Again" -> request.acceptCharsetAgain,
      "Accept-Encoding" -> request.acceptEncoding
    )
  }

  get("/forwardCaseClass") { request: CaseClassWithRequestField =>
    forward(request.request, "/forwarded/get")
  }

  get("/json") { _: Request =>
    response.ok.json("{}".getBytes)
  }

  get("/json2") { _: Request =>
    response.ok.json("{}")
  }

  get("/none") { _: Request =>
    response.ok.body(None)
  }

  get("/bodyunit") { _: Request =>
    response.ok.body(())
  }

  get("/bodynull") { _: Request =>
    response.ok.body(null.asInstanceOf[Any])
  }

  get("/bodyEmptyString") { _: Request =>
    response.ok.body("")
  }

  get("/example/routing/json/:id") { request: Request =>
    ExampleResponse(
      id = request.params("id"),
      name = "bob",
      magic = magicNum,
      moduleMagic = moduleMagicNum
    )
  }

  get("/routeParamGetAll/:id") { request: Request =>
    assert(request.params.isValid)
    request.params("id") +: request.params.getAll("id").toSeq
  }

  get("/column/:key//:*") { request: Request =>
    s"${request.params("key")}/${request.params("*")}"
  }

  post("/foo") { _: Request =>
    "bar"
  }

  post("/longer/post/path/:capture") { _: Request =>
    "bar"
  }

  post("/longer/post/path/with/name/:capture", name = "my_cool_endpoint") { _: Request =>
    "bar"
  }

  post("/formPost") { formPost: FormPostRequest =>
    formPost.name
  }

  post("/echo") { request: Request =>
    response.ok(request.contentString)
  }

  post("/formPostMultipart") { request: Request =>
    RequestUtils.multiParams(request)("file").contentType
  }

  post("/groups/:group_id/users") { user: TestUserWithGroupIdFromRoute =>
    response.created(user)
  }

  post("/multipleRouteParams") { r: MultipleInjectableValueParams =>
    response.created(r)
  }

  post("/caseClassWithRequestField") { _: CaseClassWithRequestField =>
    "hi"
  }

  get("/example/routing/flaky") { _: Request =>
    val num = flakyCount.incrementAndGet()
    if (num == 1)
      response.internalServerError.body("flaky error")
    else if (num == 2)
      throw new ChannelClosedException()
    else if (num == 3)
      throw new ChannelWriteException(new ChannelClosedException())
    else
      response.ok.body("flaky response")
  }

  get("/ok") { _: Request =>
    response.ok.body("ok")
  }

  get("/created") { _: Request =>
    response.created.body("created").location("/foo/123")
  }

  get("/accepted") { _: Request =>
    response.accepted.body("accepted").location("/foo/123")
  }

  get("/accepted2") { _: Request =>
    response.accepted("accepted").location("/foo/123")
  }

  get("/badrequest") { _: Request =>
    response.badRequest.body("bad")
  }

  get("/forbidden") { _: Request =>
    response.forbidden.body("forbidden")
  }

  get("/methodnotallowed") { _: Request =>
    response.methodNotAllowed.body("not allowed")
  }

  get("/unauthorized") { _: Request =>
    response.unauthorized.body("unauthorized")
  }

  get("/unavailable") { _: Request =>
    response.serviceUnavailable.body("unavail")
  }

  get("/conflict") { _: Request =>
    SimpleResponse(Status.Conflict, "conflict")
  }

  get("/redirect") { _: Request =>
    response.temporaryRedirect.location("/foo/123")
  }

  get("/found") { _: Request =>
    response.found.location("/foo/123")
  }

  get("/notfound") { _: Request =>
    response.notFound.body("notfound")
  }

  get("/notfound2") { _: Request =>
    response.notFound("notfound")
  }

  get("/future") { _: Request =>
    Future(response.ok.body("future"))
  }

  get("/true") { _: Request =>
    true
  }

  post("/post") { _: Request =>
    "post"
  }

  delete("/delete") { _: Request =>
    "delete"
  }

  put("/putJson/:id") { request: IdAndNameRequest =>
    request
  }

  put("/putAndPost") { request: Request =>
    "PUT" + request.contentString
  }

  post("/putAndPost") { request: Request =>
    "POST" + request.contentString
  }

  post("/postAndPut") { request: Request =>
    "POST" + request.contentString
  }

  put("/postAndPut") { request: Request =>
    "PUT" + request.contentString
  }

  put("/echo") { request: Request =>
    response.ok(request.contentString)
  }

  get("/testfile") { _: Request =>
    response.ok.file("/testfile.txt")
  }

  get("/testfileWhenNotfound") { _: Request =>
    response.ok.file("/doesntexist.txt")
  }

  get("/index/:*") { request: Request =>
    response.ok.fileOrIndex(request.params("*"), "testindex.html")
  }

  get("/null") { _: Request =>
    null
  }

  get("/empty") { _: Request =>
    ""
  }

  get("/unit") { _: Request =>
    }

  get("/exception") { _: Request =>
    throw new Exception("bad")
  }

  get("/complexpath/:name") { request: Request =>
    val name = request.params("name")
    complexServiceFactory.create(name).execute
  }

  get("/complexquery") { request: Request =>
    val name = request.params("name")
    complexServiceFactory.create(name).execute
  }

  get("/notfoundexception") { _: Request =>
    throw NotFoundException()
  }

  get("/notfoundexception2") { _: Request =>
    throw new NotFoundException("foo")
  }

  get("/servererrorexception") { _: Request =>
    throw InternalServerErrorException()
  }

  get("/serviceunavailableexception") { _: Request =>
    throw ServiceUnavailableException()
  }

  get("/serviceunavailableexception2") { _: Request =>
    throw ServiceUnavailableException.plainText("foo")
  }

  get("/responsebuilder_status_code") { _: Request =>
    response.status(503)
  }

  get("/responsebuilder_status") { _: Request =>
    response.status(Status.ServiceUnavailable)
  }

  get("/pathUrl") { request: Request =>
    RequestUtils.pathUrl(request)
  }

  get("/path") { request: Request =>
    RequestUtils.pathUrl(request)
  }

  put("/put/:id") { request: Request =>
    request.getParam("id") + "_" + request.contentString
  }

  put("/put_route_param/:id") { idRequest: IdRequest =>
    idRequest.id + "_" + idRequest.request.contentString
  }

  put("/put_route_param_and_name/:id") { idRequest: IdAndNameRequest =>
    idRequest.id + "_" + idRequest.name
  }

  put("/put_id_ignoring_body/:id") { req: IdRequestIgnoringBody =>
    req.id
  }

  put("/put_id_not_ignoring_body/:id") { req: IdRequestNotIgnoringBody =>
    req.id
  }

  get("/implicitOkAndException") { request: Request =>
    if (request.params.contains("hi"))
      "hello"
    else
      throw BadRequestException()
  }

  get("/BadRequestException") { _: Request =>
    throw BadRequestException.plainText("foo")
  }

  get("/slow") { _: Request =>
    println("SLOW ROUTE CALLED")
    Thread.sleep(9999)
    "slow"
  }

  get("/nack") { _: Request =>
    Future.exception(Failure.rejected("overloaded!"))
  }

  get("/builderCreatedWithHeader") { _: Request =>
    response.created.header("a", "b").header("Location", "http://foo.com/1")
  }

  get("/requestInjectionsNotFound") { request: RequestWithNotFoundInjections =>
    request.fooClass
  }

  get("/stringMap") { _: Request =>
    Map("message" -> helloWorldText)
  }

  post("/requestInjectionsNotAvailable") { request: Request =>
    val requestWithInjections = objectMapper.parseMessageBody[RequestWithInjections](request)
    requestWithInjections.id.id +
      requestWithInjections.id2.map(_.id).getOrElse(0L) +
      requestWithInjections.id3.getOrElse(0) +
      requestWithInjections.id4.getOrElse(0)
  }

  get("/requestInjection") { request: RequestWithInjections =>
    request
  }

  get("/requestInjectionsNotAvailable") { request: Request =>
    val requestWithInjections = objectMapper.parseMessageBody[RequestWithInjections](request)
    requestWithInjections.id.id +
      requestWithInjections.id2.map(_.id).getOrElse(0L) +
      requestWithInjections.id3.getOrElse(0) +
      requestWithInjections.id4.getOrElse(0)
  }

  get("/users/:name") { r: Request =>
    val user = TestUser(r.params("name"))
    response.ok.body(user) // test ResponseBuilder
  }

  post("/users") { user: TestUser =>
    user.name
  }

  post("/userWithMissingField") { user: TestUser =>
    user.name
  }

  post("/userWithInvalidFieldValidation") { user: TestUserWithInvalidFieldValidation =>
    user.name
  }

  post("/userWithInvalidMethodValidation") { user: TestUserWithInvalidMethodValidation =>
    user.name
  }

  post("/userWithMessageBodyReader") { user: DomainTestUser =>
    user
  }

  get("/array") { _: Request =>
    Array("a", "b")
  }

  get("/set") { _: Request =>
    response.ok.body(SortedSet("a", "b"))
  }

  get("/seq") { _: Request =>
    Seq("a", "b")
  }

  options("/options") { _: Request =>
    "options"
  }

  head("/head") { _: Request =>
    response.conflict
  }

  patch("/patch") { _: Request =>
    "patch"
  }

  patch("/jsonPatch") { jsonPatch: JsonPatch =>
    val testCase = ExampleCaseClass("world")
    val originalJson = jsonPatchOperator.toJsonNode[ExampleCaseClass](testCase)
    JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
  }

  patch("/jsonPatch/nested") { jsonPatch: JsonPatch =>
    val testCase = Level1CaseClass(Level2CaseClass(ExampleCaseClass("world")))
    val originalJson = jsonPatchOperator.toJsonNode[Level1CaseClass](testCase)
    JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
  }

  patch("/jsonPatch/level3") { jsonPatch: JsonPatch =>
    val testCase = Level0CaseClass(Level1CaseClass(Level2CaseClass(ExampleCaseClass("world"))))
    val originalJson = jsonPatchOperator.toJsonNode[Level0CaseClass](testCase)
    JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
  }

  patch("/jsonPatch/string") { jsonPatch: JsonPatch =>
    val testCase = """{"hello":"world"}"""
    val originalJson = jsonPatchOperator.toJsonNode(testCase)
    JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
  }

  patch("/jsonPatch/nonleaf") { jsonPatch: JsonPatch =>
    val testCase = RootCaseClass(
      DuoCaseClass(
        DuoStringCaseClass("left-left", "left-right"),
        DuoStringCaseClass("right-left", "right-right")
      )
    )
    val originalJson = jsonPatchOperator.toJsonNode(testCase)
    JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
  }

  patch("/jsonPatch/innerSeqCaseClass") { jsonPatch: JsonPatch =>
    val testCase = InnerSeqCaseClass(bears = Seq("grizzly", "polar"))
    val originalJson = jsonPatchOperator.toJsonNode(testCase)
    JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
  }

  patch("/jsonPatch/nestedSeqCaseClass") { jsonPatch: JsonPatch =>
    val testCase = NestedSeqCaseClass(
      animalFamilies = Seq(
        AnimalFamily(name = "ursidae", animals = Seq("grizzly", "polar"))
      )
    )
    val originalJson = jsonPatchOperator.toJsonNode(testCase)
    JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
  }

  patch("/echo") { request: Request =>
    response.ok(request.contentString)
  }

  get("/HttpResponseException") { _: Request =>
    val exception = new HttpResponseException(response.conflict("conflicted"))
    warn(exception.getMessage)
    throw exception
  }

  get("/toFutureException") { _: Request =>
    response.conflict("conflicted").toFutureException
  }

  /* Admin routes */

  get("/admin/foo") { _: Request =>
    response.ok("on the external interface")
  }

  get("/admin/external/filtered") { request: Request =>
    request.headerMap("test")
  }

  get("/admin/testme", admin = true) { _: Request =>
    response.ok("ping")
  }

  get("/admin/finatra/internal/route", admin = true) { _: Request =>
    response.ok("ping")
  }

  get("/HttpExceptionPlain") { _: Request =>
    throw HttpException.plainText(Status.Created, "foo")
  }

  get("/HttpExceptionErrors") { _: Request =>
    val e = HttpException(Status.Created, "foo1", "foo2")
    warn(e + " " + e.hashCode)
    throw e
  }

  get("/NotFoundException") { _: Request =>
    throw NotFoundException("foo1")
  }

  get("/ConflictException") { _: Request =>
    throw ConflictException("foo1")
  }

  get("/ConflictException2") { _: Request =>
    throw ConflictException.plainText("foo1")
  }

  get("/ConflictException3") { _: Request =>
    throw new ConflictException("foo1")
  }

  get("/ForbiddenException") { _: Request =>
    throw ForbiddenException.plainText("foo1")
  }

  get("/ForbiddenException2") { _: Request =>
    throw ForbiddenException("foo1")
  }

  get("/NotAcceptableException") { _: Request =>
    throw NotAcceptableException("foo1")
  }

  get("/NotAcceptableException2") { _: Request =>
    throw NotAcceptableException.plainText("foo1")
  }

  get("/InternalServerErrorExceptionPlain") { _: Request =>
    throw InternalServerErrorException.plainText("foo1")
  }

  get("/UnserializableClassField") { r: RequestWithInjectedMapper =>
    r
  }

  get("/RequestWithQueryParamSeqString") { r: RequestWithQueryParamSeqString =>
    ResponseOfQueryParamSeqString(r.foo.map(_ + 1).seq)
  }

  get("/RequestWithQueryParamSeqLong") { r: RequestWithQueryParamSeqLong =>
    ResponseOfQueryParamSeqLong(r.foo.map(_ + 1).seq)
  }

  get("/RequestWithCommaSeparatedQueryParamSeqLong") {
    r: RequestWithCommaSeparatedQueryParamSeqLong =>
      ResponseOfQueryParamSeqLong(r.foo.map(_ + 1).seq)
  }

  get("/RequestWithUselessCommaSeparatedQueryParamLong") {
    r: RequestWithUselessCommaSeparatedQueryParamLong =>
      ResponseOfQueryParamSeqLong(Seq(r.foo + 1))
  }

  get("/FooException/:id") { r: Request =>
    throw new FooException(r.params("id"))
  }

  get("/BarException") { _: Request =>
    throw new BarException
  }

  get("/BazException") { _: Request =>
    throw new BazException
  }

  get("/FooBarBazException") { _: Request =>
    // pretend a JSON parse exception happened resulting in a thrown FooBarBazException
    // a mapper for this should be added by the DoEverythingInstalledExceptionMappersModule
    throw new FooBarBazException
  }

  get("/NoSuchMethodException") { _: Request =>
    throw new NoSuchMethodException
  }

  get("/users") { r: UsersRequest =>
    r
  }

  post("/multipartParamsEcho") { r: Request =>
    RequestUtils.multiParams(r).keys
  }

  put("/multipartParamsPutEcho") { r: Request =>
    RequestUtils.multiParams(r).keys
  }

  get("/CaseClassWithIntQueryParam") { r: RequestWithIntQueryParams =>
    r.param
  }

  get("/CaseClassWithShortQueryParam") { r: RequestWithShortQueryParams =>
    r.param
  }

  get("/RequestWithBooleanQueryParams") { r: RequestWithBooleanQueryParams =>
    r.param
  }

  get("/RequestWithBooleanQueryParam") { r: RequestWithBooleanQueryParam =>
    r.param
  }

  get("/RequestWithBooleanNamedQueryParam") { r: RequestWithBooleanNamedQueryParam =>
    r.param
  }

  get("/RequestWithOptionBooleanQueryParam") { r: RequestWithOptionBooleanQueryParam =>
    "Hi " + r.param
  }

  get("/CaseClassWithCaseClassQueryParam") { r: RequestWithCaseClassQueryParams =>
    r.param
  }

  get("/RequestWithDefaultedQueryParamSeqString") { r: RequestWithDefaultedQueryParamSeqString =>
    Map("foo" -> r.foo)
  }

  get("/RequestWithDefaultQueryParam") { r: RequestWithDefaultQueryParam =>
    Map("param" -> r.param)
  }

  post("/RequestWithSeqWrappedString") { r: RequestWithSeqWrappedString =>
    r.value
  }

  post("/SomethingStreamedRequest.json") { r: SomethingStreamedRequest =>
    s"${r.somethingId}/${r.field1.get}/${r.field2.get}"
  }

  post("/SomethingStreamedRequestAsJsonResponse.json") { r: SomethingStreamedRequest =>
    SomethingStreamedResponse(r.somethingId, r.field1, r.field2)
  }

  get("/camelCaseJson") { _: Request =>
    camelCaseObjectMapper.writeValueAsString(Map("firstName" -> "Bob"))
  }

  post("/createUser") { user: CreateUserRequest =>
    response.created.location(s"/users/${user.requestId}")
  }

  post("/arrayElementValidation") { request: TestCaseClassWithSeqOfValidatedWrappedLongs =>
    s"Size of input array: ${request.seq.size}"
  }

  //needed to avoid colliding with Logging#trace :-/
  trace[Request, String]("/trace") { _: Request =>
    "trace 123"
  }

  filter[ForbiddenFilter].get("/forbiddenByFilter") { _: Request =>
    "ok!"
  }

  get("/forwardToForbidden") { request: Request =>
    forward(request, "/forwarded/forbiddenByFilter")
  }

  filter[IdentityFilter]
    .filter[IdentityFilter]
    .filter(new AppendToHeaderFilter("test", "4"))
    .filter[IdentityFilter]
    .get("/multiFilterAppend") { request: Request =>
      request.headerMap("test")
    }

  filter[IdentityFilter]
    .filter[IdentityFilter]
    .filter[IdentityFilter]
    .get("/multiIdentityFilterAppend") { _: Request =>
      "ok!"
    }

  get("/non_case_class") { _: Request =>
    new NonCaseClass
  }

  get("/bytes") { _: Request =>
    "Steve".getBytes(StandardCharsets.UTF_8)
  }

  any("/anyMethod") { request: Request =>
    // only support GET, HEAD or TRACE here
    request.method match {
      case _ @(Method.Get | Method.Head | Method.Trace) =>
        response.ok
      case _ =>
        response.methodNotAllowed
    }
  }

  get("/millis") { r: ClassWithQueryParamDateTime =>
    r.dateTime.getMillis
  }

  post("/localDateRequest") { _: TestCaseClassWithLocalDate =>
    response.ok
  }

  post("/invalidValidationRequest") { _: InvalidValidationRequest =>
    response.ok
  }

  post("/invalidValidationRequestWithCause") { request: Request =>
    try {
      response.ok(objectMapper.parse[InvalidValidationRequest](request.contentString))
    } catch {
      case e: IllegalArgumentException =>
        // want to return the actual error to the client for testing against this case
        response.internalServerError(e.getMessage)
    }
  }

  post("/seq") { r: Seq[String] =>
    r
  }

  post("/map") { r: Map[String, String] =>
    r
  }

  post("/seq2") { r: Request =>
    objectMapper.parse[Seq[String]](r.contentString)
  }

  post("/seqCaseClass") { r: Seq[TestUser] =>
    r
  }

  post("/ctu-time") { r: DomainTwitterTimeRequest =>
    r
  }
}

case class MultipleInjectableValueParams(@RouteParam @QueryParam id: String)

case class CaseClassWithRequestField(request: Request)

case class ClassWithQueryParamDateTime(@QueryParam dateTime: DateTime)

class NonCaseClass {
  val name = "Bob"
  val age = 21
}
