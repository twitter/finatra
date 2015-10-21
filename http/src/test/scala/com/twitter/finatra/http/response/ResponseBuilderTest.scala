package com.twitter.finatra.http.response

import com.google.common.net.MediaType
import com.twitter.finagle.http.{Cookie => FinagleCookie, Response, Status}
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.marshalling.mustache.MustacheService
import com.twitter.finatra.http.routing.FileResolver
import com.twitter.finatra.http.test.HttpTest
import com.twitter.inject.Mockito
import com.twitter.util.Await
import java.io.{File, FileWriter}
import org.apache.commons.io.IOUtils
import org.jboss.netty.handler.codec.http.DefaultCookie


class ResponseBuilderTest extends HttpTest with Mockito {

  protected lazy val responseBuilder = new ResponseBuilder(
    mapper,
    new FileResolver(
      localDocRoot = "src/main/webapp/",
      docRoot = ""),
    mock[MessageBodyManager],
    mock[MustacheService])

  "response builder" should {

    "handle file type as response body" in {
      val expectedContent = """{"id": "foo"}"""

      val tempFile = File.createTempFile("temp", ".json")
      tempFile.deleteOnExit()
      val writer = new FileWriter(tempFile)
      IOUtils.write(expectedContent, writer)
      writer.close()

      val response = responseBuilder.ok(tempFile)

      response.getContentString() should equal(expectedContent)
      response.headerMap("Content-Type") should equal("application/json;charset=utf-8")
    }

    "convert to an exception" in {
      val e = responseBuilder.notFound.header("foo", "bar").toException
      e.response.status should equal(Status.NotFound)
      e.response.headerMap("foo") should equal("bar")
    }

    "cookies" in {
      assertFooBarCookie(
        responseBuilder.ok.cookie("foo", "bar"))

      assertFooBarCookie(
        responseBuilder.ok.cookie(new FinagleCookie("foo", "bar")))

      assertFooBarCookie(
        responseBuilder.ok.cookie(new DefaultCookie("foo", "bar")))
    }

    "properly return responses" in {
      responseBuilder.noContent.status should equal(Status.NoContent)
      responseBuilder.notAcceptable.status should equal(Status.NotAcceptable)

      assertResponseWithFooBody(
        responseBuilder.notAcceptable("foo"),
        Status.NotAcceptable)

      responseBuilder.accepted.status should equal(Status.Accepted)

      responseBuilder.movedPermanently.status should equal(Status.MovedPermanently)
      assertResponseWithFooBody(
        responseBuilder.movedPermanently("foo"),
        Status.MovedPermanently)

      responseBuilder.notModified.status should equal(Status.NotModified)

      assertResponseWithFooBody(
        responseBuilder.badRequest("foo"),
        Status.BadRequest)

      assertResponseWithFooBody(
        responseBuilder.notFound("foo"),
        Status.NotFound)

      responseBuilder.gone.status should equal(Status.Gone)
      assertResponseWithFooBody(
        responseBuilder.gone("foo"),
        Status.Gone)

      responseBuilder.preconditionFailed.status should equal(Status.PreconditionFailed)
      assertResponseWithFooBody(
        responseBuilder.preconditionFailed("foo"),
        Status.PreconditionFailed)

      responseBuilder.requestEntityTooLarge.status should equal(Status.RequestEntityTooLarge)
      assertResponseWithFooBody(
        responseBuilder.requestEntityTooLarge("foo"),
        Status.RequestEntityTooLarge)


      assertResponseWithFooBody(
        responseBuilder.internalServerError("foo"),
        Status.InternalServerError)

      assertResponseWithFooBody(
        responseBuilder.ok.html("foo"),
        Status.Ok)

      responseBuilder.notImplemented.status should equal(Status.NotImplemented)

      responseBuilder.clientClosed.statusCode should equal(499)

      responseBuilder.ok.location(1.asInstanceOf[Any]).asInstanceOf[Response].location.get should equal("1")

      responseBuilder.ok.header("Content-Type", MediaType.JSON_UTF_8).asInstanceOf[Response].contentType.get should equal(MediaType.JSON_UTF_8.toString)

      responseBuilder.ok.headers(Map("Content-Type" -> "Foo")).asInstanceOf[Response].contentType.get should equal("Foo")

      responseBuilder.ok.headers(("Content-Type", "Foo"), ("A", "B")).asInstanceOf[Response].contentType.get should equal("Foo")

      Await.result(responseBuilder.ok.toFuture).status should equal(Status.Ok)
    }
  }

  def assertFooBarCookie(response: ResponseBuilderTest.this.responseBuilder.EnrichedResponse): Unit = {
    val cookie = response.getCookies().next()
    cookie.name should equal("foo")
    cookie.value should equal("bar")
  }

  def assertResponseWithFooBody(response: Response, expectedStatus: Status): Unit = {
    response.status should equal(expectedStatus)
    response.contentString should equal("foo")
  }
}
