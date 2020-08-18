package com.twitter.finatra.http.tests.response

import com.twitter.finagle.http.{
  Fields,
  MediaType,
  Request,
  Response,
  Status,
  Cookie => FinagleCookie
}
import com.twitter.finatra.http.marshalling.MessageBodyFlags
import com.twitter.finatra.http.modules.ResponseBuilderModule
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.modules.FileResolverFlags
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest}
import com.twitter.mock.Mockito
import java.io.{File, FileWriter}

class ResponseBuilderTest extends IntegrationTest with Mockito {

  override protected val injector: Injector =
    TestInjector(
      modules = Seq(ResponseBuilderModule),
      flags = Map(
        FileResolverFlags.LocalDocRoot -> "src/main/webapp/",
        MessageBodyFlags.ResponseCharsetEnabled -> "true"
      )
    ).create

  private lazy val responseBuilder = injector.instance[ResponseBuilder]

  test("handle simple response body") {
    val content = "test body"
    val response = responseBuilder.ok(content)

    response.getContentString() should equal(content)
    response.contentLengthOrElse(0) should equal(content.length)
  }

  test("handle simple response body with request") {
    val content = "test body"
    val request = Request()
    val response = responseBuilder.ok(request, content)

    response.getContentString() should equal(content)
  }

  test("handle file type as response body") {
    val expectedContent = """{"id": "foo"}"""

    val tempFile = File.createTempFile("temp", ".json")
    tempFile.deleteOnExit()
    val writer = new FileWriter(tempFile)
    writer.write(expectedContent)
    writer.close()

    val response = responseBuilder.ok(tempFile)

    response.getContentString() should equal(expectedContent)
    response.contentLengthOrElse(0) should equal(expectedContent.length)
    response.headerMap(Fields.ContentType) should equal(MediaType.JsonUtf8)
  }

  test("convert to an exception") {
    val e = responseBuilder.notFound.header("foo", "bar").toException
    e.response.status should equal(Status.NotFound)
    e.response.headerMap("foo") should equal("bar")
  }

  test("cookies") {
    assertFooBarCookie(responseBuilder.ok.cookie("foo", "bar"))
    assertFooBarCookie(responseBuilder.ok.cookie(new FinagleCookie("foo", "bar")))
  }

  test("appropriate response content type") {
    // we should only return the charset on appropriate content types
    val bytes: Array[Byte] = Array[Byte](10, -32, 17, 22)
    var response = responseBuilder.ok(bytes)
    response.headerMap(Fields.ContentType) should be(
      MediaType.OctetStream
    ) // does not include charset
    response.contentLengthOrElse(0) should equal(bytes.length)

    response = responseBuilder.ok("""Hello, world""")
    response.headerMap(Fields.ContentType) should be(MediaType.PlainTextUtf8) // includes charset
    response.contentLengthOrElse(0) should equal("""Hello, world""".length)

    val toMapValue = Map("key1" -> "value1", "key2" -> "value2")
    response = responseBuilder.ok(toMapValue)
    response.headerMap(Fields.ContentType) should be(MediaType.JsonUtf8) // includes charset
    response.contentLengthOrElse(0) > 0 should be(true)
  }

  test("properly return responses") {
    responseBuilder.noContent.status should equal(Status.NoContent)
    responseBuilder.notAcceptable.status should equal(Status.NotAcceptable)

    assertResponseWithFooBody(responseBuilder.notAcceptable("foo"), Status.NotAcceptable)

    responseBuilder.accepted.status should equal(Status.Accepted)

    responseBuilder.movedPermanently.status should equal(Status.MovedPermanently)
    assertResponseWithFooBody(responseBuilder.movedPermanently("foo"), Status.MovedPermanently)

    responseBuilder.notModified.status should equal(Status.NotModified)

    assertResponseWithFooBody(responseBuilder.badRequest("foo"), Status.BadRequest)

    assertResponseWithFooBody(responseBuilder.notFound("foo"), Status.NotFound)

    responseBuilder.gone.status should equal(Status.Gone)
    assertResponseWithFooBody(responseBuilder.gone("foo"), Status.Gone)

    responseBuilder.preconditionFailed.status should equal(Status.PreconditionFailed)
    assertResponseWithFooBody(responseBuilder.preconditionFailed("foo"), Status.PreconditionFailed)

    responseBuilder.requestEntityTooLarge.status should equal(Status.RequestEntityTooLarge)
    assertResponseWithFooBody(
      responseBuilder.requestEntityTooLarge("foo"),
      Status.RequestEntityTooLarge
    )

    assertResponseWithFooBody(
      responseBuilder.internalServerError("foo"),
      Status.InternalServerError
    )

    assertResponseWithFooBody(responseBuilder.ok.html("foo"), Status.Ok)

    responseBuilder.notImplemented.status should equal(Status.NotImplemented)

    responseBuilder.clientClosed.statusCode should equal(499)

    responseBuilder.ok
      .location(1.asInstanceOf[Any])
      .asInstanceOf[Response]
      .location
      .get should equal("1")

    responseBuilder.ok
      .header("Content-Type", MediaType.JsonUtf8)
      .asInstanceOf[Response]
      .contentType
      .get should equal(MediaType.JsonUtf8)

    responseBuilder.ok
      .headers(Map("Content-Type" -> "Foo"))
      .asInstanceOf[Response]
      .contentType
      .get should equal("Foo")

    responseBuilder.ok
      .headers(("Content-Type", "Foo"), ("A", "B"))
      .asInstanceOf[Response]
      .contentType
      .get should equal("Foo")

    await(responseBuilder.ok.toFuture).status should equal(Status.Ok)
  }

  def assertFooBarCookie(response: Response): Unit = {
    val cookie = response.getCookies().next()
    cookie.name should equal("foo")
    cookie.value should equal("bar")
  }

  def assertResponseWithFooBody(response: Response, expectedStatus: Status): Unit = {
    response.status should equal(expectedStatus)
    response.contentString should equal("foo")
  }
}
