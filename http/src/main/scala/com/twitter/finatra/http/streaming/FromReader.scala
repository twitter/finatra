package com.twitter.finatra.http.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.io.Reader

/**
 * Used for StreamingRequest to convert stream from Reader to other primitive stream types.
 * There is currently support for [[com.twitter.io.Reader]] and
 * [[com.twitter.concurrent.AsyncStream]].
 */
private[http] sealed abstract class FromReader[F[_]] {
  def apply[A](r: Reader[A]): F[A]
}

private[http] object FromReader {
  implicit val ReaderIdentity: FromReader[Reader] = new FromReader[Reader] {
    def apply[A](r: Reader[A]): Reader[A] = r
  }

  implicit val AsyncStreamFromReader: FromReader[AsyncStream] = new FromReader[AsyncStream] {
    def apply[A](r: Reader[A]): AsyncStream[A] = Reader.toAsyncStream(r)
  }
}
