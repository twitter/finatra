package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.{Method, Request, Status}
import com.twitter.finagle.{ChannelClosedException, ChannelWriteException}
import com.twitter.finatra.annotations.CamelCaseMapper
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.exceptions._
import com.twitter.finatra.http.marshalling.mustache.MustacheService
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.finatra.http.response._
import com.twitter.finatra.http.tests.integration.doeverything.main.domain._
import com.twitter.finatra.http.tests.integration.doeverything.main.exceptions._
import com.twitter.finatra.http.tests.integration.doeverything.main.filters.ForbiddenFilter
import com.twitter.finatra.http.tests.integration.doeverything.main.services.{ComplexServiceFactory, DoEverythingService, MultiService}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.inject.annotations.Flag
import com.twitter.util.Future
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.collection.SortedSet

class DoEverythingController @Inject()(
  @Flag("magicNum") magicNum: String,
  @Flag("moduleMagicNum") moduleMagicNum: String,
  exampleService: DoEverythingService,
  multiService: Set[MultiService],
  complexServiceFactory: ComplexServiceFactory,
  objectMapper: FinatraObjectMapper,
  @CamelCaseMapper camelCaseObjectMapper: FinatraObjectMapper,
  mustacheService: MustacheService)
  extends Controller {

  private val flakyCount = new AtomicInteger()

  get("/example/routing/always") { request: Request =>
    response.ok.body("always response")
  }

  get("/example/routing/always2") { request: Request =>
    response.ok("always response")
  }

  get("/plaintext") { request: Request =>
    "Hello, World!"
  }

  get("/bytearray") { request: Request =>
    val b = new Array[Byte](20)
    ThreadLocalRandom.current().nextBytes(b)
    b
  }

  get("/inputstream") { request: Request =>
    val b = new Array[Byte](20)
    ThreadLocalRandom.current().nextBytes(b)
    response.ok.body(new ByteArrayInputStream(b))
  }

  get("/useragent") { request: UserAgentRequest =>
    request.`user-agent`
  }

  get("/json") { request: Request =>
    response.ok.json("{}".getBytes)
  }

  get("/admin/foo") { request: Request =>
    response.ok("Hanging out on the externl interface")
  }

  get("/json2") { request: Request =>
    response.ok.json("{}")
  }

  get("/none") { request: Request =>
    response.ok.body(None)
  }

  get("/bodyunit") { request: Request =>
    response.ok.body(())
  }

  get("/bodynull") { request: Request =>
    response.ok.body(null.asInstanceOf[Any])
  }

  get("/bodyEmptyString") { request: Request =>
    response.ok.body("")
  }

  get("/example/routing/json/:id") { request: Request =>
    ExampleResponse(
      id = request.params("id"),
      name = "bob",
      magic = magicNum,
      moduleMagic = moduleMagicNum)
  }

  get("/routeParamGetAll/:id") { request: Request =>
    assert(request.params.isValid)
    request.params("id") +: request.params.getAll("id").toSeq
  }

  get("/column/:key//:*") { request: Request =>
    s"${request.params("key")}/${request.params("*")}"
  }

  post("/foo") { request: Request =>
    "bar"
  }

  post("/longer/post/path/:capture") { request: Request =>
    "bar"
  }

  post("/longer/post/path/with/name/:capture", name = "my_cool_endpoint") { request: Request =>
    "bar"
  }

  post("/formPost") { formPost: FormPostRequest =>
    formPost.name
  }

  post("/formPostView") { formPost: FormPostRequest =>
    TestUserView(
      formPost.age,
      formPost.name,
      Seq("user1", "user2"))
  }

  get("/getView") { request: Request =>
    TestUserView(
      request.params.getInt("age").get,
      request.params("name"),
      Seq("user1", "user2"))
  }

  post("/echo") { request: Request =>
    response.ok(request.contentString)
  }

  post("/formPostViewFromBuilderView") { formPost: FormPostRequest =>
    response.ok.view(
      "testuser2.mustache",
      TestUserView(formPost.age, formPost.name, Seq("user1", "user2")))
  }

  post("/formPostViewFromBuilderHtml") { formPost: FormPostRequest =>
    response.ok.html(
      TestUserView(formPost.age, formPost.name, Seq("user1", "user2")))
  }

  post("/formPostViewFromBuilderCreatedView") { formPost: FormPostRequest =>
    response.created.
      location("/foo/1").
      view(
        "testuser2.mustache",
        TestUserView(formPost.age, formPost.name, Seq("user1", "user2")))
  }

  post("/formPostViewFromBuilderCreatedHtml") { formPost: FormPostRequest =>
    response.created.
      location("/foo/1").
      html(
        TestUserView(formPost.age, formPost.name, Seq("user1", "user2")))
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

  post("/caseClassWithRequestField") { r: CaseClassWithRequestField =>
    "hi"
  }

  get("/example/routing/flaky") { request: Request =>
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

  get("/ok") { request: Request =>
    response.ok.body("ok")
  }

  get("/created") { request: Request =>
    response.created.body("created").location("/foo/123")
  }

  get("/accepted") { request: Request =>
    response.accepted.body("accepted").location("/foo/123")
  }

  get("/accepted2") { request: Request =>
    response.accepted("accepted").location("/foo/123")
  }

  get("/badrequest") { request: Request =>
    response.badRequest.body("bad")
  }

  get("/forbidden") { request: Request =>
    response.forbidden.body("forbidden")
  }

  get("/methodnotallowed") { request: Request =>
    response.methodNotAllowed.body("not allowed")
  }

  get("/unauthorized") { request: Request =>
    response.unauthorized.body("unauthorized")
  }

  get("/unavailable") { request: Request =>
    response.serviceUnavailable.body("unavail")
  }

  get("/conflict") { request: Request =>
    SimpleResponse(Status.Conflict, "conflict")
  }

  get("/redirect") { request: Request =>
    response.temporaryRedirect.location("/foo/123")
  }

  get("/found") { request: Request =>
    response.found.location("/foo/123")
  }

  get("/notfound") { request: Request =>
    response.notFound.body("notfound")
  }

  get("/notfound2") { request: Request =>
    response.notFound("notfound")
  }

  get("/future") { request: Request =>
    Future(response.ok.body("future"))
  }

  get("/true") { request: Request =>
    true
  }

  post("/post") { request: Request =>
    "post"
  }

  delete("/delete") { request: Request =>
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

  get("/testfile") { request: Request =>
    response.ok.file("/testfile.txt")
  }

  get("/testfileWhenNotfound") { request: Request =>
    response.ok.file("/doesntexist.txt")
  }

  get("/index/:*") { request: Request =>
    response.ok.fileOrIndex(
      request.params("*"),
      "testindex.html")
  }

  get("/null") { request: Request =>
    null
  }

  get("/empty") { request: Request =>
    ""
  }

  get("/unit") { request: Request =>
  }

  get("/exception") { request: Request =>
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

  get("/notfoundexception") { request: Request =>
    throw NotFoundException()
  }

  get("/notfoundexception2") { request: Request =>
    throw new NotFoundException("foo")
  }

  get("/servererrorexception") { request: Request =>
    throw InternalServerErrorException()
  }

  get("/serviceunavailableexception") { request: Request =>
    throw ServiceUnavailableException()
  }

  get("/serviceunavailableexception2") { request: Request =>
    throw ServiceUnavailableException.plainText("foo")
  }

  get("/responsebuilder_status_code") { request: Request =>
    response.status(503)
  }

  get("/responsebuilder_status") { request: Request =>
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

  get("/BadRequestException") { request: Request =>
    throw BadRequestException.plainText("foo")
  }

  get("/slow") { request: Request =>
    println("SLOW ROUTE CALLED")
    Thread.sleep(9999)
    "slow"
  }

  get("/builderCreatedWithHeader") { request: Request =>
    response.
      created.
      header("a", "b").
      header("Location", "http://foo.com/1")
  }


  get("/requestInjectionsNotFound") { request: RequestWithNotFoundInjections =>
    request.fooClass
  }

  post("/requestInjectionsNotAvailable") { request: Request =>
    val requestWithInjections = objectMapper.parse[RequestWithInjections](request)
    requestWithInjections.id.id +
      requestWithInjections.id2.map(_.id).getOrElse(0L) +
      requestWithInjections.id3.getOrElse(0) +
      requestWithInjections.id4.getOrElse(0)
  }

  get("/requestInjection") { request: RequestWithInjections =>
    request
  }

  get("/requestInjectionsNotAvailable") { request: Request =>
    val requestWithInjections = objectMapper.parse[RequestWithInjections](request)
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

  get("/array") { r: Request =>
    Array("a", "b")
  }

  get("/set") { r: Request =>
    response.ok.body(SortedSet("a", "b"))
  }

  get("/seq") { r: Request =>
    Seq("a", "b")
  }

  options("/options") { r: Request =>
    "options"
  }

  head("/head") { r: Request =>
    response.conflict
  }

  patch("/patch") { r: Request =>
    "patch"
  }

  patch("/echo") { request: Request =>
    response.ok(request.contentString)
  }

  get("/HttpResponseException") { r: Request =>
    val exception = new HttpResponseException(response.conflict("conflicted"))
    warn(exception.getMessage)
    throw exception
  }

  get("/toFutureException") { r: Request =>
    response.conflict("conflicted").toFutureException
  }

  get("/admin/finatra/foo") { r: Request =>
    "bar"
  }

  get("/HttpExceptionPlain") { r: Request =>
    throw HttpException.plainText(Status.Created, "foo")
  }

  get("/HttpExceptionErrors") { r: Request =>
    val e = HttpException(Status.Created, "foo1", "foo2")
    warn(e + " " + e.hashCode)
    throw e
  }

  get("/NotFoundException") { r: Request =>
    throw NotFoundException("foo1")
  }

  get("/ConflictException") { r: Request =>
    throw ConflictException("foo1")
  }

  get("/ConflictException2") { r: Request =>
    throw ConflictException.plainText("foo1")
  }

  get("/ConflictException3") { r: Request =>
    throw new ConflictException("foo1")
  }

  get("/ForbiddenException") { r: Request =>
    throw ForbiddenException.plainText("foo1")
  }

  get("/ForbiddenException2") { r: Request =>
    throw ForbiddenException("foo1")
  }

  get("/NotAcceptableException") { r: Request =>
    throw NotAcceptableException("foo1")
  }

  get("/NotAcceptableException2") { r: Request =>
    throw NotAcceptableException.plainText("foo1")
  }

  get("/InternalServerErrorExceptionPlain") { r: Request =>
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

  get("/FooException/:id") { r: Request =>
    throw new FooException(r.params("id"))
  }

  get("/BarException") { r: Request =>
    throw new BarException
  }

  get("/BazException") { r: Request =>
    throw new BazException
  }

  get("/NoSuchMethodException") { r: Request =>
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

  get("/RequestWithOptionBooleanQueryParam") { r: RequestWithOptionBooleanQueryParam =>
    "Hi " + r.param
  }

  get("/CaseClassWithCaseClassQueryParam") { r: RequestWithCaseClassQueryParams =>
    r.param
  }

  post("/SomethingStreamedRequest.json") { r: SomethingStreamedRequest =>
    s"${r.somethingId}/${r.field1.get}/${r.field2.get}"
  }

  post("/SomethingStreamedRequestAsJsonResponse.json") { r: SomethingStreamedRequest =>
    SomethingStreamedResponse(r.somethingId, r.field1, r.field2)
  }

  get("/camelCaseJson") { request: Request =>
    camelCaseObjectMapper.writeValueAsString(Map("firstName" -> "Bob"))
  }

  post("/createUser") { user: CreateUserRequest =>
    response.created.location(s"/users/${user.requestId}")
  }

  post("/arrayElementValidation") { request: TestCaseClassWithSeqOfValidatedWrappedLongs =>
    s"Size of input array: ${request.seq.size}"
  }

  //needed to avoid colliding with Logging#trace :-/
  trace[Request, String]("/trace") { r: Request =>
    "trace 123"
  }

  filter[ForbiddenFilter].get("/forbiddenByFilter") { r: Request =>
    "ok!"
  }

  get("/testClassWithHtml") { r: Request =>
    val testUser = TestUserView(
      28,
      "Bob Smith",
      Seq("user1", "user2"))

    TestCaseClassWithHtml(
      address = "123 Main St. Anywhere, CA US 90210",
      phone = "+12221234567",
      renderedHtml = xml.Utility.escape(mustacheService.createString("testHtml.mustache", testUser)))
  }

  get("/non_case_class") { r: Request =>
    new NonCaseClass
  }

  get("/bytes") { r: Request =>
    "Steve".getBytes(StandardCharsets.UTF_8)
  }

  any("/anyMethod") { request: Request =>
    // only support GET, HEAD or TRACE here
    request.method match {
      case m @ (Method.Get | Method.Head | Method.Trace) =>
        response.ok
      case _ =>
        response.methodNotAllowed
    }
  }
}

case class MultipleInjectableValueParams(
  @RouteParam @QueryParam id: String)

case class CaseClassWithRequestField(
  @Inject request: Request)

class NonCaseClass {
  val name = "Bob"
  val age = 21
}
