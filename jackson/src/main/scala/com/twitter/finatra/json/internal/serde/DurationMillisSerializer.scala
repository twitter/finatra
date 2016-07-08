package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.joda.time.Duration

private[finatra] object DurationMillisSerializer extends StdSerializer[Duration](classOf[Duration]) {

  override def serialize(value: Duration, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeNumber(value.getMillis)
  }
}