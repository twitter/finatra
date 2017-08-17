package com.twitter.finatra.http.tests.response

import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.response.StreamingResponse
import com.twitter.inject.Test
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Await, Awaitable, Closable, Future, Time}

class StreamingResponseTest extends Test {

  private class TestClosable extends Closable {
    @volatile var closed: Boolean = false
    def close(deadline: Time): Future[Unit] = {
      closed = true
      Future.Unit
    }
  }

  private def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, 5.seconds)

  private def fromStream(closable: Closable, stream: AsyncStream[Buf]): Response = {
    val streamingResponse = StreamingResponse(identity[Buf], closeOnFinish = closable)(stream)
    await(streamingResponse.toFutureFinagleResponse)
  }

  private def burnLoop(reader: Reader): Future[Unit] = reader.read(Int.MaxValue).flatMap {
    case Some(_) => burnLoop(reader)
    case None => Future.Unit
  }

  private def assertClosableClosed(stream: AsyncStream[Buf]): Unit = {
    val closable = new TestClosable
    val response = fromStream(closable, stream)
    await(burnLoop(response.reader))
    assert(closable.closed)
  }

  test("closes the Closable on successful completion of a successful AsyncStream") {
    Seq(
      AsyncStream.empty[Buf],
      AsyncStream(Buf.Empty, Buf.Empty),
      AsyncStream(Buf.Utf8("foo")),
      AsyncStream.fromFuture(Future(Buf.Utf8("foo")))
    ).foreach { stream =>
      assertClosableClosed(stream)
    }
  }

  test("closes the Closable if the stream throws an exception") {
    assertClosableClosed(AsyncStream.exception(new Exception("boom")))
  }

  test("closes the Closable if the response body is discarded") {
    val closable = new TestClosable
    val response = fromStream(closable, AsyncStream(Buf.Utf8("foo"), Buf.Utf8("bar")))

    // Note: It is pretty fortunate that the discard method immediately causes
    // the closable to be closed, but this need not be the case in the future.
    response.reader.discard()
    assert(closable.closed)
  }
}
