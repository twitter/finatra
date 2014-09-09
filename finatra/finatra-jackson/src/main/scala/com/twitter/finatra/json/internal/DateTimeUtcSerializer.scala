package com.twitter.finatra.json.internal

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{SerializationFeature, SerializerProvider}
import org.joda.time.{DateTime, DateTimeZone}

/**
 * A UTC serializer that forces UTC (unlike the normal jackson-jodatime one that uses the jvm timezone)
 */
object DateTimeUtcSerializer extends StdSerializer[DateTime](classOf[DateTime]) {

  override def serialize(value: DateTime, jgen: JsonGenerator, provider: SerializerProvider) {
    if (provider.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
      jgen.writeNumber(value.getMillis)
    } else {
      jgen.writeString(value.withZone(DateTimeZone.UTC).toString())
    }
  }
}