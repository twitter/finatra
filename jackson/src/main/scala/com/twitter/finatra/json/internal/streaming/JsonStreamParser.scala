package com.twitter.finatra.json.internal.streaming

import com.twitter.concurrent.AsyncStream
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.io.{Buf, Reader}
import javax.inject.{Inject, Singleton}

@Singleton
private[finatra] class JsonStreamParser @Inject()(mapper: FinatraObjectMapper) {

  def parseArray[T: Manifest](reader: Reader[Buf]): AsyncStream[T] = {
    val bufs = Reader.toAsyncStream(reader)
    parseArray[T](bufs)
  }

  def parseArray[T: Manifest](bufs: AsyncStream[Buf]): AsyncStream[T] = {
    Reader.toAsyncStream(parseJson(Reader.fromAsyncStream(bufs)))
  }

  def parseJson[T: Manifest](reader: Reader[Buf]): Reader[T] = {
    val asyncJsonParser = new AsyncJsonParser
    // If the deserialized Type is Buf, don't parse.
    if (manifest[T] == manifest[Buf]) {
      reader.asInstanceOf[Reader[T]]
    } else {
      reader.flatMap { buf =>
        val bufs: Seq[Buf] = asyncJsonParser.feedAndParse(buf)
        val values: Seq[T] = bufs.map(mapper.parse[T])
        Reader.fromSeq(values)
      }
    }
  }
}
