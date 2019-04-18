package com.twitter.finatra.http.tests.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.Response
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.{Mockito, Test}
import com.twitter.io.{Buf, Reader, StreamTermination}
import com.twitter.util.Future
import java.nio.charset.StandardCharsets

class StreamingResponseTest extends Test with Mockito {

  private def burnLoop(reader: Reader[Buf]): Future[Unit] = reader.read().flatMap {
    case Some(_) => burnLoop(reader)
    case None => Future.Unit
  }

  private[this] lazy val responseBuilder = new ResponseBuilder(
    objectMapper = FinatraObjectMapper.create(),
    fileResolver = new FileResolver(localDocRoot = "src/main/webapp/", docRoot = ""),
    messageBodyManager = mock[MessageBodyManager],
    statsReceiver = mock[StatsReceiver],
    includeContentTypeCharset = true
  )

  // Reader tests
  private def fromReader[A](reader: Reader[A]): Response = {
    val streamingResponse = responseBuilder.streaming(reader)
    await(streamingResponse.toFutureResponse)
  }

  private def infiniteReader[A](item: A): Reader[A] = {
    def ones: Stream[A] = item #:: ones
    Reader.fromSeq(ones)
  }

  test("Reader: Empty Reader") {
    val reader = Reader.empty
    val response: Response = fromReader(reader)
    // observe the EOS
    response.reader.read()
    assert(await(response.reader.onClose) == StreamTermination.FullyRead)
  }

  test("Reader: Serialize and deserialize the Reader of string") {
    val stringSeq = Seq("first", "second", "third")
    val reader: Reader[String] = Reader.fromSeq(stringSeq)
    val response: Response = fromReader(reader)
    assert(
      Buf.decodeString(await(Reader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        "\"first\"\"second\"\"third\"")
  }

  test("Reader: Serialize and deserialize the Reader of Object") {
    case class BarClass(v1 : Int, v2: String)
    val ojSeq = Seq(BarClass(1, "first"), BarClass(2, "second"))
    val reader: Reader[BarClass] = Reader.fromSeq(ojSeq)
    val response = fromReader(reader)
    assert(
      Buf.decodeString(await(Reader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        """{"v1":1,"v2":"first"}{"v1":2,"v2":"second"}"""
    )
  }

  test("Reader: write failures with ReaderDiscardedException") {
    val reader: Reader[Buf] = infiniteReader(Buf.Utf8("foo"))
    val response: Response = fromReader(reader)
    response.reader.discard()
    intercept[Throwable] {
      await(burnLoop(response.reader))
    }
    assert(await(response.reader.onClose) == StreamTermination.Discarded)
  }

  // AsyncStream tests
  private def fromStream[A](stream: AsyncStream[A]): Response = {
    val streamingResponse = responseBuilder.streaming(stream)
    await(streamingResponse.toFutureResponse())
  }

  private def infiniteStream[T](item: T): AsyncStream[T] = {
    item +:: infiniteStream(item)
  }

  test("AsyncStream: Empty AsyncStream") {
    val stream = AsyncStream.empty
    val response: Response = fromStream(stream)
    // observe the EOS
    response.reader.read()
    assert(await(response.reader.onClose) == StreamTermination.FullyRead)
  }

  test("AsyncStream: Serialize and deserialize the AsyncStream of string") {
    val stringSeq = Seq("first", "second", "third")
    val stream: AsyncStream[String] = AsyncStream.fromSeq(stringSeq)
    val response: Response = fromStream(stream)
    assert(
      Buf.decodeString(await(Reader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        "\"first\"\"second\"\"third\"")
  }

  // interrupt infinite AsyncStream
  test("AsyncStream: write failures with ReaderDiscardedException") {
    val stream: AsyncStream[Buf] = infiniteStream(Buf.Utf8("foo"))
    val response: Response = fromStream(stream)
    response.reader.discard()
    intercept[Throwable] {
      await(burnLoop(response.reader))
    }
    assert(await(response.reader.onClose) == StreamTermination.Discarded)
  }
}
