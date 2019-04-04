package com.twitter.streaming

import com.twitter.finagle.http.Response
import com.twitter.finatra.http.{EmbeddedHttpServer, StreamingJsonTestHelper}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.server.FeatureTest
import com.twitter.io.{Buf, Reader}

object StreamingServerFeatureTest {
  /* Response Implicit Utils */
  implicit class RichResponse(val self: Response) extends AnyVal {
    def readerStrings: Reader[String] = {
      self.reader.map {
        case Buf.Utf8(str) =>
          str
      }
    }

    def printReaderStrings() = {
      readerStrings map { str => println("Read:\t" + str) }
    }
  }
}

class StreamingServerFeatureTest extends FeatureTest {
  import StreamingServerFeatureTest._

  override val server = new EmbeddedHttpServer(new StreamingServer, streamResponse = true)

  lazy val streamingJsonHelper =
    new StreamingJsonTestHelper(server.mapper)

  test("streaming#post json") {
    val request = RequestBuilder.post("/tweets").chunked

    val tweets = for (i <- 1 to 100) yield {
      Tweet(text = s"msg $i", location = Some("US"))
    }

    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, tweets, delayMs = 25)
    }

    val response = server.httpRequest(request)
    response.printReaderStrings()
  }
}


