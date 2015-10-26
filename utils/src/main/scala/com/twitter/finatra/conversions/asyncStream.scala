package com.twitter.finatra.conversions

import com.twitter.concurrent.AsyncStream
import com.twitter.concurrent.AsyncStream.{fromFuture, fromSeq}
import com.twitter.util._

object asyncStream {

  implicit class RichFutureSeq[T](val futureSeq: Future[Seq[T]]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      (fromFuture(futureSeq) map fromSeq).flatten
    }
  }

  implicit class RichFutureOption[T](val futureOption: Future[Option[T]]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      (futureOption map {_.toSeq}).toAsyncStream
    }
  }

  implicit class RichFuture[T](val future: Future[T]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      AsyncStream.fromFuture(future)
    }
  }

  implicit class RichOption[T](val option: Option[T]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      AsyncStream.fromOption(option)
    }
  }

  implicit class RichTry[T](val trie: Try[T]) extends AnyVal {
    def toAsyncStream: AsyncStream[T] = {
      AsyncStream.fromFuture(Future.const(trie))
    }
  }

}
