package com.twitter.finatra.http

import com.fasterxml.jackson.databind.ObjectWriter
import com.twitter.finagle.http.Request
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.utils.FutureUtils
import com.twitter.io.Buf
import com.twitter.util.{Await, Duration, Future}

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
    for (elem <- seq.tail) {
      writeAndWait(request, "," + writeValueAsString(elem))
      Thread.sleep(delayMs)
    }
    writeAndWait(request, "]")
    closeAndWait(request)
  }

  def writeJsonArrayNoWait(request: Request, seq: Seq[Any], delayMs: Long): Future[Unit] = {
    for {
      _ <- write(request, "[")
      _ <- write(request, writeValueAsString(seq.head))
      _ = writeElements(request, seq, delayMs)
      _ <- write(request, "]")
    } yield close(request)
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

  private def writeElements(request: Request, seq: Seq[Any], delayMs: Long): Future[Unit] = {
    Future.traverseSequentially(seq.tail) { elem =>
      FutureUtils.scheduleFuture(Duration.fromMilliseconds(delayMs))(write(request, "," + writeValueAsString(elem)))
    }.map(_ => Unit)
  }

  private def writeAndWait(request: Request, str: String): Unit = {
    println(str)
    Await.result(request.writer.write(Buf.Utf8(str)))
  }

  private def write(request: Request, str: String): Future[Unit] = {
    println(str)
    request.writer.write(Buf.Utf8(str))
  }

  private def closeAndWait(request: Request): Unit = {
    Await.result(request.close())
  }

  private def close(request: Request): Future[Unit] = {
    request.close()
  }
}
