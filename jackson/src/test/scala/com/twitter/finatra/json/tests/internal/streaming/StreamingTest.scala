package com.twitter.finatra.json.tests.internal.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Method, Request}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.inject.Test
import com.twitter.io.Buf
import com.twitter.util.Await

class StreamingTest extends Test {

  //        idx: 0123456
  val jsonStr = "[1,2,3]"
  val expected123 = AsyncStream(1, 2, 3)

  "bufs to json" in {
    assertParsed(AsyncStream(
      Buf.Utf8(jsonStr.substring(0, 1)),
      Buf.Utf8(jsonStr.substring(1, 4)),
      Buf.Utf8(jsonStr.substring(4))),
      expectedInputStr = jsonStr,
      expected = expected123)
  }

  "bufs to json 2" in {
    assertParsed(AsyncStream(
      Buf.Utf8("[1"),
      Buf.Utf8(",2"),
      Buf.Utf8(",3"),
      Buf.Utf8("]")),
      expectedInputStr = jsonStr,
      expected = expected123)
  }

  "bufs to json 3" in {
    assertParsed(
      AsyncStream(
        Buf.Utf8("[1"),
        Buf.Utf8(",2,3,44"),
        Buf.Utf8("4,5]")),
      expectedInputStr = "[1,2,3,444,5]",
      expected = AsyncStream(1, 2, 3, 444, 5))
  }

  "parse request" in {
    val jsonStr = "[1,2]"
    val request = Request(Method.Post, "/")
    request.setChunked(true)

    val parser = new JsonStreamParser(FinatraObjectMapper.create())

    request.writer.write(Buf.Utf8(jsonStr)) ensure {
      request.writer.close()
    }

    Await.result(
      parser.parseArray[Int](request.reader).toSeq()) should equal(Seq(1, 2))
  }

  private def readString(bufs: AsyncStream[Buf]): String = {
    val (seq, _) = Await.result(
      bufs.observe())

    val all = seq.reduce { _ concat _ }
    val bytes = Buf.ByteArray.Shared.extract(all)
    new String(bytes)
  }

  private def assertParsed(
    bufs: AsyncStream[Buf],
    expectedInputStr: String,
    expected: AsyncStream[Int]): Unit = {

    readString(bufs) should equal(expectedInputStr)
    val parser = new JsonStreamParser(FinatraObjectMapper.create())
    Await.result(
      parser.parseArray[Int](bufs).toSeq) should equal(Await.result(expected.toSeq()))
  }
}
