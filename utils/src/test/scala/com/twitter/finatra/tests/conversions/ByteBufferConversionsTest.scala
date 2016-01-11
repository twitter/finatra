package com.twitter.finatra.tests.conversions

import com.twitter.finatra.conversions.bytebuffer._
import com.twitter.inject.Test
import com.twitter.io.Buf

class ByteBufferConversionsTest extends Test {

  "debug output" in {
    val buf = Buf.Utf8("hello")
    val bb = Buf.ByteBuffer.Shared.extract(buf)
    bb.debugOutput()
  }

}
