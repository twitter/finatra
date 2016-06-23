package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, JavaType}

private[finatra] class FinatraCaseClassDeserializers extends Deserializers.Base {
  val PRODUCT = classOf[Product]
  val OPTION = classOf[Option[_]]
  val LIST = classOf[List[_]]

  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription) = {
    if (maybeIsCaseClass(javaType.getRawClass))
      new FinatraCaseClassDeserializer(javaType, config, beanDesc)
    else
      null
  }

  private def maybeIsCaseClass(cls: Class[_]): Boolean = {
    if (!PRODUCT.isAssignableFrom(cls)) false
    else if (OPTION.isAssignableFrom(cls)) false
    else if (LIST.isAssignableFrom(cls)) false
    else if (cls.getName.startsWith("scala.Tuple")) false
    else true
  }
}
