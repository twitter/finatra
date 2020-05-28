.. _http_streaming:

HTTP Streaming
==============

Finatra supports streaming over HTTP by defining the `StreamingRequest <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/streaming/StreamingRequest.scala>`__ or a `StreamingResponse <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/streaming/StreamingResponse.scala>`__ request/response types. A `StreamingRequest`/`StreamingResponse` is an HTTP Request/Response that carries a stream of objects in its body. We support using either of our streaming primitives, `Reader <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/io/Reader.scala>`__ or `AsyncStream <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/concurrent/AsyncStream.scala>`__, but we recommend using `Reader` as it has better support for resource management.

Features
--------

- The ability to program in terms of streams of domain objects instead of bytes. For example:

  * A `StreamingRequest` will deserialize a valid JSON string to a domain object using the `JsonStreamParser <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/streaming/JsonStreamParser.scala>`__.
  * A `StreamingResponse` will convert a domain object to a JSON string using the `ScalaObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/ScalaObjectMapper.scala>`__.
- The ability to bypass the `ObjectMappper` and `JsonStreamParser` by dealing with streams of `Buf` directly.
- The ability to perform resource management by using the signal from `Reader#onClose`.
- The ability to perform composable operations over streams using `map` and `flatMap`.
- Built-in `streaming metrics <https://docbird.twitter.biz/finagle/Metrics.html#streaming>`__ to gain more insights into a streams' health.

Examples
--------

Finatra streaming support lets controllers stream input, output, or both. By recognizing the input and output signatures of a handler as either a `Reader`, `StreamingRequest`, or `StreamingResponse`, Finatra will make the necessary conversions.

`StreamingRequest` and `StreamingResponse` are fully featured Request/Response objects and allow you control over, and access to, request parameters and headers. If instead, you only need to consume or return data without having to inspect the request or modify the response, you can deal completely in terms of our streaming primitives.

.. code:: scala

  import com.twitter.finatra.http.Controller
  import com.twitter.finatra.http.streaming.StreamingRequest
  import com.twitter.io.{Buf, Reader}

  case class Tweet(text: String)

  class MyTweetController extends Controller {

    // StreamingRequest[Reader, Tweet] => StreamingResponse[Reader, String]
    post("/tweets/streaming/request/response") { streamingRequest: StreamingRequest[Reader, Tweet] =>
      // In this case we need to look at a specific header
      val specialHeader: String = streamingRequest.request.headerMap("Special-Header")
      // Grab the input stream and do some work with it
      val responseReader: Reader[String] = streamingRequest.stream.map(_.text + specialHeader)
      // create a `StreamingResponse` via `ResponseBuilder` with our special header
      response.streaming(responseReader, headers = Map("Special-Header" -> Seq("Thank you!")))
    }

    // Reader[Tweet] => StreamingResponse[Reader, String]
    post("/tweets/streaming/reader/response") { tweetReader: Reader[Tweet] =>
      // Grab the input stream and do some work with it
      val responseReader: Reader[String] = tweetReader.map(_.text)
      // create a `StreamingResponse` via `ResponseBuilder` with our special header
      response.streaming(responseReader, headers = Map("Special-Header" -> Seq("Thank you!")))
    }

    // Reader[Tweet] => Reader[String]
    post("/tweets/streaming/reader/reader") { tweetReader: Reader[Tweet] =>
      // Grab the tweets, do some work with them and return the result
      tweetReader.map(_.text)
    }

    // Reader[Buf] => Reader[Buf]
    post("/tweets/streaming/reader/reader/bytes") { byteReader: Reader[Buf] =>
      // Grab the raw bytes, do some work with them and return the result
      byteReader.map(bytes => Buf.Utf8(bytes.toString + "hi"))
    }

  }

Resource management is performed by listening to `Reader#onClose`.

.. code:: scala

  import com.twitter.finatra.http.streaming.StreamingRequest
  import com.twitter.io.Reader
  import com.twitter.util.Closable

  trait Resource extends Closable {
    def lookup(test: String): String
  }

  class MyOtherTweetController extends Controller {
    def closableResource(): Resource = ???

    post("/tweets/streaming/reader_with_resource_management") {
      streamingRequest: StreamingRequest[Reader, Tweet] =>
        val resource = closableResource()

        // create a `Reader` with any object types to pass to the `StreamingResponse`
        val responseReader: Reader[String] = streamingRequest.stream.map(
          tweet => resource.lookup(tweet.text)
        )

        // close the resource after `reader#onClose` is resolved
        responseReader.onClose.ensure(resource.close())

        // create a `StreamingResponse` via `ResponseBuilder`
        response.streaming(responseReader)
    }
  }

You can check out more streaming examples from `Finatra examples <https://github.com/twitter/finatra/tree/develop/examples/advanced/streaming-example>`__.
