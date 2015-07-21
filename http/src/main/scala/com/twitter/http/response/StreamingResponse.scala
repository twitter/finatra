package com.twitter.finatra.http.response

import com.twitter.concurrent.exp.AsyncStream
import com.twitter.finagle.http.{Response, Status}
import com.twitter.inject.Logging
import com.twitter.io.{Buf, Writer}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpResponseStatus

object StreamingResponse {

  /*
   * Constants used for creating a JSON array from a callback returning an AsyncStream
   */
  private val JsonArrayPrefix = Some(Buf.Utf8("["))
  private val JsonArraySeparator = Some(Buf.Utf8(","))
  private val JsonArraySuffix = Some(Buf.Utf8("]"))

  def apply[T](toBuf: T => Buf)(stream: => AsyncStream[T]) = {
    new StreamingResponse[T](toBuf, asyncStream = stream)
  }

  def jsonArray[T](
    toBuf: T => Buf,
    status: HttpResponseStatus = Status.Ok,
    asyncStream: AsyncStream[T]) = {

    new StreamingResponse[T](
      toBuf = toBuf ,
      status = status,
      prefixOpt = JsonArrayPrefix,
      separatorOpt = JsonArraySeparator,
      suffixOpt = JsonArraySuffix,
      asyncStream = asyncStream)
  }
}

case class StreamingResponse[T](
  toBuf: T => Buf,
  status: HttpResponseStatus = Status.Ok,
  prefixOpt: Option[Buf] = None,
  separatorOpt: Option[Buf] = None,
  suffixOpt: Option[Buf] = None,
  asyncStream: AsyncStream[T])
  extends Logging {

  def toFutureFinagleResponse: Future[Response] = {
    val response = Response()
    response.setChunked(true)
    val writer = response.writer

    /* Orphan the future which writes to our response thread */
    (for {
      _ <- writePrefix(writer)
      bufs = asyncStream map toBuf
      _ <- addSeparatorIfPresent(bufs) foreachF writer.write
      result <- writeSuffix(writer)
    } yield result) onSuccess { r =>
      debug("Success writing to chunked response")
    } onFailure { e =>
      warn("Failure writing to chunked response", e)
    } ensure {
      debug("Closing chunked response")
      response.close()
    }

    Future.value(response)
  }

  private def writePrefix(writer: Writer) = {
    prefixOpt map writer.write getOrElse Future.Unit
  }

  private def writeSuffix(writer: Writer) = {
    suffixOpt map writer.write getOrElse Future.Unit
  }

  private def addSeparatorIfPresent(stream: AsyncStream[Buf]): AsyncStream[Buf] = {
    separatorOpt map { sep: Buf =>
      addSeparator(stream, sep)
    } getOrElse {
      stream
    }
  }

  private def addSeparator(stream: AsyncStream[Buf], separator: Buf): AsyncStream[Buf] = {
    stream.take(1) ++ (stream.drop(1) map { buf =>
      separator.concat(buf)
    })
  }
}
