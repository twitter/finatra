package com.twitter.finatra.json.tests.internal.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Method, Request}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.finatra.json.tests.internal.{CaseClassWithSeqBooleans, FooClass}
import com.twitter.inject.Test
import com.twitter.io.Buf
import com.twitter.util.Await

class StreamingTest extends Test {

  //        idx: 0123456
  val jsonStr = "[1,2,3]"
  val expected123 = AsyncStream(1, 2, 3)

  test("bufs to json") {
    assertParsed(
      AsyncStream(
        Buf.Utf8(jsonStr.substring(0, 1)),
        Buf.Utf8(jsonStr.substring(1, 4)),
        Buf.Utf8(jsonStr.substring(4))
      ),
      expectedInputStr = jsonStr,
      expected = expected123
    )
  }

  test("bufs to json 2") {
    assertParsed(
      AsyncStream(Buf.Utf8("[1"), Buf.Utf8(",2"), Buf.Utf8(",3"), Buf.Utf8("]")),
      expectedInputStr = jsonStr,
      expected = expected123
    )
  }

  test("bufs to json 3") {
    assertParsed(
      AsyncStream(Buf.Utf8("[1"), Buf.Utf8(",2,3,44"), Buf.Utf8("4,5]")),
      expectedInputStr = "[1,2,3,444,5]",
      expected = AsyncStream(1, 2, 3, 444, 5)
    )
  }

  test("bufs to json with some bigger objects") {
    assertParsedSimpleObjects(
      AsyncStream(Buf.Utf8("""[{ "id":"John" }"""),
          Buf.Utf8(""",{ "id":"Tom" },{ "id":"An" },"""),
          Buf.Utf8("""{ "id":"Jean" }]""")),
      expectedInputStr = """[{ "id":"John" },{ "id":"Tom" },{ "id":"An" },{ "id":"Jean" }]""",
      expected = AsyncStream(FooClass("John"), FooClass("Tom"), FooClass("An"), FooClass("Jean"))
    )
  }

  test("bufs to json with some complex objects") {
    assertParsedComplexObjects(
      AsyncStream(Buf.Utf8("""[{ "foos":[true,false,true] }"""),
          Buf.Utf8(""",{ "foos": [true,false,true] },"""),
          Buf.Utf8("""{ "foos":[false] }]""")),
      expectedInputStr = """[{ "foos":[true,false,true] },{ "foos": [true,false,true] },{ "foos":[false] }]""",
      expected = AsyncStream(CaseClassWithSeqBooleans(Seq(true,false,true)),
          CaseClassWithSeqBooleans(Seq(true,false,true)), CaseClassWithSeqBooleans(Seq(false)))
    )
  }

  test("bufs to json with some bigger objects and an escape character") {
    assertParsedSimpleObjects(
      AsyncStream(Buf.Utf8("""[{ "id":"John" }"""),
        Buf.Utf8(""",{ "id":"Tom" },{ "id""""),
        Buf.Utf8(""":"An" },{ "id":"Jean" }]""")),
      expectedInputStr = """[{ "id":"John" },{ "id":"Tom" },{ "id":"An" },{ "id":"Jean" }]""",
      expected = AsyncStream(FooClass("John"), FooClass("Tom"), FooClass("An"), FooClass("Jean"))
    )
  }

  test("bufs to json with some bigger objects but with json object split over 2 bufs") {
    assertParsedSimpleObjects(
      AsyncStream(Buf.Utf8("""[{ "id":"John" }"""),
          Buf.Utf8(""",{ "id":"Tom" },{ "id""""),
          Buf.Utf8(""":"An" },{ "id":"Jean" }]""")),
      expectedInputStr = """[{ "id":"John" },{ "id":"Tom" },{ "id":"An" },{ "id":"Jean" }]""",
      expected = AsyncStream(FooClass("John"), FooClass("Tom"), FooClass("An"), FooClass("Jean"))
    )
  }

  test("parse request") {
    val jsonStr = "[1,2]"
    val request = Request(Method.Post, "/")
    request.setChunked(true)

    val parser = new JsonStreamParser(FinatraObjectMapper.create())

    request.writer.write(Buf.Utf8(jsonStr)) ensure {
      request.writer.close()
    }

    assertFutureValue(parser.parseArray[Int](request.reader).toSeq(), Seq(1, 2))
  }

  private def readString(bufs: AsyncStream[Buf]): String = {
    val (seq, _) = Await.result(bufs.observe())

    val all = seq.reduce { _ concat _ }
    val bytes = Buf.ByteArray.Shared.extract(all)
    new String(bytes)
  }

  private def assertParsed(
    bufs: AsyncStream[Buf],
    expectedInputStr: String,
    expected: AsyncStream[Int]
  ): Unit = {

    readString(bufs) should equal(expectedInputStr)
    val parser = new JsonStreamParser(FinatraObjectMapper.create())
    assertFuture(parser.parseArray[Int](bufs).toSeq(), expected.toSeq())
  }

  private def assertParsedSimpleObjects(
    bufs: AsyncStream[Buf],
    expectedInputStr: String,
    expected: AsyncStream[FooClass]
  ): Unit = {

    readString(bufs) should equal(expectedInputStr)
    val parser = new JsonStreamParser(FinatraObjectMapper.create())
    assertFuture(parser.parseArray[FooClass](bufs).toSeq(), expected.toSeq())
  }

  private def assertParsedComplexObjects(
    bufs: AsyncStream[Buf],
    expectedInputStr: String,
    expected: AsyncStream[CaseClassWithSeqBooleans]
  ): Unit = {

    readString(bufs) should equal(expectedInputStr)
    val parser = new JsonStreamParser(FinatraObjectMapper.create())
    assertFuture(parser.parseArray[CaseClassWithSeqBooleans](bufs).toSeq(), expected.toSeq())
  }

}
