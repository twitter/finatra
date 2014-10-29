package com.twitter.finatra.json.internal.streaming

import java.io.Reader

object ReaderIterator {
  def newlineDelimited(reader: Reader): Iterator[Reader] = {
    new ReaderIterator(reader, '\n')
  }
}

/** TODO: Handle escaped delimiters */
class ReaderIterator(
  reader: Reader,
  delimiter: Char)
  extends Iterator[Reader] {

  private val delimSplittingReader =
    new DelimitedReader(reader, delimiter)

  /* Public */

  override def hasNext: Boolean = {
    delimSplittingReader.clearDelimiterFound()
    !delimSplittingReader.isDone
  }

  override def next(): Reader = {
    if (hasNext) {
      delimSplittingReader
    }
    else {
      Iterator.empty.next()
    }
  }
}
