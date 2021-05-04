package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.module.scala._
import com.twitter.util.validation.ScalaValidator

private[jackson] class CaseClassJacksonModule(
  injectableTypes: InjectableTypes,
  validator: Option[ScalaValidator])
    extends JacksonModule {
  override def getModuleName: String = this.getClass.getName

  this += {
    _.addDeserializers(new CaseClassDeserializerResolver(injectableTypes, validator))
  }
}
