package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer}
import com.twitter.finatra.json.internal.caseclass.exceptions.FinatraJsonMappingException
import com.twitter.util.Duration

private[finatra] object DurationStringDeserializer extends JsonDeserializer[Duration] {

  override def deserialize(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Duration =
    try {
      Duration.parse(p.getValueAsString)
    } catch {
      case e: NumberFormatException =>
        throw new FinatraJsonMappingException(e.getMessage)
    }
}
