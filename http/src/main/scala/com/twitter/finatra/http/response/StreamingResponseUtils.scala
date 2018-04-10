package com.twitter.finatra.http.response

import com.twitter.concurrent.AsyncStream
import com.twitter.io.Buf

object StreamingResponseUtils {
  def prefixTransformer(prefix: Option[Buf])(in: AsyncStream[Buf]) = prefix.map(p => p +:: in).getOrElse(in)

  def suffixTransformer(suffix: Option[Buf])(in: AsyncStream[Buf]) = suffix.map(s => in ++ AsyncStream.of(s)).getOrElse(in)

  def toBufTransformer[T](toBuf: T => Buf)(in: AsyncStream[T]) = in.map(toBuf)

  def separatorTransformer(separator: Option[Buf])(stream: AsyncStream[Buf]) = {
    def addSeparator(stream: AsyncStream[Buf])(separator: Buf): AsyncStream[Buf] = {
      stream.take(1) ++ stream.drop(1).map { buf =>
        separator.concat(buf)
      }
    }

    separator.map(addSeparator(stream)).getOrElse(stream)
  }

  def tupleTransformer[T](toAdd: T)(in: AsyncStream[Buf]): AsyncStream[(T, Buf)] = in.map(elem => (toAdd, elem))

}
