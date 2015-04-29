package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.module.scala._

object FinatraCaseClassModule
  extends JacksonModule {
  override def getModuleName = "FinatraCaseClassModule"

  this += {_.addDeserializers(new FinatraCaseClassDeserializers())}
}
