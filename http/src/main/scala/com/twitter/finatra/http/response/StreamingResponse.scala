package com.twitter.finatra.http.response

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Response, Status}
import com.twitter.inject.Logging
import com.twitter.io.{Buf, Writer}
import com.twitter.util.Future

object StreamingResponse {

  /*
   * Constants used for creating a JSON array from a callback returning an AsyncStream
   */
  private val JsonArrayPrefix = Some(Buf.Utf8("["))
  private val JsonArraySeparator = Some(Buf.Utf8(","))
  private val JsonArraySuffix = Some(Buf.Utf8("]"))

  def apply[T](
    toBuf: T => Buf,
    status: Status = Status.Ok,
    headers: Map[String, String] = Map(),
    prefix: Option[Buf] = None,
    separator: Option[Buf] = None,
    suffix: Option[Buf] = None)(asyncStream: => AsyncStream[T]) = {
    new StreamingResponse[T](
      status = status,
      toBuf = toBuf,
      headers = headers,
      prefix = prefix,
      separator = separator,
      suffix = suffix,
      asyncStream = asyncStream)
  }

  def jsonArray[T](
    toBuf: T => Buf,
    status: Status = Status.Ok,
    headers: Map[String, String] = Map(),
    asyncStream: AsyncStream[T]) = {

    new StreamingResponse[T](
      toBuf = toBuf ,
      status = status,
      headers = headers,
      prefix = JsonArrayPrefix,
      separator = JsonArraySeparator,
      suffix = JsonArraySuffix,
      asyncStream = asyncStream)
  }
}

case class StreamingResponse[T](
  toBuf: T => Buf,
  status: Status,
  headers: Map[String, String],
  prefix: Option[Buf],
  separator: Option[Buf],
  suffix: Option[Buf],
  asyncStream: AsyncStream[T])
  extends Logging {

  def toFutureFinagleResponse: Future[Response] = {
    val response = Response()
    response.setChunked(true)
    response.setStatusCode(status.code)
    setHeaders(headers, response)
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
    prefix map writer.write getOrElse Future.Unit
  }

  private def writeSuffix(writer: Writer) = {
    suffix map writer.write getOrElse Future.Unit
  }

  private def addSeparatorIfPresent(stream: AsyncStream[Buf]): AsyncStream[Buf] = {
    separator map { sep: Buf =>
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

  private def setHeaders(headersOpt: Map[String, String], response: Response) = {
    for((k,v) <- headers) {
      response.headerMap.set(k, v)
    }
  }
}
