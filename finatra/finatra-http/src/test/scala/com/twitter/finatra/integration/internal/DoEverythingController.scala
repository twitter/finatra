package com.twitter.finatra.integration.internal

import com.twitter.finagle.http.{Status, Request => FinagleRequest}
import com.twitter.finagle.{ChannelClosedException, ChannelWriteException}
import com.twitter.finatra.annotations.{Flag, Mustache}
import com.twitter.finatra.exceptions.{BadRequestException, InternalServerErrorException, NotFoundException, ServiceUnavailableException}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.annotations.{FormParam, JsonInject, QueryParam}
import com.twitter.finatra.json.internal.caseclass.wrapped.JsonWrappedValue
import com.twitter.finatra.request.RequestUtils
import com.twitter.finatra.response._
import com.twitter.finatra.{Controller, Request}
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
      id = request.routeParams("id"),
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

  get("/index/:*") { request: Request =>
    response.ok.fileOrIndex(
      request.routeParams("*"),
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
    val name = request.routeParams("name")
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

  get("/finagleRequest") { request: FinagleRequest =>
    response.
      ok.
      header("a", "b")
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

  post("/requestInjectionsNotAvailable") { finatraRequest: Request =>
    val request = objectMapper.parse[RequestWithInjections](finatraRequest)
    request.id.id +
      request.id2.map(_.id).getOrElse(0L) +
      request.id3.getOrElse(0) +
      request.id4.getOrElse(0)
  }

  get("/requestInjection") { request: RequestWithInjections =>
    request.id.id +
      request.id2.map(_.id).getOrElse(0L) +
      request.id3.getOrElse(0) +
      request.id4.getOrElse(0)
  }

  get("/requestInjectionsNotAvailable") { finatraRequest: Request =>
    val request = objectMapper.parse[RequestWithInjections](finatraRequest)
    request.id.id +
      request.id2.map(_.id).getOrElse(0L) +
      request.id3.getOrElse(0) +
      request.id4.getOrElse(0)
  }

  post("/users") { user: TestUser =>
    user.name
  }

  get("/array") { r: Request =>
    Array("a", "b")
  }

  get("/set") { r: Request =>
    SortedSet("a", "b")
  }

  get("/seq") { r: Request =>
    Seq("a", "b")
  }
}

case class FormPostRequest(
  @FormParam name: String,
  @FormParam age: Int)

@Mustache("testuser")
case class TestUserView(
  age: Int,
  name: String,
  friends: Seq[String])

case class RequestWithInjections(
  @QueryParam id: UserId,
  @QueryParam id2: Option[UserId],
  @QueryParam id3: Option[Int],
  @QueryParam id4: Option[Int])

case class RequestWithNotFoundInjections(
  @JsonInject fooClass: FooClass)

case class UserId(
  id: Long)
  extends JsonWrappedValue[Long]

case class FooClass(id: String)

case class TestUser(name: String)