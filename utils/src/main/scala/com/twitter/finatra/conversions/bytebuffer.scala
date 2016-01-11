package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.buf._
import com.twitter.inject.Logging
import com.twitter.io.Buf
import java.nio.ByteBuffer

object bytebuffer extends Logging {

  implicit class RichByteBuffer(val byteBuffer: ByteBuffer) extends AnyVal {

    def debugOutput(): Unit = {
      debug(new String(byteBuffer.array()) + " " + byteBuffer.position() + "/" + byteBuffer.capacity())
    }

    def sharedBuf: Buf = {
      Buf.ByteBuffer.Shared(byteBuffer)
    }

    def utf8str: String = {
      byteBuffer.sharedBuf.utf8str
    }
  }

}
