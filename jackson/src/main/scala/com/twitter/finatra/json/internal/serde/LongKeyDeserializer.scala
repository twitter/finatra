package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.KeyDeserializers
import com.fasterxml.jackson.module.scala.JacksonModule
import com.twitter.finatra.domain.WrappedValue
import com.twitter.finatra.json.internal.caseclass.reflection.CaseClassSigParser

private[finatra] class LongKeyDeserializer(clazz: Class[_]) extends KeyDeserializer {
  private val constructor = clazz.getConstructor(classOf[Long])

  override def deserializeKey(key: String, ctxt: DeserializationContext): Object = {
    val long = key.toLong.asInstanceOf[Object]
    constructor.newInstance(long).asInstanceOf[Object]
  }
}

private[finatra] object LongKeyDeserializers extends JacksonModule {
  override def getModuleName = "LongKeyDeserializers"

  private val keyDeserializers = new KeyDeserializers {
    override def findKeyDeserializer(`type`: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): KeyDeserializer = {
      val clazz = beanDesc.getBeanClass
      if (isJsonWrappedLong(clazz))
        new LongKeyDeserializer(clazz)
      else
        null
    }
  }

  private def isJsonWrappedLong(clazz: Class[_]): Boolean = {
    classOf[WrappedValue[_]].isAssignableFrom(clazz) &&
      isWrappedLong(clazz)
  }

  private def isWrappedLong(clazz: Class[_]): Boolean = {
    val constructorParams = CaseClassSigParser.parseConstructorParams(clazz)
    constructorParams.head.scalaType.primitiveAwareErasure == classOf[Long]
  }

  this += {_.addKeyDeserializers(keyDeserializers)}
}