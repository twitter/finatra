package com.twitter.finatra.json.internal

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.twitter.finatra.json.FinatraObjectMapper

class JsonArrayIterator[T: Manifest](
  mapper: FinatraObjectMapper,
  parser: JsonParser)
  extends Iterator[T] {

  /* Constructor */

  //Verify we're at the start of an array and then skip to the first element in that array */
  if (parser.getCurrentToken == null) {
    parser.nextToken()
  }
  assert(parser.getCurrentToken == JsonToken.START_ARRAY)
  parser.nextToken()

  /* Public */

  def hasNext = {
    val hasNextResult = parser.getCurrentToken != JsonToken.END_ARRAY && !parser.isClosed
    if (!hasNextResult) {
      parser.close() //close parser so that we skip past all the elements after the array
    }
    hasNextResult
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
}
