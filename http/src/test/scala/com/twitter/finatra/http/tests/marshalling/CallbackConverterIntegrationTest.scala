package com.twitter.finatra.http.tests.marshalling

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Method => HttpMethod, Request, Response, Status}
import com.twitter.finatra.conversions.buf._
import com.twitter.finatra.http.internal.marshalling.CallbackConverter
import com.twitter.finatra.http.modules.{DocRootModule, MessageBodyModule, MustacheModule}
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.{IntegrationTest, Mockito}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Await, Future}

class CallbackConverterIntegrationTest extends IntegrationTest with Mockito {

  override val injector = TestInjector(
    MessageBodyModule,
    FinatraJacksonModule,
    MustacheModule,
    DocRootModule,
    StatsReceiverModule)

  val callbackConverter = injector.instance[CallbackConverter]

  val ford = Car("Ford")
  val okResponse = SimpleResponse(Status.Ok, "bob")

  "Future Some String" in {
    assertOk(
      callbackConverter.convertToFutureResponse(futureSomeString),
      withBody = "hello")
  }

  "Future None String" in {
    assertStatus(
      callbackConverter.convertToFutureResponse(futureNoneString),
      expectedStatus = Status.NotFound)
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
      expectedStatus = Status.NotFound)
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
      expectedStatus = Status.NotFound)
  }

  "Some" in {
    assertOk(
      callbackConverter.convertToFutureResponse(someCallback),
      withBody = """asdf""")
  }

  "Nothing" in {
    intercept[Exception] {
      callbackConverter.convertToFutureResponse(noParameterCallback)
    }
  }

  "Int" in {
    intercept[Exception] {
      callbackConverter.convertToFutureResponse(intParameterCallback)
    }
  }

  "AsyncStream request" in {
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

  "AsyncStream response" in {
    val converted = callbackConverter.convertToFutureResponse(asyncStreamResponse)

    val response = Await.result(converted(Request()))
    response.status should equal(Status.Ok)
    Await.result(Reader.readAll(response.reader)).utf8str should equal("[1,2,3]")
  }

  "AsyncStream request and response" in {
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

  private def assertStatus(
    convertedFunc: (Request) => Future[Response],
    expectedStatus: Status) {
    val response = Await.result(convertedFunc(Request()))
    response.status should equal(expectedStatus)
  }
}

case class Car(name: String) extends CarTrait

trait CarTrait {
  val name: String
}
