package com.twitter.streaming

import com.twitter.finatra.http.test.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Await

class StreamingServerFeatureTest extends FeatureTest with HttpTest {

  override val server = new EmbeddedHttpServer(
    new StreamingServer,
    streamResponse = true)

  "post streaming json" in {
    val request = RequestBuilder.post("/tweets").chunked

    val tweets = for (i <- 1 to 100) yield {
      Tweet(text = s"msg $i", location = Some("US"))
    }

    // Write to request in separate thread
    pool {
      writeJsonArray(request, tweets, delayMs = 25)
    }

    val response = server.httpRequest(request)
    response.printAsyncStrings()
  }
}
