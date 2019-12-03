package com.twitter.finatra.http.response

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Response, Status}
import com.twitter.inject.Logging
import com.twitter.io.{Buf, ReaderDiscardedException, Writer}
import com.twitter.util._
import scala.util.control.NonFatal

@deprecated(
  "Use c.t.finatra.http.response.ResponseBuilder.streaming to construct a " +
    "c.t.finatra.http.streaming.StreamingResponse instead",
  "2019-07-18")
private[twitter] class StreamingResponse[T, U](
  streamTransformer: AsyncStream[T] => AsyncStream[(U, Buf)],
  status: Status,
  headers: Map[String, String],
  asyncStream: => AsyncStream[T],
  onWrite: (U, Buf) => Try[Unit] => Unit,
  onDisconnect: () => Unit,
  closeGracePeriod: Duration)
    extends Logging {

  private[this] val writerRespondFn: Try[Unit] => Unit = {
    case Return(_) =>
      debug("Success writing to chunked response")
    case Throw(e) =>
      e match {
        case _: ReaderDiscardedException =>
          info(s"Failure writing to chunked response: ${e.getMessage}")
        case NonFatal(nf) =>
          error("Unexpected failure writing to chunked response", nf)
      }
  }

  def toFutureFinagleResponse: Future[Response] = {
    val response = Response()
    response.setChunked(true)
    response.statusCode = status.code
    setHeaders(headers, response)
    val writer = response.writer

    /* Orphan the future which writes to our response thread */
    writeToWriter(writer)
      .respond(writerRespondFn)
      .ensure {
        debug("Closing chunked response")
        response.writer
          .close(closeGracePeriod)
          .ensure {
            onDisconnect()
          }
      }

    Future.value(response)
  }

  private[this] def writeToWriter(writer: Writer[Buf]): Future[Unit] = {
    streamTransformer(asyncStream).foreachF {
      case (item, buf) =>
        writer.write(buf).respond(onWrite(item, buf))
    }
  }

  private[this] def setHeaders(headersOpt: Map[String, String], response: Response): Unit = {
    for ((k, v) <- headers) {
      response.headerMap.set(k, v)
    }
  }
}
