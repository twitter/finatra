package com.twitter.finatra.marshalling

import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.internal.marshalling.CallbackConverter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.modules.{CallbackConverterModule, LocalDocRootFlagModule, MessageBodyModule, MustacheModule}
import com.twitter.finatra.response.SimpleResponse
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Mockito, Test}
import com.twitter.util.{Await, Future}
import org.jboss.netty.handler.codec.http.HttpResponseStatus

class CallbackConverterIntegrationTest extends Test with Mockito {

  val injector = TestInjector(
    new MessageBodyModule, FinatraJacksonModule,
    MustacheModule, CallbackConverterModule, LocalDocRootFlagModule)

  val callbackConverter = injector.instance[CallbackConverter]

  val request = mock[Request]
  val ford = Car("Ford")
  val okResponse = SimpleResponse(Ok, "bob")

  "Future Some String" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSomeString),
      withBody = "hello")
  }

  "Future None String" in {
    assertStatus(
      callbackConverter.convertToFutureResponse(futureNoneString),
      expectedStatus = NotFound)
  }

  "Future Some Product" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSomeProduct),
      withBody = """{"name":"Ford"}""")
  }

  "Future Some Trait" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSomeTrait),
      withBody = """{"name":"Ford"}""")
  }

  "Future String" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureString),
      withBody = "bob")
  }

  "Future Response" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureResponse),
      withBody = "bob")
  }

  "Future Some Response" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSomeResponse),
      withBody = "bob")
  }

  "Future None Response" in {
    assertStatus(
      callbackConverter.convertToFutureResponse(futureNoneResponse),
      expectedStatus = NotFound)
  }

  "Future Seq String" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSeqString),
      withBody = """["bob"]""")
  }

  "Future Seq Car" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSeqCar),
      withBody = """[{"name":"Ford"}]""")
  }

  "Future Seq CarTrait" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSeqCarTrait),
      withBody = """[{"name":"Ford"}]""")
  }

  "Object" in {
    assertOk(
      callbackConverter.convertToFutureResponse(objectCallback),
      withBody = """asdf""")
  }

  "None" in {
    assertStatus(
      callbackConverter.convertToFutureResponse(noneCallback),
      expectedStatus = NotFound)
  }

  "Some" in {
    assertOk(
      callbackConverter.convertToFutureResponse(someCallback),
      withBody = """asdf""")
  }

  "Null" in {
    pending
    assertOk(
      callbackConverter.convertToFutureResponse(nullCallback),
      withBody = "")
  }

  def objectCallback(request: Request): Object = {
    "asdf"
  }

  def noneCallback(request: Request): Option[String] = {
    None
  }

  def someCallback(request: Request): Option[String] = {
    Some("asdf")
  }

  def nullCallback(request: Request) = {
    null
  }

  def futureSomeString(request: Request): Future[Option[String]] = {
    Future(Some("hello"))
  }

  def futureNoneString(request: Request): Future[Option[String]] = {
    Future(None)
  }

  def futureSomeProduct(request: Request): Future[Option[Car]] = {
    Future(Some(ford))
  }

  def futureSomeTrait(request: Request): Future[Option[CarTrait]] = {
    Future(Some(ford))
  }

  def futureString(request: Request): Future[String] = {
    Future("bob")
  }

  def futureResponse(request: Request): Future[Response] = {
    Future(okResponse)
  }

  def futureSomeResponse(request: Request): Future[Option[Response]] = {
    Future(Some(okResponse))
  }

  def futureNoneResponse(request: Request): Future[Option[Response]] = {
    Future.None
  }

  def futureSeqString(request: Request): Future[Seq[String]] = {
    Future(Seq("bob"))
  }

  def futureSeqCar(request: Request): Future[Seq[Car]] = {
    Future(Seq(ford))
  }

  def futureSeqCarTrait(request: Request): Future[Seq[CarTrait]] = {
    Future(Seq(ford))
  }

  private def assertOk(response: Response, expectedBody: String) {
    response.status should equal(Status.Ok)
    response.contentString should equal(expectedBody)
  }

  private def assertOk(convertedFunc: (Request) => Future[Response], withBody: String) {
    val response = Await.result(convertedFunc(request))
    assertOk(response, withBody)
  }

  private def assertStatus(convertedFunc: (Request) => Future[Response], expectedStatus: HttpResponseStatus) {
    val response = Await.result(convertedFunc(request))
    response.status should equal(expectedStatus)
  }
}

case class Car(name: String) extends CarTrait

trait CarTrait {
  val name: String
}
