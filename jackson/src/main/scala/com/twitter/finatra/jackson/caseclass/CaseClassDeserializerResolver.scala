package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, JavaType}
import com.twitter.finatra.validation.ValidationProvider

private object CaseClassDeserializerResolver {
  val PRODUCT: Class[Product] = classOf[Product]
  val OPTION: Class[Option[_]] = classOf[Option[_]]
  val LIST: Class[List[_]] = classOf[List[_]]
}

private[finatra] class CaseClassDeserializerResolver(
  injectableTypes: InjectableTypes,
  validationProvider: ValidationProvider)
    extends Deserializers.Base {

  import CaseClassDeserializerResolver._

  override def findBeanDeserializer(
    javaType: JavaType,
    deserializationConfig: DeserializationConfig,
    beanDescription: BeanDescription
  ): CaseClassDeserializer = {
    if (maybeIsCaseClass(javaType.getRawClass))
      new CaseClassDeserializer(
        javaType,
        deserializationConfig,
        beanDescription,
        injectableTypes,
        validationProvider)
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
