package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.joda.time.DateTime

/**
 * A Datetime deserializer with improved exception handling
 */
object FinatraDatetimeDeserializer extends StdDeserializer[DateTime](classOf[DateTime]) {

  def deserialize(jp: JsonParser, ctxt: DeserializationContext): DateTime = {
    try {
      jp.getCurrentToken match {
        case JsonToken.VALUE_NUMBER_INT =>
          val value = jp.getLongValue
          if (value < 0)
            throw ctxt.mappingException("field cannot be negative")
          else
            new DateTime(jp.getLongValue)
        case JsonToken.VALUE_STRING =>
          val value = jp.getText.trim
          if (value.isEmpty)
            throw ctxt.mappingException("field cannot be empty")
          else
            new DateTime(value)
        case _ =>
          throw ctxt.mappingException(handledType())
      }
    } catch {
      case e: IllegalArgumentException =>
        throw ctxt.mappingException("error parsing '" + jp.getText + "' into an ISO 8601 datetime")
    }
  }
}