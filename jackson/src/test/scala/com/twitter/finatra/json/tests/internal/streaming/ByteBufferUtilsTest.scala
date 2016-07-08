package com.twitter.finatra.json.tests.internal.streaming

import com.twitter.finatra.conversions.bytebuffer._
import com.twitter.finatra.json.internal.streaming.ByteBufferUtils
import com.twitter.inject.Test
import com.twitter.io.Buf

class ByteBufferUtilsTest extends Test {

  "ByteBufferUtils.append" in {
    val input = Buf.ByteBuffer.Shared.extract(Buf.Utf8("1"))
    input.get()
    val byteBufferResult = ByteBufferUtils.append(
      input,
      Buf.Utf8(",2"))

    byteBufferResult.utf8str should equal("1,2")
  }
}
