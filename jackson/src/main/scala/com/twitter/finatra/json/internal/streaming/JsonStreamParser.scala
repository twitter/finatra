package com.twitter.finatra.json.internal.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.io.{Buf, Reader}
import javax.inject.{Inject, Singleton}

@Singleton
private[finatra] class JsonStreamParser @Inject()(
  mapper: FinatraObjectMapper) {

  def parseArray[T: Manifest](reader: Reader): AsyncStream[T] = {
    val bufs = AsyncStream.fromReader(reader)
    parseArray[T](bufs)
  }

  def parseArray[T: Manifest](bufs: AsyncStream[Buf]): AsyncStream[T] = {
    val jsonDecoder = new JsonArrayChunker()
    for {
      buf <- bufs
      jsonArrayDelimitedBuf <- AsyncStream.fromSeq(jsonDecoder.decode(buf))
      parsedElem = mapper.parse[T](jsonArrayDelimitedBuf)
    } yield parsedElem
  }
}
