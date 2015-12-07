package com.twitter.inject.server

import com.twitter.concurrent.AsyncStream
import com.twitter.io.{Buf, Reader}

object AsyncStreamUtils {

  def readerToAsyncStream(reader: Reader): AsyncStream[Buf] = {
    for {
      optBuf <- AsyncStream.fromFuture(reader.read(Int.MaxValue))
      result <- optBuf match {
        case None => AsyncStream.empty[Buf]
        case Some(buf) =>
          buf +:: readerToAsyncStream(reader)
      }
    } yield result
  }

}
