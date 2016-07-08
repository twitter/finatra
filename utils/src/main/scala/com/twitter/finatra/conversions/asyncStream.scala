package com.twitter.finatra.conversions

import com.twitter.concurrent.AsyncStream
import com.twitter.concurrent.AsyncStream.{fromFuture, fromSeq}
import com.twitter.util.{Future, Try}

object asyncStream {

  implicit class RichFutureSeq[T](val self: Future[Seq[T]]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      (fromFuture(self) map fromSeq).flatten
    }
  }

  implicit class RichFutureOption[T](val self: Future[Option[T]]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      (self map {_.toSeq}).toAsyncStream
    }
  }

  implicit class RichFuture[T](val self: Future[T]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      AsyncStream.fromFuture(self)
    }
  }

  implicit class RichOption[T](val self: Option[T]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      AsyncStream.fromOption(self)
    }
  }

  implicit class RichTry[T](val self: Try[T]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      AsyncStream.fromFuture(Future.const(self))
    }
  }
}
