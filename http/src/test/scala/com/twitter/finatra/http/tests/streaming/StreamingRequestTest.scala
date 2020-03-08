package com.twitter.finatra.http.tests.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Method, Request, Version}
import com.twitter.finatra.http.streaming.StreamingRequest
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.streaming.JsonStreamParser
import com.twitter.inject.Test
import com.twitter.io.{Buf, Reader}

class StreamingRequestTest extends Test {

  val jsonStr = """["first","second","third"]"""

  private val parser = new JsonStreamParser(ScalaObjectMapper())

  test("Request explicitly To AsyncStream of string") {
    val reader = Reader.fromSeq(Seq(
      Buf.Utf8(jsonStr.substring(0, 1)),
      Buf.Utf8(jsonStr.substring(1, 4)),
      Buf.Utf8(jsonStr.substring(4))))
    val request = Request(Version.Http11, Method.Post, "/", reader)

    val streamingRequest = StreamingRequest.fromRequestToAsyncStream[String](parser, request)
    assert(await(streamingRequest.stream.toSeq()) == Seq("first", "second", "third"))
  }

  test("Request implicitly To Reader of string") {
    val reader = Reader.fromSeq(Seq(
      Buf.Utf8(jsonStr.substring(0, 1)),
      Buf.Utf8(jsonStr.substring(1, 4)),
      Buf.Utf8(jsonStr.substring(4))))
    val request = Request(Version.Http11, Method.Post, "/", reader)

    val streamingRequest = StreamingRequest[Reader, String](parser, request)
    assert(await(Reader.toAsyncStream(streamingRequest.stream).toSeq()) ==
      Seq("first", "second", "third"))
  }

  test("Request implicitly to AsyncStream of string") {
    val reader = Reader.fromSeq(Seq(
      Buf.Utf8(jsonStr.substring(0, 1)),
      Buf.Utf8(jsonStr.substring(1, 4)),
      Buf.Utf8(jsonStr.substring(4))))
    val request = Request(Version.Http11, Method.Post, "/", reader)

    val streamingRequest = StreamingRequest[AsyncStream, String](parser, request)
    assert(await(streamingRequest.stream.toSeq()) ==
      Seq("first", "second", "third"))
  }

  test("Request implicitly to AsyncStream of Object") {
    val reader = Reader.fromSeq(Seq(
      Buf.Utf8("""[{"v1":1,"v2""""),
      Buf.Utf8(""":"first"},{""""),
      Buf.Utf8("""v1":2,"v2":"second"}]""")))
    val request = Request(Version.Http11, Method.Post, "/", reader)

    val streamingRequest = StreamingRequest[AsyncStream, BarClass](parser, request)
    assert(await(streamingRequest.stream.toSeq()) ==
      Seq(BarClass(1, "first"), BarClass(2, "second")))
  }
}

case class BarClass(v1 : Int, v2: String)
