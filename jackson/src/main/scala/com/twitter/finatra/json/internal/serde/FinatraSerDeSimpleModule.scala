package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.databind.module.SimpleModule
import com.twitter.finatra.json.internal.caseclass.wrapped.WrappedValueSerializer
import org.joda.time.DateTime

private[finatra] object FinatraSerDeSimpleModule extends SimpleModule {
  addSerializer(WrappedValueSerializer)
  addSerializer(DurationMillisSerializer)
  addDeserializer(classOf[DateTime], FinatraDatetimeDeserializer)
}