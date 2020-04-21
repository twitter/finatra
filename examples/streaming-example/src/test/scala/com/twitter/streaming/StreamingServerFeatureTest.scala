package com.twitter.streaming

import com.fasterxml.jackson.databind.JsonNode
// import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.{EmbeddedHttpServer, StreamingJsonTestHelper}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.server.FeatureTest
import com.twitter.io.BufReader
import com.twitter.util.{Duration, Future}

object StreamingServerFeatureTest {
  val TweetMsgPrefix: String = "msg: "
  val TweetLocation: String = "US"
}

class StreamingServerFeatureTest extends FeatureTest {
  import StreamingServerFeatureTest._

  // our response stream has a delay between messages so 5 seconds might cut it close
  // in slow CI environments like Travis so we are bumping timeouts to 10 seconds.
  // override protected def defaultAwaitTimeout: Duration = 10.seconds

  override val server = new EmbeddedHttpServer(
    new StreamingServer,
    streamResponse = true,
    disableTestLogging = true)

  lazy val streamingJsonHelper =
    new StreamingJsonTestHelper(server.mapper)

  // these streaming endpoints remove the TweetLocation from the original tweets
  test("streamingRequest#post via Reader") {
    val response = verifyStreamingEndpointPost("/tweets/streaming/reader")
    val firstItem = getFirstItem(response)
    assert(firstItem.equals(TweetMsgPrefix + "1"))
  }

  test("streamingRequest#post with resource management") {
    val response = verifyStreamingEndpointPost("/tweets/streaming/reader_with_resource_management")
    val firstItem = getFirstItem(response)
    assert(firstItem.equals(TweetMsgPrefix + "1"))
  }

  test("streamingRequest#post via AsyncStream") {
    val response = verifyStreamingEndpointPost("/tweets/streaming/asyncstream")
    val firstItem = getFirstItem(response)
    assert(firstItem.equals(TweetMsgPrefix + "1"))
  }

  test("streaming#post json") {
    verifyStreamingEndpointPost("/tweets")
  }

  /* -----------------------------------------------------------------------------------------------
   *                                              Utils
   * ---------------------------------------------------------------------------------------------*/
  private def verifyStreamingEndpointPost(url: String): Response = {
    val request = RequestBuilder.post(url).chunked
    streamTweets(request)
    server.httpRequest(request, andExpect = Status.Ok)
  }

  private def getFirstItem(response: Response): String = {
    val buf = await(BufReader.readAll(response.reader))
    server.mapper.parse[JsonNode](buf).get(0).asText()
  }

  private def streamTweets(request: Request): Future[Unit] = {
    val tweets = for (i <- 1 to 100) yield {
      Tweet(msg = TweetMsgPrefix + s"$i", location = Some(TweetLocation))
    }
    // Write to request in separate thread
    pool {
      streamingJsonHelper.writeJsonArray(request, tweets, delayMs = 25)
    }
  }
}
