package com.twitter.inject.server.tests

import com.twitter.inject.Test
import com.twitter.inject.server.AsyncStreamUtils
import com.twitter.io.{Buf, Reader}
import com.twitter.util.Await

class AsyncStreamUtilsTest extends Test {

  "test" in {
    val buf = Buf.Utf8("hello")
    val reader = Reader.fromBuf(buf)
    val asyncStream = AsyncStreamUtils.readerToAsyncStream(reader)
    Await.result(asyncStream.toSeq()) should equal(Seq(buf))
  }

}
