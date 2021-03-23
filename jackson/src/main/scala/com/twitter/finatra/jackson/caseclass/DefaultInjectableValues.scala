package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.annotation.{JacksonAnnotation, JacksonInject}
import com.fasterxml.jackson.databind.{
  BeanProperty,
  DeserializationContext,
  InjectableValues,
  JavaType
}
import com.google.inject.{ConfigurationException, Injector}
import com.twitter.finatra.jackson.caseclass.exceptions.InjectableValuesException
import com.twitter.finatra.json.annotations.InjectableValue
import com.twitter.inject.Logging
import com.twitter.util.reflect.Annotations
import java.lang.annotation.Annotation
import javax.inject.Inject

private[finatra] object DefaultInjectableValues {

  /**
   * We support the same annotations as the jackson-module-guice `com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector`:
   * [[JacksonInject]], [[Inject]], and [[com.google.inject.Inject]].
   *
   * @see [[https://github.com/FasterXML/jackson-modules-base/blob/master/guice/src/main/java/com/fasterxml/jackson/module/guice/GuiceAnnotationIntrospector.java]]
   * */
  private val InjectionAnnotations: Set[Class[_ <: Annotation]] = Set(
    classOf[JacksonInject],
    classOf[Inject],
    classOf[com.google.inject.Inject]
  )

  /**
   * This method does a couple of checks:
   * 1. ensures we only have a single injection annotation per field
   * 2. checks if the injection annotation is being used in combination with any [[com.fasterxml.jackson.annotation.JacksonAnnotation]]
   *    as the correct thing to do during deserialization is ambiguous in almost all cases.
   * 3. returns that the field is injectable if there is a single injection annotation on the
   *    field or the field type is one of the given injectable types.
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
  def isInjectableField(
    name: String,
    injectableTypes: InjectableTypes,
    javaType: JavaType,
    annotations: Array[Annotation]
  ): Boolean = {
    // dependency injection annotations -- cannot be used in combination with any JacksonAnnotation
    val injectionAnnotations = Annotations.filterAnnotations(InjectionAnnotations, annotations)
    // annotations that are annotated with `@InjectableValue` -- OK in combo with JacksonAnnotations,
    // though support for a given JacksonAnnotation may vary or be non-existent
    // depending on what `InjectableValues` implementation is configured
    val injectableAnnotations = injectionAnnotations ++ Annotations
      .filterIfAnnotationPresent[InjectableValue](annotations)

    assert(
      injectableAnnotations.length <= 1,
      "Only 1 injectable annotation allowed per field. " +
        "We found [" + injectableAnnotations
        .map(_.annotationType.getName).mkString(", ") + s"] on field $name."
    )

    if (injectionAnnotations.nonEmpty) {
      // has one of `javax.inject.Inject`, `com.google.inject.Inject` or `com.fasterxml.jackson.annotation.JacksonInject`
      // should not also have any JacksonAnnotation
      val jacksonAnnotations =
        Annotations
          .filterIfAnnotationPresent[JacksonAnnotation](annotations)
      assert(
        jacksonAnnotations.isEmpty,
        "Using JacksonAnnotations in combination with an annotation to " +
          "inject a field via dependency injection is not supported. " +
          "We found [" + (jacksonAnnotations ++ injectionAnnotations)
          .map(_.annotationType.getName).mkString(", ") + s"] on field $name."
      )
    }

    // is injectable if annotated with a supported injectable annotation or is a supported injectable type
    injectableAnnotations.nonEmpty || injectableTypes.list.contains(javaType.getRawClass)
  }
}

/**
 * The only Jackson [[https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/InjectableValues.java InjectableValues]]
 * supported by default is for values that can be obtained from a non-null [[com.google.inject.Injector]].
 * Thus the field MUST be annotated with one of [[javax.inject.Inject]] or [[com.google.inject.Inject]].
 *
 * Expects valueIds to be of the form [[InjectableValueId]].
 *
 * @see [[InjectableValueId]]
 * @see [[https://github.com/FasterXML/jackson-modules-base/blob/master/guice/src/main/java/com/fasterxml/jackson/module/guice/GuiceInjectableValues.java]]
 */
private[finatra] class DefaultInjectableValues(injector: Injector)
    extends InjectableValues
    with Logging {

  override def findInjectableValue(
    valueId: Object,
    context: DeserializationContext,
    forProperty: BeanProperty,
    beanInstance: Object
  ): Object = {
    if (isInjectable(forProperty)) {
      val key = valueId.asInstanceOf[InjectableValueId].key
      try {
        injector
          .getInstance(key)
          .asInstanceOf[Object]
      } catch {
        case ex: ConfigurationException =>
          debug(ex.getMessage, ex)
          throw InjectableValuesException(
            forProperty.getMember.getDeclaringClass,
            forProperty.getName,
            key,
            ex)
      }
    } else null
  }

  private[this] def isInjectable(forProperty: BeanProperty): Boolean = {
    injector != null && hasAnnotation(
      forProperty,
      DefaultInjectableValues.InjectionAnnotations.toSeq)
  }

  private[this] lazy val hasAnnotation: (BeanProperty, Seq[Class[_ <: Annotation]]) => Boolean = {
    (beanProperty, annotations) =>
      annotations.exists(beanProperty.getContextAnnotation(_) != null)
  }
}
