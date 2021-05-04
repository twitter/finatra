package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, JavaType}
import com.twitter.util.reflect.{Types => ReflectionTypes}
import com.twitter.util.validation.ScalaValidator

private[finatra] class CaseClassDeserializerResolver(
  injectableTypes: InjectableTypes,
  validator: Option[ScalaValidator])
    extends Deserializers.Base {

  override def findBeanDeserializer(
    javaType: JavaType,
    deserializationConfig: DeserializationConfig,
    beanDescription: BeanDescription
  ): CaseClassDeserializer = {
    if (ReflectionTypes.isCaseClass(javaType.getRawClass))
      new CaseClassDeserializer(
        javaType,
        deserializationConfig,
        beanDescription,
        injectableTypes,
        validator)
    else
      null
  }
}
