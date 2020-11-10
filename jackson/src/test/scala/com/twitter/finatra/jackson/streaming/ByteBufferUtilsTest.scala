package com.twitter.finatra.jackson.streaming

import com.twitter.inject.conversions.bytebuffer._
import com.twitter.inject.Test
import com.twitter.io.Buf

class ByteBufferUtilsTest extends Test {

  test("ByteBufferUtils.append") {
    val input = Buf.ByteBuffer.Shared.extract(Buf.Utf8("1"))
    input.get()
    val byteBufferResult = ByteBufferUtils.append(input, Buf.Utf8(",2"), position = 0)

    byteBufferResult.utf8str should equal("1,2")
  }
}
