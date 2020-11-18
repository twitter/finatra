package com.twitter.finatra.jackson.streaming

import com.fasterxml.jackson.core.async.ByteArrayFeeder
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser
import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken}
import com.twitter.io.Buf
import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer

/**
 * Integrate Jackson JSON Parser to parse JSON using non-blocking input source.
 *
 * Jackson JsonParser would validate the input JSON and throw
 * [[com.fasterxml.jackson.core.JsonParseException]] if unexpected token occurs.
 * Finatra AsyncJsonParser would decide where to split up the input Buf, return whole
 * serialized JSON objects and keep the unfinished bytes in the ByteBuffer.
 */
private[finatra] class AsyncJsonParser {
  private[this] val parser: JsonParser = new JsonFactory().createNonBlockingByteArrayParser()
  private[this] val feeder: ByteArrayFeeder =
    parser.asInstanceOf[NonBlockingJsonParser].getNonBlockingInputFeeder

  // All mutable state is synchronized on `this`

  // we can only parse the whole object/array/primitive type,
  // and keep unfinished bytes in this buffer.
  private[this] var remaining = ByteBuffer.allocate(0)
  // keep track the object's starting position in remaining byteBuffer
  private[this] var position: Int = 0
  // offset that records the length of parsed bytes in parser, accumulated every chunk
  private[this] var offset: Int = 0
  private[this] var depth: Int = 0

  /**
   * Parse the Buf to slice it into a List of objects/arrays/primitive types,
   * keep the unparsed part and wait for the next feed
   */
  private[finatra] def feedAndParse(buf: Buf): Seq[Buf] = synchronized {
    assertState()

    buf match {
      case Buf.ByteArray.Owned((bytes, begin, end)) =>
        feeder.feedInput(bytes, begin, end)
      case Buf.ByteArray.Shared(bytes) =>
        feeder.feedInput(bytes, 0, bytes.length)
      case b =>
        val bytes = new Array[Byte](b.length)
        b.write(bytes, 0)
        feeder.feedInput(bytes, 0, bytes.length)
    }

    remaining = ByteBufferUtils.append(remaining, buf, position)
    val result: ListBuffer[Buf] = ListBuffer.empty

    while (parser.nextToken() != JsonToken.NOT_AVAILABLE) {
      updateOpenBrace()
      if (startInitialArray) {
        // exclude the initial `[`
        position = parser.getCurrentLocation.getByteOffset.toInt - offset
      } else if (startObjectArray) {
        // include the starting token of the object or array
        position = parser.getCurrentLocation.getByteOffset.toInt - offset - 1
      } else if (endObjectArray) {
        result += getSlicedBuf
      } else if (endInitialArray) {
        depth = -1
      } else if (depth == 1) {
        result += getSlicedBuf
      } else {
        // fall through expected; a valid JsonToken, such as: FIELD_NAME, VALUE_NUMBER_INT, etc.
      }
    }

    result.toSeq
  }

  private def getSlicedBuf: Buf = {
    remaining.position(position)
    val newPosition = parser.getCurrentLocation.getByteOffset.toInt - offset
    val buf = remaining.slice()
    buf.limit(newPosition - position)

    remaining.position(newPosition)
    remaining = remaining.slice()

    offset = offset + newPosition
    position = 1

    Buf.ByteBuffer.Shared(buf)
  }

  private def startInitialArray: Boolean =
    parser.currentToken() == JsonToken.START_ARRAY && depth == 1

  private def startObjectArray: Boolean =
    depth == 2 && (parser.currentToken() == JsonToken.START_ARRAY ||
      parser.currentToken() == JsonToken.START_OBJECT)

  private def endObjectArray: Boolean =
    depth == 1 && (parser.currentToken() == JsonToken.END_ARRAY ||
      parser.currentToken() == JsonToken.END_OBJECT)

  private def updateOpenBrace(): Unit = {
    if (parser.currentToken() == JsonToken.START_ARRAY ||
      parser.currentToken() == JsonToken.START_OBJECT)
      depth += 1
    else if (parser.currentToken() == JsonToken.END_ARRAY ||
      parser.currentToken() == JsonToken.END_OBJECT)
      depth -= 1
  }

  private def endInitialArray: Boolean =
    depth == 0 && parser.currentToken() == JsonToken.END_ARRAY

  private def assertState(): Unit = {
    if (depth == -1) {
      throw new IllegalStateException("End of the JSON object (`]`) already found")
    }
  }

  // expose for tests
  private[jackson] def copiedByteBuffer: ByteBuffer = synchronized { remaining.duplicate() }
  private[jackson] def getParsingDepth: Int = synchronized { depth }
}
