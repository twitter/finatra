package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.twitter.finatra.json.internal.caseclass.exceptions.FinatraJsonMappingException
import com.twitter.util.{Return, Try}
import org.joda.time.DateTime

/**
 * A Datetime deserializer with improved exception handling (compared to jackson-datatype-joda)
 */
private[finatra] object FinatraDatetimeDeserializer extends StdDeserializer[DateTime](classOf[DateTime]) {

  def deserialize(jp: JsonParser, ctxt: DeserializationContext): DateTime = {
    try {
      jp.getCurrentToken match {
        case JsonToken.VALUE_NUMBER_INT =>
          val value = jp.getLongValue
          if (value < 0)
            throw new FinatraJsonMappingException("field cannot be negative")
          else
            new DateTime(jp.getLongValue)
        case JsonToken.VALUE_STRING =>
          val value = jp.getText.trim
          if (value.isEmpty)
            throw new FinatraJsonMappingException("field cannot be empty")
          else
            // First, attempt to convert as a String value (for backwards-compatibility),
            // Long millis will fail and we then attempt to parse with value.toLong
            Try(new DateTime(value)) match {
              case Return(datetime) =>
                datetime
              case _ =>
                new DateTime(value.toLong)
            }
        case _ =>
          throw ctxt.mappingException(handledType())
      }
    } catch {
      case e: IllegalArgumentException =>
        throw new FinatraJsonMappingException("error parsing '" + jp.getText + "' into an ISO 8601 datetime")
    }
  }
}