.. _http_streaming:

HTTP Streaming
==============

Finatra supports streaming over HTTP with a `StreamingRequest <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/streaming/StreamingRequest.scala>`__ or a `StreamingResponse <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/streaming/StreamingResponse.scala>`__.
A `StreamingRequest`/`StreamingResponse` is an HTTP Request/Response that carries a stream of objects in its body (preferably in the form of `Reader <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/io/Reader.scala>`__, or `AsyncStream <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/concurrent/AsyncStream.scala>`__). You can create and access the stream via `request.stream` or `response.stream`.

Advantages
----------

The major advantages of Finatra HTTP Streaming include:

- Ability to stream at object level instead of bytes.

  * `StreamingResponse` will serialize a Json object to a Json string with its `ObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__.
  * `StreamingRequest` will deserialize a valid Json string to a Json object with its `JsonStreamParser <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/streaming/JsonStreamParser.scala>`__.
- Resource management by listening to the result from `Reader#onClose`.
- Easy manipulation of each stream chunk via `Reader#map`.
- Built-in `streaming metrics <https://docbird.twitter.biz/finagle/Metrics.html#streaming>`__ to gain more insights into streams' health.

Example
-------

- How to stream from your controller:

.. code:: scala

  import com.twitter.finatra.http.streaming.StreamingRequest
  import com.twitter.io.Reader

  post("/tweets/streaming/reader") { streamingRequest: StreamingRequest[Reader, Tweet] =>
    // create a `Reader` with any object types to pass to the `StreamingResponse`
    val responseReader: Reader[String] = streamingRequest.stream.map(_.text)
    // create a `StreamingResponse` via `ResponseBuilder`
    response.streaming(responseReader)
  }


- How to stream with resource management:

.. code:: scala

  import com.twitter.finatra.http.streaming.StreamingRequest
  import com.twitter.io.Reader
  import com.twitter.util.Closable

  post("/tweets/streaming/reader_with_resource_management") {
    streamingRequest: StreamingRequest[Reader, Tweet] =>
    // A closable resource that needs to listen to the close of the stream
    val closable = Closable.nop
    // create a `Reader` with any object types to pass to the `StreamingResponse`
    val responseReader: Reader[String] = streamingRequest.stream.map(_.text)
    // close the resource after `reader#onClose` is resolved
    responseReader.onClose.ensure(closable.close())
    // create a `StreamingResponse` via `ResponseBuilder`
    response.streaming(responseReader)
  }

You can check out more streaming examples from `Finatra examples <https://github.com/twitter/finatra/tree/develop/examples/streaming-example>`__.
