package com.twitter.finatra

import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.exceptions.NotFoundException
import com.twitter.finatra.guice.TwitterTestInjector
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.marshalling.CallbackConvertor
import com.twitter.finatra.response.SimpleResponse
import com.twitter.finatra.test.Test
import com.twitter.finatra.twitterserver.modules.{MessageBodyModule, MustacheModule}
import com.twitter.util.{Await, Future}

class CallbackConverterIntegrationTest extends Test {

  val injector = TwitterTestInjector(
    MessageBodyModule, FinatraJacksonModule, MustacheModule)

  val callbackConverter = injector.instance[CallbackConvertor]

  val request = mock[Request]
  val ford = Car("Ford")

  "Future Some String" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSomeString),
      withBody = "hello")
  }

  "Future None String" in {
    val convertedFunc = callbackConverter.convertToFutureResponse(futureNoneString)
    intercept[NotFoundException] {
      Await.result(convertedFunc(request))
    }
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

  "Future Ok String" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureOk),
      withBody = "bob")
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
    val convertedFunc = callbackConverter.convertToFutureResponse(noneCallback)
    intercept[NotFoundException] {
      Await.result(convertedFunc(request))
    }
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

  def futureOk(request: Request): Future[Response] = {
    Future(SimpleResponse(Ok, "bob"))
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

}

case class Car(name: String) extends CarTrait

trait CarTrait {
  val name: String
}