package com.twitter.finatra.http

import com.fasterxml.jackson.databind.ObjectWriter
import com.twitter.finagle.http.Request
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.utils.FutureUtils
import com.twitter.io.Buf
import com.twitter.util.{Duration, Future}

/**
 * Helper class to "stream" JSON to a [[com.twitter.finagle.http.Request]]. Uses the given mapper
 * or the passed optional com.fasterxml.jackson.databind.ObjectWriter for writing JSON values.
 *
 * @param mapper - default [[com.twitter.finatra.jackson.ScalaObjectMapper]] to use for writing values.
 * @param writer - optional com.fasterxml.jackson.databind.ObjectWriter to use instead of the given mapper.
 */
class StreamingJsonTestHelper(mapper: ScalaObjectMapper, writer: Option[ObjectWriter] = None) {

  def writeJsonArray(request: Request, seq: Seq[Any], delayMs: Long): Future[Unit] =
    for {
      _ <- write(request, "[")
      _ <- write(request, writeValueAsString(seq.head))
      _ <- writeElements(request, seq.tail, delayMs)
      _ <- write(request, "]")
      closed <- close(request)
    } yield closed

  /* Private */

  private def writeValueAsString(any: Any): String = writer match {
    case Some(objectWriter) =>
      objectWriter.writeValueAsString(any)
    case _ =>
      mapper.writeValueAsString(any)
  }

  private def writeElements(request: Request, seq: Seq[Any], delayMs: Long): Future[Unit] =
    Future
      .traverseSequentially(seq) { elem =>
        FutureUtils.scheduleFuture(Duration.fromMilliseconds(delayMs))(
          write(request, "," + writeValueAsString(elem)))
      }.unit

  private def write(request: Request, str: String): Future[Unit] =
    request.writer.write(Buf.Utf8(str))

  private def close(request: Request): Future[Unit] = request.close()
}
