package com.twitter.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.{EmbeddedHttpServer, StreamingJsonTestHelper}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.server.FeatureTest
import com.twitter.io.Buf
import com.twitter.util.Await

class StreamingServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    new StreamingServer,
    streamResponse = true)

  lazy val streamingJsonHelper =
    new StreamingJsonTestHelper(server.mapper)

  "post streaming json" in {
    val request = RequestBuilder.post("/tweets").chunked

    val tweets = for (i <- 1 to 100) yield {
      Tweet(text = s"msg $i", location = Some("US"))
    }

    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, tweets, delayMs = 25)
    }

    val response = server.httpRequest(request)
    response.printAsyncStrings()
  }


  /* Response Implicit Utils */

  implicit class RichResponse(val self: Response) {
    def asyncStrings = {
      AsyncStream.fromReader(self.reader) map { case Buf.Utf8(str) =>
        str
      }
    }

    def printAsyncStrings() = {
      Await.result(
        self.asyncStrings map { "Read:\t" + _ } foreach println)
    }
  }
}
