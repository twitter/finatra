package com.twitter.finatra.json.internal

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.joda.time.{DateTime, DateTimeZone}

/**
 * A UTC deserializer that forces UTC (unlike the normal jackson-jodatime one that uses the jvm timezone)
 */
object DateTimeUtcDeserializer extends StdDeserializer[DateTime](classOf[DateTime]) {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): DateTime = {
    try {
      jp.getCurrentToken match {
        case JsonToken.VALUE_NUMBER_INT =>
          new DateTime(jp.getLongValue, DateTimeZone.UTC)
        case JsonToken.VALUE_STRING =>
          new DateTime(jp.getText.trim, DateTimeZone.UTC)
        case _ =>
          throw ctxt.mappingException(handledType())
      }
    } catch {
      case e: IllegalArgumentException =>
        throw ctxt.mappingException("error parsing '" + jp.getText + "' as an ISO 8601 datetime")
    }
  }
}