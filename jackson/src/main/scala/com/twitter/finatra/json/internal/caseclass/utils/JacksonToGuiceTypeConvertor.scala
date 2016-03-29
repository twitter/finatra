package com.twitter.finatra.json.internal.caseclass.utils

import com.fasterxml.jackson.databind.JavaType
import com.google.inject.util.Types
import java.lang.reflect.Type

private[internal] object JacksonToGuiceTypeConvertor {

  // Inspired by net.codingwell.scalaguice.typeOf
  // See: https://github.com/codingwell/scala-guice/blob/v3.0.2/src/main/scala/net/codingwell/package.scala#L26
  def typeOf(javaType: JavaType): Type = {
    def toWrapper(c: Type) = c match {
      case java.lang.Byte.TYPE => classOf[java.lang.Byte]
      case java.lang.Short.TYPE => classOf[java.lang.Short]
      case java.lang.Character.TYPE => classOf[java.lang.Character]
      case java.lang.Integer.TYPE => classOf[java.lang.Integer]
      case java.lang.Long.TYPE => classOf[java.lang.Long]
      case java.lang.Float.TYPE => classOf[java.lang.Float]
      case java.lang.Double.TYPE => classOf[java.lang.Double]
      case java.lang.Boolean.TYPE => classOf[java.lang.Boolean]
      case java.lang.Void.TYPE => classOf[java.lang.Void]
      case cls => cls
    }

    if (javaType.isArrayType)
      return javaType.getRawClass

    val typeArguments = for {
      i <- 0 until javaType.containedTypeCount()
      containedType = javaType.containedType(i)
    } yield containedType

    typeArguments match {
      case Seq() => toWrapper(javaType.getRawClass)
      case args => javaType.getRawClass match {
        case c: Class[_] if c.getEnclosingClass == null => Types.newParameterizedType(c, args.map(typeOf(_)): _*)
        case c: Class[_] => Types.newParameterizedTypeWithOwner(c.getEnclosingClass, c, args.map(typeOf(_)): _*)
      }
    }
  }
}
