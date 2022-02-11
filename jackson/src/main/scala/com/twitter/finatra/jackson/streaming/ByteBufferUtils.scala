package com.twitter.finatra.jackson.streaming

import com.twitter.inject.conversions.buf._
import com.twitter.io.Buf
import com.twitter.util.logging.Logging
import java.nio.ByteBuffer

private[finatra] object ByteBufferUtils extends Logging {

  /**
   * Append a [[Buf]] to a [[ByteBuffer]]
   * @param byteBuffer [[ByteBuffer]] to which to append
   * @param buf [[Buf]] to append
   * @param position Sets the underlying buffer's position. If the mark is defined and larger than the
   *        new position then it is discarded.
   * @return Owned [[ByteBuffer]] with the appended [[Buf]] at the given position
   */
  //TODO: Optimize/Refactor
  def append(byteBuffer: ByteBuffer, buf: Buf, position: Int): ByteBuffer = {
    val byteBufferBuf = {
      val byteBufferCopy = byteBuffer.duplicate()
      byteBufferCopy.position(0)
      Buf.ByteBuffer.Shared(byteBufferCopy)
    }

    val combinedBufs = byteBufferBuf.concat(buf)
    val result = Buf.ByteBuffer.Shared.extract(combinedBufs)
    result.position(position)
    result
  }

  def debugBuffer(byteBuffer: ByteBuffer): Unit = {
    if (logger.isDebugEnabled) {
      val copy = byteBuffer.duplicate()
      copy.position(0)
      val buf = Buf.ByteBuffer.Shared(copy)
      val str = buf.utf8str

      debug(s"byteBuffer: $str pos: ${byteBuffer.position()}")
    }
  }
}
