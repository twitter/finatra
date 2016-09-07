package com.twitter.finatra.json.internal.streaming

import com.twitter.finatra.conversions.buf._
import com.twitter.inject.Logging
import com.twitter.io.Buf
import java.nio.ByteBuffer

private[finatra] object ByteBufferUtils extends Logging {

  //TODO: Optimize/Refactor
  def append(byteBuffer: ByteBuffer, buf: Buf): ByteBuffer = {
    val byteBufferBuf = {
      val byteBufferCopy = byteBuffer.duplicate()
      byteBufferCopy.position(0)
      Buf.ByteBuffer.Shared(byteBufferCopy)
    }

    val combinedBufs = byteBufferBuf.concat(buf)
    Buf.ByteBuffer.Shared.extract(combinedBufs)
  }

  def debugBuffer(byteBuffer: ByteBuffer) = {
    if (logger.isDebugEnabled) { 
      val copy = byteBuffer.duplicate()
      copy.position(0)
      val buf = Buf.ByteBuffer.Shared(copy)
      val str = buf.utf8str

      debug(s"byteBuffer: $str pos: ${byteBuffer.position }")
    }
  }
}
