package com.twitter.finatra.json.tests.internal.streaming

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.conversions.buf._
import com.twitter.finatra.json.internal.streaming.{ParsingState, JsonArrayChunker}
import com.twitter.finatra.json.{JsonDiff, FinatraObjectMapper}
import com.twitter.finatra.json.internal.streaming.ParsingState._
import com.twitter.inject.Test
import com.twitter.io.Buf

class JsonObjectDecoderTest extends Test {

  "decode" in {
    val decoder = new JsonArrayChunker()

    assertDecode(
      decoder,
      input = "[1",
      output = Seq(),
      remainder = "1",
      pos = 1,
      openBraces = 1,
      parsingState = InsideArray)

    assertDecode(
      decoder,
      input = ",2",
      output = Seq("1"),
      remainder = "2",
      pos = 1)

    assertDecode(
      decoder,
      input = ",3",
      output = Seq("2"),
      remainder = "3",
      pos = 1)

    assertDecode(
      decoder,
      input = "]",
      output = Seq("3"),
      remainder = "",
      pos = 0,
      openBraces = 0,
      done = true)
  }

  val mapper = FinatraObjectMapper.create()

  "decode with nested objects" in {
    val jsonObj = """
     {
      "sub_object": {
        "msg": "hi"
      }
     }
     """

    assertSingleJsonParse(jsonObj)
  }

  "decode json inside a string" in {
    val jsonObj = """{"foo": "bar"}"""
    assertSingleJsonParse(jsonObj)
  }

  "Caling decode when already finished" in {
    val decoder = new JsonArrayChunker()
    decoder.decode(Buf.Utf8("[]"))
    intercept[Exception] {
      decoder.decode(Buf.Utf8("{}"))
    }
  }

  private def assertDecode(
    decoder: JsonArrayChunker,
    input: String,
    output: Seq[String],
    remainder: String,
    pos: Int,
    openBraces: Int = 1,
    parsingState: ParsingState = InsideArray,
    done: Boolean = false): Unit = {

    val result = decoder.decode(Buf.Utf8(input))
    result map { _.utf8str } should equal(output)

    val copiedByteBuffer = decoder.copiedByteBuffer.duplicate()
    copiedByteBuffer.position(0)
    val recvBuf = Buf.ByteBuffer.Shared(copiedByteBuffer)
    println("Result remainder: " + recvBuf.utf8str)

    recvBuf.utf8str should equal(remainder)
    decoder.copiedByteBuffer.position() should equal(pos)
    decoder.openBraces should equal(openBraces)
    decoder.parsingState should equal(parsingState)
    decoder.done should equal(done)
  }

  def assertSingleJsonParse(jsonObj: String): Unit = {
    val decoder = new JsonArrayChunker()
    val result = decoder.decode(Buf.Utf8("[" + jsonObj + "]"))
    val nodes = result map mapper.parse[JsonNode]
    nodes.size should be(1)
    JsonDiff.jsonDiff(nodes.head, jsonObj)
  }
}
