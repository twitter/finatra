package com.twitter.finatra.http.response

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.internal.marshalling.mustache.MustacheService
import com.twitter.finatra.http.routing.FileResolver
import com.twitter.finatra.http.test.HttpTest
import com.twitter.inject.Mockito
import java.io.{File, FileWriter}
import org.apache.commons.io.IOUtils


class ResponseBuilderTest extends HttpTest with Mockito
{

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
  }
}
