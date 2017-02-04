package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.bytebuffer._
import com.twitter.io.Buf

class ByteBufferConversionsTest extends Test {

  test("debug output") {
    val buf = Buf.Utf8("hello")
    val bb = Buf.ByteBuffer.Shared.extract(buf)
    bb.debugOutput()
  }

}
