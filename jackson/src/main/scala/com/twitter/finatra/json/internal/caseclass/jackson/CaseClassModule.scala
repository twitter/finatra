package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.module.scala._

private[finatra] object CaseClassModule extends JacksonModule {
  override def getModuleName: String = getClass.getName

  this += { _.addDeserializers(new CaseClassDeserializers()) }
}
