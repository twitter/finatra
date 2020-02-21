package com.twitter.finatra.http.streaming

import com.twitter.finagle.http.{Response, Status, Version}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.io.{Buf, Reader}
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicBoolean
import scala.language.higherKinds

/**
 * StreamingResponse is an abstraction over an output Primitive Stream - Reader or AsyncStream.
 * It carries the output stream as well as some HTTP Response metadata.
 *
 * @param mapper Server's configured ScalaObjectMapper.
 * @param stream The output stream.
 * @param status Represents an HTTP status code.
 * @param headers A Map of message headers.
 * @tparam F The Primitive Stream type.
 * @tparam A The type of streaming values.
 *
 * @note Users should construct this via c.t.finatra.http.response.ResponseBuilder#streaming
 */
final class StreamingResponse[F[_]: ToReader, A: Manifest] private[http] (
  mapper: ScalaObjectMapper,
  stream: F[A],
  status: Status = Status.Ok,
  headers: Map[String, Seq[String]] = Map.empty) {

  private[this] val head = new AtomicBoolean(true)
  private[this] val reader: Reader[Buf] = implicitly[ToReader[F]].apply(stream) match {
    case bufReader if manifest[A] == manifest[Buf] => bufReader.asInstanceOf[Reader[Buf]]
    case anyReader => toJsonArray(anyReader)
  }

  private[this] def toJsonArray(fromReader: Reader[A]): Reader[Buf] = {
    Reader.fromSeq(
      Seq(
        Reader.fromBuf(Buf.Utf8("[")),
        fromReader.map { i =>
          if (head.compareAndSet(true, false)) {
            mapper.writeValueAsBuf(i)
          } else {
            Buf.Utf8(",").concat(mapper.writeValueAsBuf(i))
          }
        },
        Reader.fromBuf(Buf.Utf8("]"))
      )).flatten
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

  /**
   * Get the underlying Buf Reader.
   * If the consumed Stream primitive is not Buf, the returned reader streams a serialized
   * JSON array.
   * If the consumed Stream primitive is Buf, the returned reader streams the same Buf.
   */
  def toBufReader: Reader[Buf] = reader

}
