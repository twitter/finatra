package com.twitter.finatra.http.response

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Response, Status}
import com.twitter.inject.Logging
import com.twitter.io.Reader.ReaderDiscarded
import com.twitter.io.{Buf, Writer}
import com.twitter.util.{Closable, Duration, Future}

import scala.util.control.NonFatal

object StreamingResponse {

  /*
   * Constants used for creating a JSON array from a callback returning an AsyncStream
   */
  private val JsonArrayPrefix = Some(Buf.Utf8("["))
  private val JsonArraySeparator = Some(Buf.Utf8(","))
  private val JsonArraySuffix = Some(Buf.Utf8("]"))

  /**
   * Construct a [[StreamingResponse]] from an `AsyncStream`.
   *
   * A [[StreamingResponse]] is useful for streaming data back to the client in chunks: data will
   * be rendered to the client as it resolves from the `AsyncStream` while also utilizing the
   * back-pressure mechanisms provide by the underlying transport, preventing unnecessary resource
   * consumption for properly constructed `AsyncStream`s.
   *
   * @param toBuf Function for converting messages to a binary `Buf` representation.
   * @param status Status code of the generated response.
   * @param headers Headers for the generated response.
   * @param prefix Optional first chunk of the response body. Note that a separator will not be
   *               added between the prefix and the elements of the `AsyncStream`.
   * @param separator Separator to be interleaved between each result of the `AsyncStream`.
   * @param suffix Suffix to append to the end of the `AsyncStream`. Note that a separator will
   *               not be included between the last stream chunk and the suffix.
   * @param closeOnFinish A hook for cleaning up resources after completion of the rendering
   *                      process. Note that this will be called regardless of whether rendering
   *                      the body is successful.
   * @param asyncStream The data that will represent the body of the response.
   */
  def apply[T](
    toBuf: T => Buf,
    status: Status = Status.Ok,
    headers: Map[String, String] = Map(),
    prefix: Option[Buf] = None,
    separator: Option[Buf] = None,
    suffix: Option[Buf] = None,
    closeOnFinish: Closable = Closable.nop
  )(asyncStream: => AsyncStream[T]) = {
    new StreamingResponse[T](
      status = status,
      toBuf = toBuf,
      headers = headers,
      prefix = prefix,
      separator = separator,
      suffix = suffix,
      asyncStream = () => asyncStream,
      closeOnFinish = closeOnFinish
    )
  }

  def jsonArray[T](
    toBuf: T => Buf,
    status: Status = Status.Ok,
    headers: Map[String, String] = Map(),
    closeOnFinish: Closable = Closable.nop,
    asyncStream: => AsyncStream[T]
  ) = {

    new StreamingResponse[T](
      toBuf = toBuf,
      status = status,
      headers = headers,
      prefix = JsonArrayPrefix,
      separator = JsonArraySeparator,
      suffix = JsonArraySuffix,
      asyncStream = () => asyncStream,
      closeOnFinish = closeOnFinish
    )
  }
}

/**
 * Representation of a streaming HTTP response based on the `AsyncStream` construct.
 */
class StreamingResponse[T] private (
  toBuf: T => Buf,
  status: Status,
  headers: Map[String, String],
  prefix: Option[Buf],
  separator: Option[Buf],
  suffix: Option[Buf],
  asyncStream: () => AsyncStream[T],
  closeOnFinish: Closable,
  closeGracePeriod: Duration = Duration.Zero
) extends Logging {

  def toFutureFinagleResponse: Future[Response] = {
    val response = Response()
    response.setChunked(true)
    response.statusCode = status.code
    setHeaders(headers, response)
    val writer = response.writer

    /* Orphan the future which writes to our response thread */
    (for {
      _ <- writePrefix(writer)
      _ <- addSeparatorIfPresent(asyncStream().map(toBuf)).foreachF(writer.write)
      result <- writeSuffix(writer)
    } yield result).onSuccess { _ =>
      debug("Success writing to chunked response")
    }.onFailure {
      case e: ReaderDiscarded =>
        info(s"Failure writing to chunked response: ${e.getMessage}")
      case NonFatal(e) =>
        error("Unexpected failure writing to chunked response", e)
    }.ensure {
      debug("Closing chunked response")
      Closable.all(response.writer, closeOnFinish).close(closeGracePeriod)
    }

    Future.value(response)
  }

  private[this] def writePrefix(writer: Writer): Future[Unit] =
    writeOption(prefix, writer)

  private[this] def writeSuffix(writer: Writer): Future[Unit] =
    writeOption(suffix, writer)

  private[this] def writeOption(data: Option[Buf], writer: Writer): Future[Unit] = data match {
    case Some(data) => writer.write(data)
    case None => Future.Unit
  }

  private[this] def addSeparatorIfPresent(stream: => AsyncStream[Buf]): AsyncStream[Buf] =
    separator match {
      case Some(sep) => addSeparator(stream, sep)
      case None => stream
    }

  private[this] def addSeparator(stream: => AsyncStream[Buf], separator: Buf): AsyncStream[Buf] = {
    stream.take(1) ++ (stream.drop(1).map { buf =>
      separator.concat(buf)
    })
  }

  private[this] def setHeaders(headersOpt: Map[String, String], response: Response) = {
    for ((k, v) <- headers) {
      response.headerMap.set(k, v)
    }
  }
}

