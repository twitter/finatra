package com.twitter.finatra.http.tests.response

import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.http.response.StreamingResponse
import com.twitter.inject.Test
import com.twitter.io.Reader.ReaderDiscarded
import com.twitter.io.{Buf, Reader}
import com.twitter.util._

import scala.collection.mutable

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

  private def burnLoop(reader: Reader[Buf]): Future[Unit] = reader.read(Int.MaxValue).flatMap {
    case Some(_) => burnLoop(reader)
    case None => Future.Unit
  }

  private def assertClosableClosed(stream: AsyncStream[Buf]): Unit = {
    val closable = new TestClosable
    val response = fromStream(closable, stream)
    await(burnLoop(response.reader))
    assert(closable.closed)
  }

  private def failWriteWith(t: Throwable): Unit = {
    val stream = infiniteStream(Buf.Utf8("foo"))
    val closable = new TestClosable

    val response = fromStream(closable, stream)
    response.writer.fail(t)
    val thrownException = intercept[Throwable] {
      await(burnLoop(response.reader))
    }
    assert(thrownException == t)
    assert(closable.closed)
  }

  private def infiniteStream[T](item: T): AsyncStream[T] = {
    item +:: infiniteStream(item)
  }

  test("write failures with ReaderDiscarded") {
    failWriteWith(new ReaderDiscarded)
  }

  test("write failures with other exception") {
    failWriteWith(new RuntimeException("FORCED EXCEPTION"))
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

  test("runs the responder after the writes succeed") {
    val stream = AsyncStream.fromSeq(Seq("first item", "second item"))
    val results: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty

    def transformer(stream: AsyncStream[String]): AsyncStream[(String, Buf)] = {
      def toBuf(item: String) = Buf.Utf8(item)

      stream.map(item => (item, toBuf(item)))
    }

    def responder(item: String, b: Buf)(t: Try[Unit]): Unit = {
      results += item
    }

    val streamingResponse = StreamingResponse[String, String](
      transformer,
      Status.Ok,
      Map.empty,
      responder,
      () => (),
      Duration.Zero
    )(stream)

    val response = await(streamingResponse.toFutureFinagleResponse)
    await(burnLoop(response.reader))

    assert(results == Seq("first item", "second item"))
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
