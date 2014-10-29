package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.databind.module.SimpleModule
import com.twitter.finatra.json.internal.caseclass.wrapped.JsonWrappedValueSerializer
import org.joda.time.DateTime

object FinatraSerDeSimpleModule extends SimpleModule {
  addSerializer(JsonWrappedValueSerializer)
  addSerializer(DurationMillisSerializer)
  addDeserializer(classOf[DateTime], FinatraDatetimeDeserializer)
}