package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.twitter.util.Time

private[finatra] object TwitterUtilTimeStringSerializer extends StdSerializer[Time](classOf[Time]) {
  override def serialize(value: Time, jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    jgen.writeString(value.toString)
  }
}
