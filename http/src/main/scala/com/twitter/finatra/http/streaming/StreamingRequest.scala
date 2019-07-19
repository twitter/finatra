package com.twitter.finatra.http.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.Request
import com.twitter.finatra.json.internal.streaming.JsonStreamParser
import scala.language.higherKinds

private[http] object StreamingRequest {

  /**
   * Convert a Request to a StreamingRequest over the provided primitive stream type.
   */
  def apply[F[_]: FromReader, A: Manifest](
    parser: JsonStreamParser,
    request: Request
  ): StreamingRequest[F, A] =
    new StreamingRequest(parser, request)

  /**
   * Convert a Request To a StreamingRequest over AsyncStream[A]
   */
  def fromRequestToAsyncStream[A: Manifest](
    parser: JsonStreamParser,
    request: Request
  ): StreamingRequest[AsyncStream, A] =
    new StreamingRequest[AsyncStream, A](parser, request)
}

/**
 * StreamingRequest is an abstraction over an input Primitive Stream - Reader or AsyncStream.
 * It carries the stream as well as the original Http Request.
 *
 * @param parser Parse bufs to objects.
 * @param request Finagle Http Request.
 * @tparam F The Primitive Stream type.
 * @tparam A The type of streaming values.
 */
final class StreamingRequest[F[_]: FromReader, A: Manifest] private (
  parser: JsonStreamParser,
  val request: Request) {

  /**
   * Convert the Reader[Buf] to the provided stream primitive.
   */
  val stream: F[A] = implicitly[FromReader[F]].apply(parser.parseJson[A](request.reader))
}
