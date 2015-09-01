package com.twitter.finatra.json.internal.streaming

import com.twitter.finatra.conversions.buf._
import com.twitter.finatra.json.internal.streaming.ParsingState._
import com.twitter.inject.Logging
import com.twitter.io.Buf
import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

// General logic copied from:
// https://github.com/netty/netty/blob/master/codec/src/main/java/io/netty/handler/codec/json/JsonObjectDecoder.java
private[finatra] class JsonArrayChunker extends Logging {

  private[finatra] var parsingState: ParsingState = Normal
  private[finatra] var done = false
  private[finatra] var openBraces = 0
  private var byteBuffer = ByteBuffer.allocate(0)

  /* Public */

  def decode(inputBuf: Buf): Seq[Buf] = {
    assertDecode(inputBuf)
    byteBuffer = ByteBufferUtils.append(byteBuffer, inputBuf)

    val result = ArrayBuffer[Buf]()

    while (byteBuffer.hasRemaining) {
      ByteBufferUtils.debugBuffer(byteBuffer)
      val currByte = byteBuffer.get

      if (!arrayFound && currByte == '[') {
        debug("ArrayFound. Openbraces = 1")
        parsingState = InsideArray
        openBraces = 1
        byteBuffer = byteBuffer.slice()
      }
      else if (!arrayFound && Character.isWhitespace(currByte.toChar)) {
        debug("Skip space")
      }
      else {
        decodeByteAndUpdateState(currByte, byteBuffer)
        if (!insideString && (openBraces == 1 && currByte == ',' || openBraces == 0 && currByte == ']')) {
          result += extractBuf()

          if (currByte == ']') {
            debug("Done")
            done = true
          }
        }
      }
    }

    result
  }

  /* Private */

  private def decodeByteAndUpdateState(c: Byte, in: ByteBuffer) {
    debug("decode '" + c.toChar + "'")
    if ((c == '{' || c == '[') && !insideString) {
      openBraces += 1
      debug("openBraces = " + openBraces)
    }
    else if ((c == '}' || c == ']') && !insideString) {
      openBraces -= 1
      debug("openBraces = " + openBraces)
    }
    else if (c == '"') {
      // start of a new JSON string. It's necessary to detect strings as they may
      // also contain braces/brackets and that could lead to incorrect results.
      if (!insideString) {
        debug("State = InsideString")
        parsingState = InsideString
      }
      // If the double quote wasn't escaped then this is the end of a string.
      else if (in.get(in.position - 1) != '\\') {
        debug("State = Parsing")
        parsingState = Normal
      }
    }
  }

  //TODO: Optimize
  private def extractBuf(): Buf = {
    val copy = byteBuffer.duplicate()
    copy.position(0)
    copy.limit(byteBuffer.position - 1)
    val copyBuf = Buf.ByteBuffer.Shared(copy)

    byteBuffer = byteBuffer.slice()
    debug("Extract result " + copyBuf.utf8str)
    copyBuf
  }

  private def assertDecode(inputBuf: Buf): Unit = {
    debug("Decode called with \"" + inputBuf.utf8str + "\"")
    if (done) {
      throw new scala.Exception("End array already found")
    }
  }

  private def insideString = parsingState == InsideString

  private def arrayFound = parsingState == InsideArray

  private[finatra] def copiedByteBuffer = byteBuffer.duplicate()
}
