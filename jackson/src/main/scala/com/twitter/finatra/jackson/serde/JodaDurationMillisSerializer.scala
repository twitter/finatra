package com.twitter.finatra.jackson.serde

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.joda.time.Duration

@deprecated(
  "Users should prefer JDK8 equivalent classes and associated Jackson support",
  "2021-04-12")
private[finatra] object JodaDurationMillisSerializer
    extends StdSerializer[Duration](classOf[Duration]) {

  override def serialize(
    value: Duration,
    jgen: JsonGenerator,
    provider: SerializerProvider
  ): Unit = {
    jgen.writeNumber(value.getMillis)
  }
}
