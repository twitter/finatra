package com.twitter.finatra.response

import java.io.{File, FileWriter}

import com.twitter.finatra.test.HttpTest
import org.apache.commons.io.IOUtils


class ResponseBuilderTest extends HttpTest {

  "response builder" should {

    "handle file type as response body" in {
      val expectedContent = """{"id": "foo"}"""

      val tempFile = File.createTempFile(getClass.getSimpleName, ".json")
      tempFile.deleteOnExit()
      val writer = new FileWriter(tempFile)
      IOUtils.write(expectedContent, writer)
      writer.close()

      val response = testResponseBuilder.ok(tempFile)

      response.getContentString() should equal(expectedContent)
    }
  }
}
