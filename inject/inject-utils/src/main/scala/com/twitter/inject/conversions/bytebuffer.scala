package com.twitter.inject.conversions

import com.twitter.inject.conversions.buf._
import com.twitter.io.Buf
import com.twitter.util.logging.Logging
import java.nio.ByteBuffer

object bytebuffer extends Logging {

  implicit class RichByteBuffer(val self: ByteBuffer) extends AnyVal {

    def debugOutput(): Unit = {
      debug(new String(self.array()) + " " + self.position() + "/" + self.capacity())
    }

    def sharedBuf: Buf = {
      Buf.ByteBuffer.Shared(self)
    }

    def utf8str: String = {
      self.sharedBuf.utf8str
    }
  }

}
