package com.twitter.finatra.jackson.streaming

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.json.JsonDiff
import com.twitter.inject.Test
import com.twitter.io.Buf
import com.twitter.inject.conversions.buf._
import org.scalacheck.{Gen, Shrink}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AsyncJsonParserTest extends Test with ScalaCheckDrivenPropertyChecks {

  test("decode") {
    val parser = new AsyncJsonParser()

    assertParsing(
      parser,
      input = "[1",
      output = Seq(),
      remainder = "[1",
      pos = 0,
      depth = 1
    )

    assertParsing(
      parser,
      input = ",2",
      output = Seq("1"),
      remainder = ",2",
      pos = 0,
      depth = 1
    )

    assertParsing(
      parser,
      input = ",3",
      output = Seq("2"),
      remainder = ",3",
      pos = 0,
      depth = 1
    )

    assertParsing(
      parser,
      input = "]",
      output = Seq("3"),
      remainder = "]",
      pos = 0,
      depth = -1
    )
  }

  test("decode with empty json") {
    val jsonObj = ""
    assertJsonParse(jsonObj, 0)
  }

  test("decode with nested arrays") {
    val jsonObj = """[1, 2, 3]"""
    assertJsonParse(jsonObj, 1)
  }

  test("decode with nested objects") {
    val jsonObj = """
     {
      "sub_object": {
        "msg": "hi"
      }
     }
     """

    assertJsonParse(jsonObj, 1)
  }

  test("decode with nested objects in array") {
    val jsonObj = """
      {
        "sub_object1": {
          "nested_item": {
            "msg": "hi"
          }
        }
      },
      {"sub_object2": {
          "msg": "hi"
        }
      }
     """
    assertJsonParse(jsonObj, 2)
  }

  test("invalid json field name") {
    val jsonObj = """
      {
        "sub_object1": {
          {"this" : "shouldnot}: "work"
        }
      },
      {"sub_object2": {
          "msg": "hi"
        }
      }
     """
    intercept[JsonParseException] {
      assertJsonParse(jsonObj, 2)
    }
  }

  test("miss use of curlies and squares ") {
    val jsonObj = """
     [
      {
        "msg": "hi"
      ],
     }
     """
    intercept[JsonParseException] {
      assertJsonParse(jsonObj, 1)
    }
  }

  test("decode json inside a string") {
    val jsonObj = """{"foo": "bar"}"""
    assertJsonParse(jsonObj, 1)
  }

  test("Calling decode when already finished") {
    val parser = new AsyncJsonParser()
    parser.feedAndParse(Buf.Utf8("[]"))
    intercept[Exception] {
      parser.feedAndParse(Buf.Utf8("{}"))
    }
  }

  test("other Buf types") {
    val parser = new AsyncJsonParser()
    val jsonObj = """{"foo": "bar"}"""
    val result = parser.feedAndParse(
      Buf.ByteBuffer.Owned(java.nio.ByteBuffer
        .wrap("[".getBytes("UTF-8") ++ jsonObj.getBytes("UTF-8") ++ "]".getBytes("UTF-8"))))
    val mapper = ScalaObjectMapper()
    val nodes = result.map(mapper.parse[JsonNode])
    nodes.size should be(1)
    JsonDiff.jsonDiff(nodes.head, jsonObj)
  }

  test("decode json split up over multiple chunks") {

    implicit val noShrink: Shrink[Seq[Buf]] = Shrink.shrinkAny

    forAll(genJsonObj) { json =>
      forAll(chunk(Buf.Utf8(json))) { bufs =>
        testChunks(json, bufs)
      }
    }
  }

  private def genJsonObj: Gen[String] = {
    implicit val noShrink: Shrink[Seq[Buf]] = Shrink.shrinkAny
    val jsonObjs = """
      [
        {
          "id": "John"
        },
        {
          "id": "Tom"
        },
        {
          "id": "An"
        },
        {
          "id": "Jean"
        }
      ]"""

    val nestedObjs = """
      [
        {
          "sub_object1": {
            "nested_item": {
              "msg": "hi"
            }
          }
        },
        {"sub_object2": {
            "msg": "hi"
          }
      }
      ]"""

    val nestedLists = """
      [
        [1, 2, 3],
        [4, 5, 6],
        [7, 8, [1, 2, 3]]
      ]"""

    val listAndObjs = """
      [
        {
          "id": "John"
        },
        {
          "id": "Tom"
        },
        [1,2,3,4],
        {
          "id": "An"
        }
      ]"""

    Gen.oneOf(Seq(jsonObjs, nestedObjs, nestedLists, listAndObjs))
  }

  private def chunk(buf: Buf): Gen[Seq[Buf]] =
    if (buf.isEmpty) Gen.const(Seq.empty[Buf])
    else {
      Gen.choose(1, buf.length).flatMap { n =>
        val taken = math.min(buf.length, n)
        val a = buf.slice(0, taken)
        val b = buf.slice(taken, buf.length)

        chunk(b).map(rest => a +: rest)
      }
    }

  private def testChunks(jsonObj: String, chunks: Seq[Buf]): Unit = {
    val mapper = ScalaObjectMapper()
    val parser = new AsyncJsonParser()

    val nodes: Seq[JsonNode] = chunks.flatMap { buf =>
      val bufs: Seq[Buf] = parser.feedAndParse(buf)
      bufs.map(mapper.parse[JsonNode])
    }
    JsonDiff.jsonDiff(nodes, jsonObj)
  }

  private def assertParsing(
    parser: AsyncJsonParser,
    input: String,
    output: Seq[String],
    remainder: String,
    pos: Int,
    depth: Int = 0
  ): Unit = {

    val result: Seq[Buf] = parser.feedAndParse(Buf.Utf8(input))
    result.map { _.utf8str } should equal(output)

    val copiedByteBuffer = parser.copiedByteBuffer.duplicate()
    copiedByteBuffer.position(0)
    val recvBuf = Buf.ByteBuffer.Shared(copiedByteBuffer)

    recvBuf.utf8str should equal(remainder)
    parser.copiedByteBuffer.position() should equal(pos)
    parser.getParsingDepth should equal(depth)
  }

  private def assertJsonParse(jsonObj: String, size: Int): Unit = {
    val mapper = ScalaObjectMapper()

    val parser = new AsyncJsonParser()
    val result: Seq[Buf] = parser.feedAndParse(Buf.Utf8("[" + jsonObj + "]"))
    val nodes: Seq[JsonNode] = result.map(mapper.parse[JsonNode])
    nodes.size should be(size)
    if (size > 0) JsonDiff.jsonDiff(nodes.head, jsonObj)
  }
}
