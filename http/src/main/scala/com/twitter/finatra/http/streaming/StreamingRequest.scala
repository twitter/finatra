package com.twitter.finatra.http.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.Request
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import com.twitter.io.{Buf, Reader}

private[http] object StreamingRequest {

  def apply[F[_]: FromReader, A: Manifest](
    parser: JsonStreamParser,
    reader: Reader[Buf]
  ): StreamingRequest[F, A] = new StreamingRequest(parser, reader)

  /**
   * Convert a Request To a StreamingRequest over AsyncStream[A]
   */
  def fromRequestToAsyncStream[A: Manifest](
    parser: JsonStreamParser,
    request: Request
  ): StreamingRequest[AsyncStream, A] =
    new StreamingRequest[AsyncStream, A](parser, request.reader)

  /**
   * Convert a Request to StreamingRequest over the provided primitive stream type.
   */
  def fromRequest[F[_]: FromReader, A: Manifest](
    parser: JsonStreamParser,
    request: Request
  ): StreamingRequest[F, A] =
    new StreamingRequest(parser, request.reader)
}

/**
 * StreamingRequest is an abstraction over an input Primitive Stream - Reader or AsyncStream.
 * It carries the stream as well as some Http Request metadata.
 * @param parser Parse bufs to objects.
 * @param reader See [[com.twitter.finagle.http.Message#reader()]].
 * @tparam F The Primitive Stream type.
 * @tparam A The type of streaming values.
 */
private[http] final class StreamingRequest[F[_]: FromReader, A: Manifest] private (
  parser: JsonStreamParser,
  reader: Reader[Buf]) {

  /**
   * Convert the Reader[Buf] to the provided stream primitive.
   */
  val stream: F[A] = implicitly[FromReader[F]].apply(parser.parseJson[A](reader))
}
