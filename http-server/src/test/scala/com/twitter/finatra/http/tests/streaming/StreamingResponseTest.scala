package com.twitter.finatra.http.tests.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.Response
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.marshalling.MessageBodyManager
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.Test
import com.twitter.io.{Buf, BufReader, Reader, StreamTermination}
import com.twitter.util.Future
import com.twitter.util.mock.Mockito
import java.nio.charset.StandardCharsets

class StreamingResponseTest extends Test with Mockito {

  private def burnLoop(reader: Reader[Buf]): Future[Unit] = reader.read().flatMap {
    case Some(_) => burnLoop(reader)
    case None => Future.Unit
  }

  private[this] lazy val responseBuilder = new ResponseBuilder(
    objectMapper = ScalaObjectMapper(),
    fileResolver = new FileResolver(localDocRoot = "src/main/webapp/", docRoot = ""),
    messageBodyManager = mock[MessageBodyManager],
    statsReceiver = mock[StatsReceiver],
    includeContentTypeCharset = true
  )

  // Reader tests
  private def fromReader[A: Manifest](reader: Reader[A]): Response = {
    val streamingResponse = responseBuilder.streaming(reader)
    await(streamingResponse.toFutureResponse())
  }

  private def infiniteReader[A](item: A): Reader[A] = {
    def ones: Stream[A] = item #:: ones
    Reader.fromSeq(ones)
  }

  test("Reader: Empty Reader") {
    val reader = Reader.empty
    val response: Response = fromReader(reader)
    assert(
      Buf.decodeString(await(BufReader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        "[]")
    assert(await(response.reader.onClose) == StreamTermination.FullyRead)
  }

  test("Reader: Serialize and deserialize the Reader of string") {
    val stringSeq = Seq("first", "second", "third")
    val reader: Reader[String] = Reader.fromSeq(stringSeq)
    val response: Response = fromReader(reader)
    assert(
      Buf.decodeString(await(BufReader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        """["first","second","third"]""")
  }

  test("Reader: Serialize and deserialize the Reader of Float") {
    val listFloat = Array[Float](1, 2, 3)
    val reader: Reader[Float] = Reader.fromSeq(listFloat)
    val response: Response = fromReader(reader)
    assert(
      Buf.decodeString(await(BufReader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        "[1.0,2.0,3.0]")
  }

  test("Reader: Serialize and deserialize the Reader of Buf") {
    val bufSeq = Seq(Buf.ByteArray(1, 2, 3), Buf.ByteArray(4, 5, 6))
    val reader: Reader[Buf] = Reader.fromSeq(bufSeq)
    val response: Response = fromReader(reader)
    assert(await(BufReader.readAll(response.reader)) == bufSeq.head.concat(bufSeq.last))
  }

  test("Reader: Serialize and deserialize the Reader of Object") {
    case class BarClass(v1: Int, v2: String)
    val ojSeq = Seq(BarClass(1, "first"), BarClass(2, "second"))
    val reader: Reader[BarClass] = Reader.fromSeq(ojSeq)
    val response = fromReader(reader)
    assert(
      Buf.decodeString(await(BufReader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        """[{"v1":1,"v2":"first"},{"v1":2,"v2":"second"}]"""
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
  private def fromStream[A: Manifest](stream: AsyncStream[A]): Response = {
    val streamingResponse = responseBuilder.streaming(stream)
    await(streamingResponse.toFutureResponse())
  }

  private def infiniteStream[T](item: T): AsyncStream[T] = {
    item +:: infiniteStream(item)
  }

  test("AsyncStream: Empty AsyncStream") {
    val stream = AsyncStream.empty
    val response: Response = fromStream(stream)
    assert(
      Buf.decodeString(await(BufReader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        "[]")
    assert(await(response.reader.onClose) == StreamTermination.FullyRead)
  }

  test("AsyncStream: Serialize and deserialize the AsyncStream of string") {
    val stringSeq = Seq("first", "second", "third")
    val stream: AsyncStream[String] = AsyncStream.fromSeq(stringSeq)
    val response: Response = fromStream(stream)
    assert(
      Buf.decodeString(await(BufReader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        """["first","second","third"]""")
  }

  test("AsyncStream: Serialize and deserialize the AsyncStream of int") {
    val intSeq = Seq(1, 2, 3)
    val stream: AsyncStream[Int] = AsyncStream.fromSeq(intSeq)
    val response: Response = fromStream(stream)
    assert(
      Buf.decodeString(await(BufReader.readAll(response.reader)), StandardCharsets.UTF_8) ==
        "[1,2,3]")
  }

  test("AsyncStream: Serialize and deserialize the AsyncStream of Buf") {
    val bufSeq = Seq(Buf.ByteArray(1, 2, 3), Buf.ByteArray(4, 5, 6))
    val stream: AsyncStream[Buf] = AsyncStream.fromSeq(bufSeq)
    val response: Response = fromStream(stream)
    assert(await(BufReader.readAll(response.reader)) == bufSeq.head.concat(bufSeq.last))
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

  test("Response header map is correctly set") {
    val headerMap = Map[String, Seq[String]](
      "key1" -> Seq("value1", "value2", "value3"),
      "key2" -> Seq("v4", "v5", "v6")
    )
    val streamingResponse = responseBuilder.streaming(Reader.value("content"), headers = headerMap)
    val response = await(streamingResponse.toFutureResponse())
    assert(response.headerMap.getAll("key1") == Seq("value1", "value2", "value3"))
    assert(response.headerMap.getAll("key2") == Seq("v4", "v5", "v6"))
  }

  test("Add prefix and suffix to a JSON stream") {
    case class Lunch(drink: String, protein: Option[Long], carbs: Int)
    val lunches = List("coke", "sprite", "coffee", "tea", "fanta").map { drink =>
      Lunch(drink, Some(drink.length * 2), drink.length)
    }
    val streamingResponse1 = responseBuilder.streaming(Reader.fromSeq(lunches))
    val lunchBuf = streamingResponse1.toBufReader

    val prefix = Reader.fromBuf(Buf.Utf8("""{"options":"""))
    val suffix = Reader.fromBuf(Buf.Utf8(""","date": "02/12/2020"}"""))
    val response = fromReader(Reader.concat(Seq(prefix, lunchBuf, suffix)))

    val expectedJson =
      """{"options":[
        |{"drink":"coke","protein":8,"carbs":4},
        |{"drink":"sprite","protein":12,"carbs":6},
        |{"drink":"coffee","protein":12,"carbs":6},
        |{"drink":"tea","protein":6,"carbs":3},
        |{"drink":"fanta","protein":10,"carbs":5}
        |],"date": "02/12/2020"}""".stripMargin.replaceAll("\n", "")
    assert(
      Buf.decodeString(
        await(BufReader.readAll(response.reader)),
        StandardCharsets.UTF_8) == expectedJson)

  }
}
