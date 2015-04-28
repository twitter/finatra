package com.twitter.finatra.http.integration.doeverything.main.controllers

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finagle.{ChannelClosedException, ChannelWriteException}
import com.twitter.finatra.annotations.Flag
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.exceptions._
import com.twitter.finatra.http.integration.doeverything.main.domain._
import com.twitter.finatra.http.integration.doeverything.main.exceptions._
import com.twitter.finatra.http.integration.doeverything.main.services.{ComplexServiceFactory, DoEverythingService, MultiService}
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.finatra.http.response._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.collection.SortedSet

class DoEverythingController @Inject()(
  @Flag("magicNum") magicNum: String,
  @Flag("moduleMagicNum") moduleMagicNum: String,
  exampleService: DoEverythingService,
  multiService: Set[MultiService],
  complexServiceFactory: ComplexServiceFactory,
  objectMapper: FinatraObjectMapper)
  extends Controller {

  private val flakyCount = new AtomicInteger()

  get("/example/routing/always") { request: Request =>
    response.ok.body("always response")
  }

  get("/example/routing/json/:id") { request: Request =>
    ExampleResponse(
      id = request.params("id"),
      name = "bob",
      magic = magicNum,
      moduleMagic = moduleMagicNum)
  }

  post("/foo") { request: Request =>
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

  post("/groups/:group_id/users") { user: TestUserWithGroupIdFromRoute =>
    response.created(user)
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

  get("/methodnotallowed") { request: Request =>
    response.methodNotAllowed.body("not allowed")
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

  get("/servererrorexception") { request: Request =>
    throw InternalServerErrorException()
  }

  get("/serviceunavailableexception") { request: Request =>
    throw ServiceUnavailableException()
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

  put("/put") { request: Request =>
    request.contentString
  }

  get("/implicitOkAndException") { request: Request =>
    if (request.params.contains("hi"))
      "hello"
    else
      throw BadRequestException()
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
    request.id.id +
      request.id2.map(_.id).getOrElse(0L) +
      request.id3.getOrElse(0) +
      request.id4.getOrElse(0)
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

  delete("/delete") { r: Request =>
    "delete"
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

  get("/HttpResponseException") { r: Request =>
    throw new HttpResponseException(
      response.conflict("conflicted"))
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

  get("/NotAcceptableException") { r: Request =>
    throw NotAcceptableException("foo1")
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
}
