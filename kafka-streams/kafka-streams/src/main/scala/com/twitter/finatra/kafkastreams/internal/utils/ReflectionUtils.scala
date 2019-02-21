package com.twitter.finatra.kafkastreams.internal.utils

import java.lang.reflect.{Field, Modifier}

private[kafkastreams] object ReflectionUtils {

  def getField(clazz: Class[_], fieldName: String): Field = {
    val field = clazz.getDeclaredField(fieldName)
    field.setAccessible(true)
    field
  }

  def getField[T](anyRef: AnyRef, fieldName: String): T = {
    val field = getField(anyRef.getClass, fieldName)
    field.get(anyRef).asInstanceOf[T]
  }

  def getFinalField(clazz: Class[_], fieldName: String): Field = {
    val field = clazz.getDeclaredField(fieldName)
    field.setAccessible(true)
    removeFinal(field)
    field
  }

  def getFinalField[T](anyRef: AnyRef, fieldName: String): T = {
    val field = getFinalField(anyRef.getClass, fieldName)
    field.get(anyRef).asInstanceOf[T]
  }

  def removeFinal(field: Field): Unit = {
    val fieldModifiers = classOf[Field].getDeclaredField("modifiers")
    fieldModifiers.setAccessible(true)
    fieldModifiers.setInt(field, field.getModifiers & ~Modifier.FINAL)
  }

}
