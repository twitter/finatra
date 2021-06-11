package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.databind.{BeanProperty, DeserializationContext, InjectableValues}
import com.google.inject.{ConfigurationException, Injector, Key}
import com.twitter.finatra.jackson.caseclass.exceptions.InjectableValuesException
import com.twitter.inject.Logging
import java.lang.annotation.Annotation

private[finatra] object GuiceInjectableValues {
  type HasAnnotationType = (BeanProperty, Seq[Class[_ <: Annotation]])

  /**
   * We support the same annotations as the jackson-module-guice `com.fasterxml.jackson.module.guice.DefaultAnnotationIntrospector`:
   * [[JacksonInject]], [[javax.inject.Inject]], and [[com.google.inject.Inject]].
   *
   * @see [[https://github.com/FasterXML/jackson-modules-base/blob/master/guice/src/main/java/com/fasterxml/jackson/module/guice/GuiceAnnotationIntrospector.java]]
   * */
  val InjectionAnnotations: Set[Class[_ <: Annotation]] = Set(
    classOf[JacksonInject],
    classOf[javax.inject.Inject],
    classOf[com.google.inject.Inject]
  )
}

/**
 * Based on the jackson-module-guice GuiceInjectableValues but with a few differences.
 * - ensure the injector is not null before attempting to inject a value
 * - ensure that the given valueId is a com.google.inject.Key type before attempting to inject a value
 *
 * @see [[https://github.com/FasterXML/jackson-module-guice/blob/master/src/main/java/com/fasterxml/jackson/module/guice/GuiceInjectableValues.java]]
 */
private[finatra] class GuiceInjectableValues(injector: Injector)
    extends InjectableValues
    with Logging {
  import GuiceInjectableValues._

  override def findInjectableValue(
    valueId: Any,
    ctxt: DeserializationContext,
    forProperty: BeanProperty,
    beanInstance: Any
  ): AnyRef = valueId match {
    case key: Key[_] if isInjectable(forProperty) =>
      try {
        this.injector.getInstance(key).asInstanceOf[Object]
      } catch {
        case ex: ConfigurationException =>
          debug(ex.getMessage, ex)
          throw InjectableValuesException(
            forProperty.getMember.getDeclaringClass,
            forProperty.getName,
            key,
            ex)
      }
    case _ =>
      null
  }

  // ensure the property is annotated with something that signals it should be injected
  private[this] def isInjectable(forProperty: BeanProperty): Boolean =
    this.injector != null && Annotations.hasAnnotation(forProperty, InjectionAnnotations.toSeq)
}
