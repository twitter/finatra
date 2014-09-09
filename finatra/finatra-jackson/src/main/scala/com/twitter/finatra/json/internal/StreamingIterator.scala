package com.twitter.finatra.json.internal

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.Logging

class StreamingIterator[T: Manifest](
  mapper: FinatraObjectMapper,
  arrayName: String,
  parser: JsonParser)
  extends Iterator[T]
  with Logging {

  /* Constructor */

  initParser()

  /* Public */

  def hasNext = {
    parser.getCurrentToken != JsonToken.END_ARRAY && !parser.isClosed
  }

  def next() = {
    if (hasNext) {
      val value = mapper.parse[T](parser)
      parser.nextToken()
      value
    }
    else {
      Iterator.empty.next()
    }
  }

  /* Private */

  private def initParser() {
    /* skip to first token in stream if parser is open */
    assert(!parser.isClosed)
    parser.nextToken()

    /* Skip to named JSON array if specified */
    if (arrayName.nonEmpty) {
      skipToNamedArray()
    }

    /* Verify we're at the start of an array and then skip to the first element in that array */
    assert(parser.getCurrentToken == JsonToken.START_ARRAY)
    parser.nextToken()
  }

  private def skipToNamedArray() {
    while (!parser.isClosed && parser.getCurrentName != arrayName) {
      trace("Skipping " + parser.getCurrentToken + "\t" + parser.getCurrentName)
      parser.nextToken
    }
    parser.nextToken()
  }
}
