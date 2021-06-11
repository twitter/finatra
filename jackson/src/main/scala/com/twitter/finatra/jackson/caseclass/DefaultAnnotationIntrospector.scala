package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.annotation.{JacksonAnnotation, JacksonInject}
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.google.inject.{BindingAnnotation, Key}
import com.twitter.util.jackson.annotation.InjectableValue
import com.twitter.util.jackson.caseclass.Types
import com.twitter.util.reflect.{Annotations => ReflectAnnotations}
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import scala.jdk.CollectionConverters._

/** Default supported annotations are: Guice Inject + Jackson Inject + Annotations annotated with @InjectableValue */
private[jackson] class DefaultAnnotationIntrospector
    extends com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector {

  override def findInjectableValue(m: AnnotatedMember): JacksonInject.Value = {
    // TODO: optimize
    val annotatedMemberAnnotationsMap = m.getAllAnnotations
    if (annotatedMemberAnnotationsMap != null) {
      val annotatedMemberAnnotations = annotatedMemberAnnotationsMap.annotations()
      val annotations = annotatedMemberAnnotations.asScala.toArray
      if (isInjectable(m.getName, annotations)) {
        val key: Key[_] = {
          val bindingAnnotations =
            ReflectAnnotations.filterIfAnnotationPresent[BindingAnnotation](annotations)
          if (bindingAnnotations.length > 1) {
            throw new IllegalArgumentException("Too many binding annotations on " + m.getName)
          } else if (bindingAnnotations.length == 1) {
            Key.get(typeOf(m.getType), bindingAnnotations.head)
          } else {
            Key.get(typeOf(m.getType))
          }
        }
        JacksonInject.Value.forId(key)
      } else null
    } else null
  }

  /**
   * This method does a couple of checks:
   * 1. ensures we only have a single injection annotation per field
   * 2. checks if the injection annotation is being used in combination with any [[com.fasterxml.jackson.annotation.JacksonAnnotation]]
   *    as the correct thing to do during deserialization is ambiguous in almost all cases.
   * 3. returns that the field is injectable if there is a single injection annotation on the field.
   *
   * @note the case could be made that it is OK to additionally specify [[com.fasterxml.jackson.annotation.JsonIgnore]] on a field
   *       (in addition to an injection annotation) b/c it can be argued from the [[com.fasterxml.jackson.annotation.JsonIgnore]]
   *       javadoc that is it legitimate to mark a field to be injected via Jackson InjectableValues
   *       as "ignored" since [[com.fasterxml.jackson.annotation.JsonIgnore]] is intended to signal that the "logical
   *       property is to be ignored by introspection-based serialization and deserialization
   *       functionality". However, the correct behavior if the [[com.fasterxml.jackson.annotation.JsonIgnore]] value is set to "false"
   *       is not clear in this case and thus we require including no other [[com.fasterxml.jackson.annotation.JacksonAnnotation]]s
   *       on the field.
   */
  def isInjectable(
    name: String,
    annotations: Array[Annotation]
  ): Boolean = {
    // dependency injection annotations -- cannot be used in combination with any JacksonAnnotation
    val injectionAnnotations =
      ReflectAnnotations.filterAnnotations(GuiceInjectableValues.InjectionAnnotations, annotations)
    // annotations that are annotated with `@InjectableValue` -- OK in combo with JacksonAnnotations,
    // though support for a given JacksonAnnotation may vary or be non-existent
    // depending on what `InjectableValues` implementation is configured
    val injectableValuesAnnotations =
      injectionAnnotations ++
        ReflectAnnotations.filterIfAnnotationPresent[InjectableValue](annotations)

    assert(
      injectableValuesAnnotations.length <= 1,
      "Only 1 injectable annotation allowed per field. " +
        "We found [" + injectableValuesAnnotations
        .map(_.annotationType.getName).mkString(", ") + s"] on field $name."
    )

    if (injectionAnnotations.nonEmpty) {
      // has one of `javax.inject.Inject`, `com.google.inject.Inject` or `com.fasterxml.jackson.annotation.JacksonInject`
      // should not also have any JacksonAnnotation
      val jacksonAnnotations =
        annotations.filter(a =>
          ReflectAnnotations.isAnnotationPresent[JacksonAnnotation](a) &&
            !ReflectAnnotations.equals[JacksonInject](a))
      assert(
        jacksonAnnotations.isEmpty,
        "Using JacksonAnnotations in combination with an annotation to " +
          "inject a field via dependency injection is not supported. " +
          "We found [" + (jacksonAnnotations ++ injectionAnnotations)
          .map(_.annotationType.getName).mkString(", ") + s"] on field $name."
      )
    }

    // is injectable if annotated with a supported injectable values annotation
    injectableValuesAnnotations.nonEmpty
  }

  /**
   * Translates a [[com.fasterxml.jackson.databind.JavaType]] into a fully described
   * [[java.lang.reflect.Type]]. Inspired by `net.codingwell.scalaguice.typeOf`.
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
  private def typeOf(javaType: JavaType): Type = {
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
