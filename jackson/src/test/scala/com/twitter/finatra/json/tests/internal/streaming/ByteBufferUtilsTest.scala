package com.twitter.finatra.json.tests.internal.streaming

import com.twitter.inject.conversions.bytebuffer._
import com.twitter.finatra.json.internal.streaming.ByteBufferUtils
import com.twitter.inject.WordSpecTest
import com.twitter.io.Buf

class ByteBufferUtilsTest extends WordSpecTest {

  "ByteBufferUtils.append" in {
    val input = Buf.ByteBuffer.Shared.extract(Buf.Utf8("1"))
    input.get()
    val byteBufferResult = ByteBufferUtils.append(
      input,
      Buf.Utf8(",2"))

    byteBufferResult.utf8str should equal("1,2")
  }
}
