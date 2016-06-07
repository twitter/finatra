package com.twitter.finatra.http

import com.fasterxml.jackson.databind.ObjectWriter
import com.twitter.finagle.http.Request
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.io.Buf
import com.twitter.util.Await

/**
  * Helper class to "stream" JSON to a [[com.twitter.finagle.http.Request]]. Uses the given mapper
  * or the passed optional com.fasterxml.jackson.databind.ObjectWriter for writing JSON values.
  * @param mapper - default [[com.twitter.finatra.json.FinatraObjectMapper]] to use for writing values.
  * @param writer - optional com.fasterxml.jackson.databind.ObjectWriter to use instead of the given mapper.
  */
class StreamingJsonTestHelper(
  mapper: FinatraObjectMapper,
  writer: Option[ObjectWriter] = None) {

  def writeJsonArray(request: Request, seq: Seq[Any], delayMs: Long): Unit = {
    writeAndWait(request, "[")
    writeAndWait(request, writeValueAsString(seq.head))
    for (elem <- seq.tail) {
      writeAndWait(request, "," + writeValueAsString(elem))
      Thread.sleep(delayMs)
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

  private def writeAndWait(request: Request, str: String) {
    println("Write:\t" + str)
    Await.result(
      request.writer.write(Buf.Utf8(str)))
  }

  private def closeAndWait(request: Request) {
    Await.result(
      request.close())
  }
}
