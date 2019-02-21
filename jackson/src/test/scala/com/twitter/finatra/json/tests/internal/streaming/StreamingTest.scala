package com.twitter.finatra.json.tests.internal.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Method, Request}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.finatra.json.tests.internal.{CaseClassWithSeqBooleans, FooClass}
import com.twitter.inject.Test
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Await, Future}
import scala.collection.mutable.ArrayBuffer

class StreamingTest extends Test {

  //        idx: 0123456
  val jsonStr = "[1,2,3]"
  val expected123 = AsyncStream(1, 2, 3)

  test("bufs to json - AsyncStream") {
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

  test("bufs to json 2 - AsyncStream") {
    assertParsed(
      AsyncStream(Buf.Utf8("[1"), Buf.Utf8(",2"), Buf.Utf8(",3"), Buf.Utf8("]")),
      expectedInputStr = jsonStr,
      expected = expected123
    )
  }

  test("bufs to json 3 - AsyncStream") {
    assertParsed(
      AsyncStream(Buf.Utf8("[1"), Buf.Utf8(",2,3,44"), Buf.Utf8("4,5]")),
      expectedInputStr = "[1,2,3,444,5]",
      expected = AsyncStream(1, 2, 3, 444, 5)
    )
  }

  test("bufs to json with some bigger objects - AsyncStream") {
    assertParsedSimpleObjects(
      AsyncStream(
        Buf.Utf8("""[{ "id":"John" }"""),
        Buf.Utf8(""",{ "id":"Tom" },{ "id":"An" },"""),
        Buf.Utf8("""{ "id":"Jean" }]""")),
      expectedInputStr = """[{ "id":"John" },{ "id":"Tom" },{ "id":"An" },{ "id":"Jean" }]""",
      expected = AsyncStream(FooClass("John"), FooClass("Tom"), FooClass("An"), FooClass("Jean"))
    )
  }

  test("bufs to json with some complex objects - AsyncStream") {
    assertParsedComplexObjects(
      AsyncStream(
        Buf.Utf8("""[{ "foos":[true,false,true] }"""),
        Buf.Utf8(""",{ "foos": [true,false,true] },"""),
        Buf.Utf8("""{ "foos":[false] }]""")),
      expectedInputStr =
        """[{ "foos":[true,false,true] },{ "foos": [true,false,true] },{ "foos":[false] }]""",
      expected = AsyncStream(
        CaseClassWithSeqBooleans(Seq(true, false, true)),
        CaseClassWithSeqBooleans(Seq(true, false, true)),
        CaseClassWithSeqBooleans(Seq(false)))
    )
  }

  test("bufs to json with some bigger objects and an escape character - AsyncStream") {
    assertParsedSimpleObjects(
      AsyncStream(
        Buf.Utf8("""[{ "id":"John" }"""),
        Buf.Utf8(""",{ "id":"\\Tom" },{ "id""""),
        Buf.Utf8(""":"An" },{ "id":"Jean" }]""")),
      expectedInputStr = """[{ "id":"John" },{ "id":"\\Tom" },{ "id":"An" },{ "id":"Jean" }]""",
      expected = AsyncStream(FooClass("John"), FooClass("\\Tom"), FooClass("An"), FooClass("Jean"))
    )
  }

  test("bufs to json with some bigger objects but with json object split over multiple bufs - AsyncStream") {
    assertParsedSimpleObjects(
      AsyncStream(
        Buf.Utf8("""[{ "id":"John" }"""),
        Buf.Utf8(""",{ "id":"Tom" },{ "id""""),
        Buf.Utf8(""":"An" },{ "id":"Jean" }]""")),
      expectedInputStr = """[{ "id":"John" },{ "id":"Tom" },{ "id":"An" },{ "id":"Jean" }]""",
      expected = AsyncStream(FooClass("John"), FooClass("Tom"), FooClass("An"), FooClass("Jean"))
    )
  }

  test("parse request - AsyncStream") {
    val jsonStr = "[1,2]"
    val request = Request(Method.Post, "/")
    request.setChunked(true)

    val parser = new JsonStreamParser(FinatraObjectMapper.create())

    request.writer.write(Buf.Utf8(jsonStr)) ensure {
      request.writer.close()
    }

    assertFutureValue(parser.parseArray[Int](request.reader).toSeq(), Seq(1, 2))
  }

  test("bufs to json - Reader") {
    assertParsed(
      Reader.fromSeq(
        Seq(
          Buf.Utf8(jsonStr.substring(0, 1)),
          Buf.Utf8(jsonStr.substring(1, 4)),
          Buf.Utf8(jsonStr.substring(4)))),
      expected = Reader.fromSeq(Seq(1, 2, 3))
    )
  }

  test("bufs to json 2 - Reader") {
    assertParsed(
      Reader.fromSeq(Seq(Buf.Utf8("[1"), Buf.Utf8(",2"), Buf.Utf8(",3"), Buf.Utf8("]"))),
      expected = Reader.fromSeq(Seq(1, 2, 3))
    )
  }

  test("bufs to json 3 - Reader") {
    assertParsed(
      Reader.fromSeq(Seq(Buf.Utf8("[1"), Buf.Utf8(",2,3,44"), Buf.Utf8("4,5]"))),
      expected = Reader.fromSeq(Seq(1, 2, 3, 444, 5))
    )
  }

  test("bufs to json with some bigger objects - Reader") {
    assertParsedSimpleObjects(
      Reader.fromSeq(
        Seq(
          Buf.Utf8("""[{ "id":"John" }"""),
          Buf.Utf8(""",{ "id":"Tom" },{ "id":"An" },"""),
          Buf.Utf8("""{ "id":"Jean" }]"""))),
      expected =
        Reader.fromSeq(Seq(FooClass("John"), FooClass("Tom"), FooClass("An"), FooClass("Jean")))
    )
  }

  test("bufs to json with some complex objects - Reader") {
    assertParsedComplexObjects(
      Reader.fromSeq(
        Seq(
          Buf.Utf8("""[{ "foos":[true,false,true] }"""),
          Buf.Utf8(""",{ "foos": [true,false,true] },"""),
          Buf.Utf8("""{ "foos":[false] }]"""))),
      expected = Reader.fromSeq(
        Seq(
          CaseClassWithSeqBooleans(Seq(true, false, true)),
          CaseClassWithSeqBooleans(Seq(true, false, true)),
          CaseClassWithSeqBooleans(Seq(false))))
    )
  }

  test("bufs to json with some bigger objects and an escape character - Reader") {
    assertParsedSimpleObjects(
      Reader.fromSeq(
        Seq(
          Buf.Utf8("""[{ "id":"John" }"""),
          Buf.Utf8(""",{ "id":"\\Tom" },{ "id""""),
          Buf.Utf8(""":"An" },{ "id":"Jean" }]"""))),
      expected =
        Reader.fromSeq(Seq(FooClass("John"), FooClass("\\Tom"), FooClass("An"), FooClass("Jean")))
    )
  }

  test("bufs to json with some bigger objects but with json object split over multiple bufs - Reader") {
    assertParsedSimpleObjects(
      Reader.fromSeq(
        Seq(
          Buf.Utf8("""[{ "id":"John" }"""),
          Buf.Utf8(""",{ "id":"Tom" },{ "id""""),
          Buf.Utf8(""":"An" },{ "id":"Jean" }]"""))),
      expected =
        Reader.fromSeq(Seq(FooClass("John"), FooClass("Tom"), FooClass("An"), FooClass("Jean")))
    )
  }

  test("parse request - Reader") {
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

  private def assertParsed(reader: Reader[Buf], expected: Reader[Int]): Unit = {
    val parser = new JsonStreamParser(FinatraObjectMapper.create())
    assertReader(parser.parseJson[Int](reader), expected)
  }

  private def assertParsedSimpleObjects(reader: Reader[Buf], expected: Reader[FooClass]): Unit = {
    val parser = new JsonStreamParser(FinatraObjectMapper.create())
    assertReader(parser.parseJson[FooClass](reader), expected)
  }

  private def assertParsedComplexObjects(
    reader: Reader[Buf],
    expected: Reader[CaseClassWithSeqBooleans]
  ): Unit = {

    val parser = new JsonStreamParser(FinatraObjectMapper.create())
    assertReader(parser.parseJson[CaseClassWithSeqBooleans](reader), expected)
  }

  protected def assertReader[A](r1: Reader[A], r2: Reader[A]): Unit = {
    assertFuture(readAll(r1), readAll(r2))
  }

  private def readAll[A](r: Reader[A]): Future[List[A]] = {
    def loop(left: ArrayBuffer[A]): Future[List[A]] =
      r.read().flatMap {
        case Some(right) => loop(left += right)
        case _ => Future.value(left.toList)
      }

    loop(ArrayBuffer.empty[A])
  }
}
