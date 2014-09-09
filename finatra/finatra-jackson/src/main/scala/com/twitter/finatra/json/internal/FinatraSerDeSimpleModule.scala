package com.twitter.finatra.json.internal

import com.fasterxml.jackson.databind.module.SimpleModule
import com.twitter.finatra.json.internal.caseclass.wrapped.JsonWrappedValueSerializer
import org.joda.time.DateTime

object FinatraSerDeSimpleModule extends SimpleModule {
  addSerializer(JsonWrappedValueSerializer)
  addSerializer(DateTimeUtcSerializer)
  addSerializer(DurationMillisSerializer)
  addDeserializer(classOf[DateTime], DateTimeUtcDeserializer)
}