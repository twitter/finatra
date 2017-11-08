package com.twitter.finatra.json.internal.streaming

import com.twitter.inject.Logging
import com.twitter.inject.conversions.buf._
import com.twitter.io.Buf
import java.nio.ByteBuffer

private[finatra] object ByteBufferUtils extends Logging {

  //TODO: Optimize/Refactor
  def append(byteBuffer: ByteBuffer, buf: Buf, pos: Int): ByteBuffer = {
    val byteBufferBuf = {
      val byteBufferCopy = byteBuffer.duplicate()
      byteBufferCopy.position(0)
      Buf.ByteBuffer.Shared(byteBufferCopy)
    }

    val combinedBufs = byteBufferBuf.concat(buf)
    val result = Buf.ByteBuffer.Shared.extract(combinedBufs)
    result.position(pos)
    result
  }

  def debugBuffer(byteBuffer: ByteBuffer) = {
    if (logger.isDebugEnabled) {
      val copy = byteBuffer.duplicate()
      copy.position(0)
      val buf = Buf.ByteBuffer.Shared(copy)
      val str = buf.utf8str

      debug(s"byteBuffer: $str pos: ${byteBuffer.position}")
    }
  }
}
