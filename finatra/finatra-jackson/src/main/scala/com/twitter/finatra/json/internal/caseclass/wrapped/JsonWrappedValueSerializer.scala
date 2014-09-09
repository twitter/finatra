package com.twitter.finatra.json.internal.caseclass.wrapped

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

object JsonWrappedValueSerializer extends StdSerializer[JsonWrappedValue[_]](classOf[JsonWrappedValue[_]]) {

  override def serialize(wrappedValue: JsonWrappedValue[_], jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeObject(
      wrappedValue.onlyValue)
  }
}