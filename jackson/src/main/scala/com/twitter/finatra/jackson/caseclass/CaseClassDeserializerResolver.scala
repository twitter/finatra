package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, JavaType}
import com.twitter.finatra.validation.Validator
import com.twitter.util.reflect.{Types => ReflectionTypes}

private[finatra] class CaseClassDeserializerResolver(
  injectableTypes: InjectableTypes,
  validator: Option[Validator])
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
