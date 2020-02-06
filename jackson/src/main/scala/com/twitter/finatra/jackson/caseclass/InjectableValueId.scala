package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.databind.JavaType
import com.google.inject.{BindingAnnotation, Key}
import com.twitter.inject.utils.AnnotationUtils
import java.lang.annotation.Annotation
import java.lang.reflect.Type

private object InjectableValueId {

  /**
   * Translates a [[com.fasterxml.jackson.databind.JavaType]] into a fully described
   * [[java.lang.reflect.Type]]. Inspired by [[net.codingwell.scalaguice.typeOf]].
   *
   * Attempts to parameterize the [[Key]] correctly improving on the `jackson-module-guice`
   * [[Key]] value from the `GuiceAnnotationInspector`:
   * [[https://github.com/FasterXML/jackson-modules-base/blob/65208a924aeeb7cd4c31238e5c36c0ce66348db0/guice/src/main/java/com/fasterxml/jackson/module/guice/GuiceAnnotationIntrospector.java#L80]]
   *
   * @see [[https://github.com/codingwell/scala-guice/blob/v3.0.2/src/main/scala/net/codingwell/package.scala#L26]]
   * @param javaType the [[com.fasterxml.jackson.databind.JavaType]] to translate
   *
   * @return the [[java.lang.reflect.Type]] that corresponds to the given
   *         [[com.fasterxml.jackson.databind.JavaType]]
   */
  def typeOf(javaType: JavaType): Type = {
    if (javaType.isArrayType) return javaType.getRawClass

    val typeArguments =
      for (i <- 0 until javaType.containedTypeCount) yield javaType.containedType(i)

    typeArguments match {
      case Seq() =>
        Types.wrapperType(javaType.getRawClass)
      case args =>
        javaType.getRawClass match {
          case c: Class[_] if c.getEnclosingClass == null =>
            com.google.inject.util.Types.newParameterizedType(c, args.map(typeOf): _*)
          case c: Class[_] =>
            com.google.inject.util.Types
              .newParameterizedTypeWithOwner(c.getEnclosingClass, c, args.map(typeOf): _*)
        }
    }
  }
}

/**
 * General Object Key for Jackson Field Injection. Creates a key for use by [[DefaultInjectableValues]].
 *
 * @see [[DefaultInjectableValues]]
 */
private[caseclass] case class InjectableValueId(
  name: String,
  javaType: JavaType,
  annotations: Array[Annotation]) {

  /** Key[_] to use for DefaultInjectableValues (Guice) injection */
  lazy val key: Key[_] = {
    val bindingAnnotations =
      AnnotationUtils.filterIfAnnotationPresent[BindingAnnotation](annotations)
    if (bindingAnnotations.length > 1) {
      throw new IllegalArgumentException("Too many binding annotations on " + name)
    } else if (bindingAnnotations.length == 1) {
      Key.get(InjectableValueId.typeOf(javaType), bindingAnnotations.head)
    } else {
      Key.get(InjectableValueId.typeOf(javaType))
    }
  }
}
