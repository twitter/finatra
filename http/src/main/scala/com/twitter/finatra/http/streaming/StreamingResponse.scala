package com.twitter.finatra.http.streaming

import com.twitter.finagle.http.{Response, Status, Version}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.Future

/**
 * StreamingResponse is an abstraction over an output Primitive Stream - Reader or AsyncStream.
 * It carries the output stream as well as some HTTP Response metadata.
 *
 * @param stream The output stream.
 * @tparam F The Primitive Stream type.
 * @tparam A The type of streaming values.
 */
private[http] final case class StreamingResponse[F[_]: ToReader, A] private (
  mapper: FinatraObjectMapper,
  val stream: F[A]) {

  private[this] val reader = implicitly[ToReader[F]].apply(stream).map { i =>
    mapper.writeValueAsBuf(i)
  }

  /**
   * Write the stream to a Finagle Response.
   */
  def toFutureResponse(
    version: Version = Version.Http11,
    status: Status = Status.Ok
  ): Future[Response] = {
    Future.value(Response(version, status, reader))
  }

  /** For Java support */
  def toFutureResponse: Future[Response] = toFutureResponse()
}
