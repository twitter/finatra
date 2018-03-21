package com.twitter.finatra.http.response

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Response, Status}
import com.twitter.inject.Logging
import com.twitter.io.Reader.ReaderDiscarded
import com.twitter.io.{Buf, Writer}
import com.twitter.util._

import scala.util.control.NonFatal

object StreamingResponse {

  /*
   * Constants used for creating a JSON array from a callback returning an AsyncStream
   */
  private val JsonArrayPrefix = Some(Buf.Utf8("["))
  private val JsonArraySeparator = Some(Buf.Utf8(","))
  private val JsonArraySuffix = Some(Buf.Utf8("]"))

  /**
    * Construct a [[StreamingResponse]] from an `AsyncStream` and an `AsyncStream[T] => AsyncStream[(U, Buf)]`
    *
    * A [[StreamingResponse]] is useful for streaming data back to the client in chunks: data will
    * be rendered to the client as it resolves from the `AsyncStream` while also utilizing the
    * back-pressure mechanisms provide by the underlying transport, preventing unnecessary resource
    * consumption for properly constructed `AsyncStream`s.
    *
    * @param streamTransformer Function which converts AsyncStream[T] to AsyncStream[(U, Buf)]
    * @param status Status code of the generated response.
    * @param headers Headers for the generated response.
    * @param onDisconnect A hook to clean up resources upon disconnection, either normally or exceptionally.
    * @param closeGracePeriod The grace period provided to close the Response.writer.
    * @param asyncStream The data that will represent the body of the response.
    * @tparam T The incoming type.
    * @tparam U An auxillary type passed to onWrite which may be helpful when executing the onWrite callback.
    *
    */
  def apply[T, U](
    streamTransformer: AsyncStream[T] => AsyncStream[(U, Buf)],
    status: Status,
    headers: Map[String, String],
    onWrite: (U, Buf) => Try[Unit] => Unit,
    onDisconnect: () => Unit,
    closeGracePeriod: Duration
  )(asyncStream: => AsyncStream[T]): StreamingResponse[T, U] = {
    new StreamingResponse[T, U](
      streamTransformer,
      status,
      headers,
      asyncStream,
      onWrite,
      onDisconnect,
      closeGracePeriod
    )
  }

  /**
   * Construct a [[StreamingResponse]] from an `AsyncStream` and a `toBuf`
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
   * @param closeGracePeriod The grace period provided to the closeable in closeOnFinish
   * @param asyncStream The data that will represent the body of the response.
   */
  def apply[T](
    toBuf: T => Buf,
    status: Status = Status.Ok,
    headers: Map[String, String] = Map(),
    prefix: Option[Buf] = None,
    separator: Option[Buf] = None,
    suffix: Option[Buf] = None,
    closeOnFinish: Closable = Closable.nop,
    closeGracePeriod: Duration = Duration.Zero
  )(asyncStream: => AsyncStream[T]): StreamingResponse[T, Unit] = {

    val chainedTransformers: AsyncStream[T] => AsyncStream[(Unit, Buf)] = {
      (StreamingResponseUtils.toBufTransformer(toBuf) _)
          .andThen(StreamingResponseUtils.separatorTransformer(separator) _)
          .andThen(StreamingResponseUtils.prefixTransformer(prefix) _)
          .andThen(StreamingResponseUtils.suffixTransformer(suffix) _)
          .andThen(StreamingResponseUtils.tupleTransformer(()) _)
    }

    def onDisconnect(): Unit = {
      closeOnFinish.close(closeGracePeriod)
      ()
    }

    def onWrite(ignored: Unit, buf: Buf)(t: Try[Unit]): Unit = ()

    StreamingResponse[T, Unit](
      streamTransformer = chainedTransformers,
      status = status,
      headers = headers,
      onDisconnect = onDisconnect,
      onWrite = onWrite,
      closeGracePeriod = closeGracePeriod
    ) {
      asyncStream
    }
  }

  def jsonArray[T](
    toBuf: T => Buf,
    status: Status = Status.Ok,
    headers: Map[String, String] = Map(),
    closeOnFinish: Closable = Closable.nop,
    asyncStream: => AsyncStream[T]
  ): StreamingResponse[T, Unit] = {
    StreamingResponse.apply(
      toBuf = toBuf,
      status = status,
      headers = headers,
      prefix = JsonArrayPrefix,
      separator = JsonArraySeparator,
      suffix = JsonArraySuffix,
      closeOnFinish = closeOnFinish) {
        asyncStream
    }
  }
}

class StreamingResponse[T, U] private (
  streamTransformer: AsyncStream[T] => AsyncStream[(U, Buf)],
  status: Status,
  headers: Map[String, String],
  asyncStream: => AsyncStream[T],
  onWrite: (U, Buf) => Try[Unit] => Unit,
  onDisconnect: () => Unit,
  closeGracePeriod: Duration
) extends Logging
{

  def toFutureFinagleResponse: Future[Response] = {
    val response = Response()
    response.setChunked(true)
    response.statusCode = status.code
    setHeaders(headers, response)
    val writer = response.writer

    /* Orphan the future which writes to our response thread */
    write(writer).onSuccess { _ =>
      debug("Success writing to chunked response")
    }.onFailure {
      case e: ReaderDiscarded =>
        info(s"Failure writing to chunked response: ${e.getMessage}")
      case NonFatal(e) =>
        error("Unexpected failure writing to chunked response", e)
    }.ensure {
      debug("Closing chunked response")
      response.writer
        .close(closeGracePeriod)
        .ensure {
          onDisconnect()
        }
    }

    Future.value(response)
  }

  private[this] def write(writer: Writer): Future[Unit] = {
    streamTransformer(asyncStream).foreachF { case (item, buf) =>
      writer.write(buf).respond(onWrite(item, buf))
    }
  }

  private[this] def setHeaders(headersOpt: Map[String, String], response: Response) = {
    for ((k, v) <- headers) {
      response.headerMap.set(k, v)
    }
  }
}

