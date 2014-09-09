package com.twitter.finatra.json.internal

import com.fasterxml.jackson.core.{JsonToken, JsonParser}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ObjectNode, JsonNodeFactory}
import com.twitter.finatra.json.FinatraObjectMapper

object JsonStreamParseResult {

  /**
   * When a JSON object has an array field,
   * this method returns a result object with all elements up to the array,
   * and then a JsonArrayIterator for the array.
   */
  def create[T: Manifest](
    mapper: FinatraObjectMapper,
    parser: JsonParser,
    factory: JsonNodeFactory,
    arrayName: String): JsonStreamParseResult[T] = {

    assert(!parser.isClosed)
    parser.nextToken() //skip to START_OBJECT
    parser.nextToken() //skip to first FIELD_NAME

    val elementsPriorToArray = skipToArrayFieldName(parser, factory, arrayName)

    parser.nextToken() //skip to START_ARRAY
    assert(parser.getCurrentToken == JsonToken.START_ARRAY)

    JsonStreamParseResult[T](
      preArrayJsonNode = elementsPriorToArray,
      arrayIterator = new JsonArrayIterator[T](mapper, parser))
  }

  /**
   * Skip to named array and return elements found prior to the array
   */
  private def skipToArrayFieldName[T: Manifest](
    parser: JsonParser,
    factory: JsonNodeFactory,
    arrayName: String): ObjectNode = {

    /* JsonNode of all elements seen prior to the array */
    val jsonNode = factory.objectNode()

    /* Find named JSON array while capturing elements along the way */
    while (!parser.isClosed && parser.getCurrentName != arrayName) {
      val fieldName = parser.getCurrentName
      parser.nextToken() //skip to field value

      jsonNode.set(
        fieldName,
        parser.readValueAsTree.asInstanceOf[JsonNode])

      parser.nextToken() //skip to next field
    }

    if (parser.isClosed) {
      throw new JsonArrayNotFoundException(arrayName)
    }

    jsonNode
  }
}

case class JsonStreamParseResult[T](
  preArrayJsonNode: JsonNode,
  arrayIterator: Iterator[T])
