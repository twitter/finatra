package com.twitter.inject.app.internal

import java.lang.reflect.Type

private object PrimitiveType {

  def asFull(t: Type): Type = t match {
    case java.lang.Integer.TYPE => classOf[java.lang.Integer]
    case java.lang.Short.TYPE => classOf[java.lang.Short]
    case java.lang.Long.TYPE => classOf[java.lang.Long]
    case java.lang.Float.TYPE => classOf[java.lang.Float]
    case java.lang.Double.TYPE => classOf[java.lang.Double]
    case java.lang.Byte.TYPE => classOf[java.lang.Byte]
    case java.lang.Boolean.TYPE => classOf[java.lang.Boolean]
    case other => other
  }

  def asPartial(t: Type): Type = t match {
    case java.lang.Integer.TYPE => classOf[java.lang.Object]
    case java.lang.Short.TYPE => classOf[java.lang.Object]
    case java.lang.Long.TYPE => classOf[java.lang.Object]
    case java.lang.Float.TYPE => classOf[java.lang.Object]
    case java.lang.Double.TYPE => classOf[java.lang.Object]
    case java.lang.Byte.TYPE => classOf[java.lang.Object]
    case java.lang.Boolean.TYPE => classOf[java.lang.Object]
    case other => other
  }
}
