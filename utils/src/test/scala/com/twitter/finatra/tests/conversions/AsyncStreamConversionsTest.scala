package com.twitter.finatra.tests.conversions

import com.twitter.concurrent.AsyncStream
import com.twitter.finatra.conversions.asyncStream._
import com.twitter.inject.Test
import com.twitter.util.{Await, Future, Throw, Try}

class AsyncStreamConversionsTest extends Test {

  "Future[Seq[T]]" in {
    assertAsyncStream(
      Future(Seq(1, 2, 3)).toAsyncStream,
      Seq(1, 2, 3))
  }

  "Future[Option[T]]" in {
    assertAsyncStream(
      Future(Option(1)).toAsyncStream,
      Seq(1))

    assertAsyncStream(
      Future(None).toAsyncStream,
      Seq())
  }

  "Future[T]" in {
    assertAsyncStream(
      Future(1).toAsyncStream,
      Seq(1))
  }

  "Failed Future[T]" in {
    intercept[TestException] {
      Await.result(
        Future.exception[Int](new TestException).toAsyncStream.toSeq())
    }
  }

  "Option[T]" in {
    assertAsyncStream(
      Some(1).toAsyncStream,
      Seq(1))

    assertAsyncStream(
      None.toAsyncStream,
      Seq())
  }

  "Try[T]" in {
    assertAsyncStream(
      Try(1).toAsyncStream,
      Seq(1))
  }

  "Failed Try[T]" in {
    intercept[TestException] {
      Await.result(
        Throw(new TestException).toAsyncStream.toSeq())
    }
  }

  private def assertAsyncStream[T](asyncStream: AsyncStream[T], expected: Seq[T]): Unit = {
    Await.result(asyncStream.toSeq()) should equal(expected)
  }

  class TestException extends Exception

}
