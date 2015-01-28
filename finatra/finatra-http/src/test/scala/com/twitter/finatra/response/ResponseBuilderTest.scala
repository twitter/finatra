package com.twitter.finatra.response

import com.twitter.finatra.marshalling.MessageBodyManager
import com.twitter.finatra.marshalling.mustache.MustacheService
import com.twitter.finatra.routing.FileResolver
import com.twitter.finatra.test.HttpTest
import java.io.{File, FileWriter}
import org.apache.commons.io.IOUtils


class ResponseBuilderTest extends HttpTest {

  protected lazy val responseBuilder = new ResponseBuilder(
    mapper,
    new FileResolver,
    new MessageBodyManager(null, null, null),
    new MustacheService(null))

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
  }
}
