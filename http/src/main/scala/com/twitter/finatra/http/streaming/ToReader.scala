package com.twitter.finatra.http.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.io.Reader

/**
 * Used for StreamingResponse to convert stream from other primitive stream types to Reader.
 * There is currently support for [[com.twitter.io.Reader]] and
 * [[com.twitter.concurrent.AsyncStream]].
 */
sealed abstract class ToReader[F[_]] {
  def apply[A](f: F[A]): Reader[A]
}

object ToReader {
  implicit val ReaderIdentity: ToReader[Reader] = new ToReader[Reader] {
    def apply[A](r: Reader[A]): Reader[A] = r
  }

  implicit val AsyncStreamToReader: ToReader[AsyncStream] = new ToReader[AsyncStream]{
    def apply[A](as: AsyncStream[A]): Reader[A] = Reader.fromAsyncStream(as)
  }
}
