package com.twitter.finatra.http

import com.fasterxml.jackson.databind.ObjectWriter
import com.twitter.finagle.http.Request
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.io.Buf
import com.twitter.util.{Await, Duration}

/**
 * Helper class to "stream" JSON to a [[com.twitter.finagle.http.Request]]. Uses the given mapper
 * or the passed optional com.fasterxml.jackson.databind.ObjectWriter for writing JSON values.
 *
 * @param mapper - default [[com.twitter.finatra.jackson.ScalaObjectMapper]] to use for writing values.
 * @param writer - optional com.fasterxml.jackson.databind.ObjectWriter to use instead of the given mapper.
 */
class StreamingJsonTestHelper(mapper: ScalaObjectMapper, writer: Option[ObjectWriter] = None) {

  def writeJsonArray(request: Request, seq: Seq[Any], delayMs: Long): Unit = {
    writeAndWait(request, "[")
    writeAndWait(request, writeValueAsString(seq.head))

    val tail = seq.tail
    var index = 0
    while (index < tail.length) {
      val elem = tail(index)
      val delay = Duration.fromMilliseconds(delayMs * index)
      // we purposely don't Await to "fire and forget" the (orphaned) futures
      DefaultTimer.doLater(delay)(writeAndWait(request, "," + writeValueAsString(elem)))
      index += 1
    }
    writeAndWait(request, "]")
    closeAndWait(request)
  }

  /* Private */

  private def writeValueAsString(any: Any): String = {
    writer match {
      case Some(objectWriter) =>
        objectWriter.writeValueAsString(any)
      case _ =>
        mapper.writeValueAsString(any)
    }
  }

  private def writeAndWait(request: Request, str: String): Unit = {
    Await.result(request.writer.write(Buf.Utf8(str)))
  }

  private def closeAndWait(request: Request): Unit = {
    Await.result(request.close())
  }
}
