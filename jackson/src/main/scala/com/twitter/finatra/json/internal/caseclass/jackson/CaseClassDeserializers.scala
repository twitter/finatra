package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, JavaType}
import com.twitter.finatra.json.internal.caseclass.validation.{
  DefaultValidationProvider,
  ValidationProvider
}

private object CaseClassDeserializers {
  val PRODUCT: Class[Product] = classOf[Product]
  val OPTION: Class[Option[_]] = classOf[Option[_]]
  val LIST: Class[List[_]] = classOf[List[_]]
}

private[finatra] class CaseClassDeserializers(
  validationProvider: ValidationProvider = DefaultValidationProvider
) extends Deserializers.Base {

  import CaseClassDeserializers._

  override def findBeanDeserializer(
    javaType: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): CaseClassDeserializer = {
    if (maybeIsCaseClass(javaType.getRawClass))
      new CaseClassDeserializer(javaType, config, beanDesc, validationProvider)
    else
      null
  }

  private def maybeIsCaseClass(cls: Class[_]): Boolean = {
    if (!PRODUCT.isAssignableFrom(cls)) false
    else if (OPTION.isAssignableFrom(cls)) false
    else if (LIST.isAssignableFrom(cls)) false
    else if (cls.getName.startsWith("scala.Tuple")) false
    else if (cls.getName.startsWith("scala.util.Either")) false
    else true
  }
}
