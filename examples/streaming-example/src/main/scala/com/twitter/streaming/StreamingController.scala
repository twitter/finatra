package com.twitter.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.streaming.StreamingRequest
import com.twitter.io.Reader
import com.twitter.util.Closable

class StreamingController extends Controller {

  /**
   * This is the recommended way of doing streaming. It uses `StreamingRequest` and `StreamingResponse`
   * backed by a `Reader`
   */
  post("/tweets/streaming/reader") { streamingRequest: StreamingRequest[Reader, Tweet] =>
    // Create a `Reader` to pass to the `StreamingResponse`,
    // you could apply all your transformer logic here
    val responseReader: Reader[String] = streamingRequest.stream.map(_.text)
    response.streaming(responseReader)
  }

  /**
   * This is an example if you need to handle any resource management that listens to the status of
   * the stream
   */
  post("/tweets/streaming/reader_with_resource_management") {
    streamingRequest: StreamingRequest[Reader, Tweet] =>
      // A closable resource that needs to listen to the close of the stream
      val closable = Closable.nop
      val responseReader: Reader[String] = streamingRequest.stream.map(_.text)
      responseReader.onClose.ensure(closable.close())
      response.streaming(responseReader)
  }

  /**
   * This should only be used if you need to migrate an existing endpoint that uses `AsyncStream`.
   * It uses `StreamingRequest` and `StreamingResponse` backed by an `AsyncStream`. Otherwise please
   * follow the above example to create `StreamingRequest/Response` with `Reader` instead
   */
  post("/tweets/streaming/asyncstream") { streamingRequest: StreamingRequest[AsyncStream, Tweet] =>
    val requestAsyncStream: AsyncStream[Tweet] = streamingRequest.stream

    // Apply transformer logic for `AsyncStream`
    val transformedStream: AsyncStream[String] = requestAsyncStream.map(_.text)
    // Convert the asyncStream to a reader, because it is preferred to create a `StreamingResponse`
    // backed by a `Reader`
    response.streaming(Reader.fromAsyncStream(transformedStream))
  }

  /**
   * Streaming via `Reader`
   */
  post("/tweets") { tweets: Reader[Tweet] =>
    tweets.map { tweet =>
      "Created tweet " + tweet
    }
  }

}
