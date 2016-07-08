package com.twitter.finatra.json.internal.caseclass.wrapped

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.twitter.finatra.domain.WrappedValue

private[finatra] object WrappedValueSerializer extends StdSerializer[WrappedValue[_]](classOf[WrappedValue[_]]) {

  override def serialize(wrappedValue: WrappedValue[_], jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeObject(
      wrappedValue.onlyValue)
  }
}