package com.twitter.finatra.http.tests.marshalling

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Method => HttpMethod, Request, Response, Status}
import com.twitter.finatra.http.internal.marshalling.CallbackConverter
import com.twitter.finatra.http.modules.{DocRootModule, MessageBodyModule, MustacheModule}
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.conversions.buf._
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.{Mockito, IntegrationTest}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Await, Future}
import scala.concurrent.{Future => ScalaFuture}

class CallbackConverterIntegrationTest extends IntegrationTest with Mockito {

  override val injector =
    TestInjector(
      MessageBodyModule,
      FinatraJacksonModule,
      MustacheModule,
      DocRootModule,
      StatsReceiverModule
    ).create

  val callbackConverter = injector.instance[CallbackConverter]

  val ford = Car("Ford")
  val okResponse = SimpleResponse(Status.Ok, "bob")

  test("Future Some String") {
    assertOk(callbackConverter.convertToFutureResponse(futureSomeString), withBody = "hello")
  }

  test("Future None String") {
    assertStatus(
      callbackConverter.convertToFutureResponse(futureNoneString),
      expectedStatus = Status.NotFound
    )
  }

  test("Future Some Product") {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSomeProduct),
      withBody = """{"name":"Ford"}"""
    )
  }

  test("Future Some Trait") {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSomeTrait),
      withBody = """{"name":"Ford"}"""
    )
  }

  test("Future String") {
    assertOk(callbackConverter.convertToFutureResponse(futureString), withBody = "bob")
  }

  test("Future Response") {
    assertOk(callbackConverter.convertToFutureResponse(futureResponse), withBody = "bob")
  }

  test("Future Some Response") {
    assertOk(callbackConverter.convertToFutureResponse(futureSomeResponse), withBody = "bob")
  }

  test("Future None Response") {
    assertStatus(
      callbackConverter.convertToFutureResponse(futureNoneResponse),
      expectedStatus = Status.NotFound
    )
  }

  test("Future Seq String") {
    assertOk(callbackConverter.convertToFutureResponse(futureSeqString), withBody = """["bob"]""")
  }

  test("Future Seq Car") {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSeqCar),
      withBody = """[{"name":"Ford"}]"""
    )
  }

  test("Future Seq CarTrait") {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSeqCarTrait),
      withBody = """[{"name":"Ford"}]"""
    )
  }

  // ScalaFuture
  test("ScalaFuture Some String") {
    assertOk(callbackConverter.convertToFutureResponse(scalaFutureSomeString), withBody = "hello")
  }

  test("ScalaFuture None String") {
    assertStatus(
      callbackConverter.convertToFutureResponse(scalaFutureNoneString),
      expectedStatus = Status.NotFound
    )
  }

  test("ScalaFuture Some Product") {
    assertOk(
      callbackConverter.convertToFutureResponse(scalaFutureSomeProduct),
      withBody = """{"name":"Ford"}"""
    )
  }

  test("ScalaFuture Some Trait") {
    assertOk(
      callbackConverter.convertToFutureResponse(scalaFutureSomeTrait),
      withBody = """{"name":"Ford"}"""
    )
  }

  test("ScalaFuture String") {
    assertOk(callbackConverter.convertToFutureResponse(scalaFutureString), withBody = "bob")
  }

  test("ScalaFuture Response") {
    assertOk(callbackConverter.convertToFutureResponse(scalaFutureResponse), withBody = "bob")
  }

  test("ScalaFuture Some Response") {
    assertOk(callbackConverter.convertToFutureResponse(scalaFutureSomeResponse), withBody = "bob")
  }

  test("ScalaFuture None Response") {
    assertStatus(
      callbackConverter.convertToFutureResponse(scalaFutureNoneResponse),
      expectedStatus = Status.NotFound
    )
  }

  test("ScalaFuture Seq String") {
    assertOk(
      callbackConverter.convertToFutureResponse(scalaFutureSeqString),
      withBody = """["bob"]"""
    )
  }

  test("ScalaFuture Seq Car") {
    assertOk(
      callbackConverter.convertToFutureResponse(scalaFutureSeqCar),
      withBody = """[{"name":"Ford"}]"""
    )
  }

  test("ScalaFuture Seq CarTrait") {
    assertOk(
      callbackConverter.convertToFutureResponse(scalaFutureSeqCarTrait),
      withBody = """[{"name":"Ford"}]"""
    )
  }

  test("Object") {
    assertOk(callbackConverter.convertToFutureResponse(objectCallback), withBody = """asdf""")
  }

  test("None") {
    assertStatus(
      callbackConverter.convertToFutureResponse(noneCallback),
      expectedStatus = Status.NotFound
    )
  }

  test("Some") {
    assertOk(callbackConverter.convertToFutureResponse(someCallback), withBody = """asdf""")
  }

  test("Nothing") {
    intercept[Exception] {
      callbackConverter.convertToFutureResponse(noParameterCallback)
    }
  }

  test("Int") {
    intercept[Exception] {
      callbackConverter.convertToFutureResponse(intParameterCallback)
    }
  }

  test("Map[String, String]") {
    assertOk(
      callbackConverter.convertToFutureResponse(stringMapCallback),
      withBody = """{"message":"Hello, World!"}"""
    )
  }

  test("String") {
    assertOk(callbackConverter.convertToFutureResponse(stringCallback), withBody = "Hello, World!")
  }

  test("AsyncStream request") {
    val jsonStr = "[1,2]"
    val request = Request(HttpMethod.Post, "/")
    request.setChunked(true)
    request.writer.write(Buf.Utf8(jsonStr)) ensure {
      request.writer.close()
    }

    val converted = callbackConverter.convertToFutureResponse(asyncStreamRequest)

    val response = Await.result(converted(request))
    assertOk(response, "List(1, 2)")
  }

  test("AsyncStream response") {
    val converted = callbackConverter.convertToFutureResponse(asyncStreamResponse)

    val response = Await.result(converted(Request()))
    response.status should equal(Status.Ok)
    Await.result(Reader.readAll(response.reader)).utf8str should equal("[1,2,3]")
  }

  test("AsyncStream request and response") {
    val jsonStr = "[1,2]"
    val request = Request(HttpMethod.Post, "/")
    request.setChunked(true)
    request.writer.write(Buf.Utf8(jsonStr)) ensure {
      request.writer.close()
    }

    val converted = callbackConverter.convertToFutureResponse(asyncStreamRequestAndResponse)

    val response = Await.result(converted(request))
    response.status should equal(Status.Ok)
    Await.result(Reader.readAll(response.reader)).utf8str should equal("""["1","2"]""")
  }

  test("Null") {
    assertOk(callbackConverter.convertToFutureResponse(nullCallback), withBody = "")
  }

  def stringMapCallback(request: Request): Map[String, String] = {
    Map("message" -> "Hello, World!")
  }

  def stringCallback(request: Request): String = {
    "Hello, World!"
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

  def noParameterCallback = {
    "hello world"
  }

  def intParameterCallback(i: Int) = {
    "int"
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

  def scalaFutureSomeString(request: Request): ScalaFuture[Option[String]] = {
    ScalaFuture.successful(Some("hello"))
  }

  def scalaFutureNoneString(request: Request): ScalaFuture[Option[String]] = {
    ScalaFuture.successful(None)
  }

  def scalaFutureSomeProduct(request: Request): ScalaFuture[Option[Car]] = {
    ScalaFuture.successful(Some(ford))
  }

  def scalaFutureSomeTrait(request: Request): ScalaFuture[Option[CarTrait]] = {
    ScalaFuture.successful(Some(ford))
  }

  def scalaFutureString(request: Request): ScalaFuture[String] = {
    ScalaFuture.successful("bob")
  }

  def scalaFutureResponse(request: Request): ScalaFuture[Response] = {
    ScalaFuture.successful(okResponse)
  }

  def scalaFutureSomeResponse(request: Request): ScalaFuture[Option[Response]] = {
    ScalaFuture.successful(Some(okResponse))
  }

  def scalaFutureNoneResponse(request: Request): ScalaFuture[Option[Response]] = {
    ScalaFuture.successful(None)
  }

  def scalaFutureSeqString(request: Request): ScalaFuture[Seq[String]] = {
    ScalaFuture.successful(Seq("bob"))
  }

  def scalaFutureSeqCar(request: Request): ScalaFuture[Seq[Car]] = {
    ScalaFuture.successful(Seq(ford))
  }

  def scalaFutureSeqCarTrait(request: Request): ScalaFuture[Seq[CarTrait]] = {
    ScalaFuture.successful(Seq(ford))
  }

  def asyncStreamRequestAndResponse(stream: AsyncStream[Int]): AsyncStream[String] = {
    stream map { _.toString }
  }

  def asyncStreamRequest(stream: AsyncStream[Int]): Future[String] = {
    stream.toSeq() map { _.toString }
  }

  def asyncStreamResponse(request: Request): AsyncStream[Int] = {
    AsyncStream(1, 2, 3)
  }

  private def assertOk(response: Response, expectedBody: String) {
    response.status should equal(Status.Ok)
    response.contentString should equal(expectedBody)
  }

  private def assertOk(convertedFunc: (Request) => Future[Response], withBody: String) {
    val response = Await.result(convertedFunc(Request()))
    assertOk(response, withBody)
  }

  private def assertStatus(convertedFunc: (Request) => Future[Response], expectedStatus: Status) {
    val response = Await.result(convertedFunc(Request()))
    response.status should equal(expectedStatus)
  }
}

case class Car(name: String) extends CarTrait

trait CarTrait {
  val name: String
}
