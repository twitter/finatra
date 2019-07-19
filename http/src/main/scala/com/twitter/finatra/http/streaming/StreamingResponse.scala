package com.twitter.finatra.http.streaming

import com.twitter.finagle.http.{Response, Status, Version}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.io.Buf
import com.twitter.util.Future
import scala.language.higherKinds

/**
 * StreamingResponse is an abstraction over an output Primitive Stream - Reader or AsyncStream.
 * It carries the output stream as well as some HTTP Response metadata.
 *
 * @param mapper Server's configured FinatraObjectMapper.
 * @param stream The output stream.
 * @param status Represents an HTTP status code.
 * @param headers A Map of message headers.
 * @tparam F The Primitive Stream type.
 * @tparam A The type of streaming values.
 *
 * @note Users should construct this via c.t.finatra.http.response.ResponseBuilder#streaming
 */
final class StreamingResponse[F[_]: ToReader, A] private[http] (
  mapper: FinatraObjectMapper,
  stream: F[A],
  status: Status = Status.Ok,
  headers: Map[String, Seq[String]] = Map.empty) {

  private[this] val reader = implicitly[ToReader[F]].apply(stream).map {
    case buf: Buf => buf
    case str: String => Buf.Utf8(str)
    case any => mapper.writeValueAsBuf(any)
  }

  private[this] def setHeaders(response: Response, headerMap: Map[String, Seq[String]]): Unit = {
    for {
      (key, values) <- headerMap
      value <- values
    } response.headerMap.add(key, value)
  }

  /**
   * Construct a Future of Finagle Http Response via the output stream and
   * some HTTP Response metadata.
   */
  def toFutureResponse(): Future[Response] = {
    val response = Response(Version.Http11, status, reader)
    setHeaders(response, headers)
    Future.value(response)
  }
}
