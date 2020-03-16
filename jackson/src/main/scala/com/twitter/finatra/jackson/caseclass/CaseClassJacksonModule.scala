package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.module.scala._
import com.twitter.finatra.validation.Validator

private[jackson] class CaseClassJacksonModule(
  injectableTypes: InjectableTypes,
  validator: Option[Validator])
    extends JacksonModule {
  override def getModuleName: String = this.getClass.getName

  this += {
    _.addDeserializers(
      new CaseClassDeserializerResolver(injectableTypes, validator))
  }
}
