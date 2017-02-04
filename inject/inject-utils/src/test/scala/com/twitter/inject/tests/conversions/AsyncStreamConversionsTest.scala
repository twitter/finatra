package com.twitter.inject.tests.conversions

import com.twitter.concurrent.AsyncStream
import com.twitter.inject.Test
import com.twitter.inject.conversions.asyncStream._
import com.twitter.util.{Await, Future, Throw, Try}

class AsyncStreamConversionsTest extends Test {

  test("Future[Seq[T]]") {
    assertAsyncStream(
      Future(Seq(1, 2, 3)).toAsyncStream,
      Seq(1, 2, 3))
  }

  test("Future[Option[T]]") {
    assertAsyncStream(
      Future(Option(1)).toAsyncStream,
      Seq(1))

    assertAsyncStream(
      Future(None).toAsyncStream,
      Seq())
  }

  test("Future[T]") {
    assertAsyncStream(
      Future(1).toAsyncStream,
      Seq(1))
  }

  test("Failed Future[T]") {
    intercept[TestException] {
      Await.result(
        Future.exception[Int](new TestException).toAsyncStream.toSeq())
    }
  }

  test("Option[T]") {
    assertAsyncStream(
      Some(1).toAsyncStream,
      Seq(1))

    assertAsyncStream(
      None.toAsyncStream,
      Seq())
  }

  test("Try[T]") {
    assertAsyncStream(
      Try(1).toAsyncStream,
      Seq(1))
  }

  test("Failed Try[T]") {
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
