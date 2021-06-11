package com.twitter.finatra.jackson.serde

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.joda.cfg.FormatConfig
import org.joda.time.DateTime

@deprecated(
  "Users should prefer JDK8 equivalent classes and associated Jackson support",
  "2021-04-12")
private[jackson] object BaseSerdeModule extends SimpleModule {
  addSerializer(JodaDurationMillisSerializer)
  addDeserializer(
    classOf[DateTime],
    new JodaDatetimeDeserializer(FormatConfig.DEFAULT_DATETIME_PARSER))
}
