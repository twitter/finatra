package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.module.scala._
import com.twitter.finatra.validation.ValidationProvider

class CaseClassJacksonModule(
  injectableTypes: InjectableTypes,
  validationProvider: ValidationProvider)
    extends JacksonModule {
  override def getModuleName: String = this.getClass.getName

  this += {
    _.addDeserializers(new CaseClassDeserializerResolver(injectableTypes, validationProvider))
  }
}
