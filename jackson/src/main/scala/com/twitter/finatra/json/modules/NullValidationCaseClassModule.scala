package com.twitter.finatra.json.modules

import com.fasterxml.jackson.module.scala._
import com.twitter.finatra.json.internal.caseclass.jackson.{CaseClassDeserializers, NullCaseClassValidationProvider}

/**
 * Module that supports skipping validation of Finatra validation annotations.
 */
private[json] object NullValidationCaseClassModule extends JacksonModule {
  override def getModuleName = "NullValidationCaseClassModule"

  this += { _.addDeserializers(new CaseClassDeserializers(NullCaseClassValidationProvider)) }
}
