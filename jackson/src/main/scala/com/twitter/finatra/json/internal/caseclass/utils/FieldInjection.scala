package com.twitter.finatra.json.internal.caseclass.utils

import com.fasterxml.jackson.core.{JsonParser, ObjectCodec}
import com.fasterxml.jackson.databind.deser.impl.ValueInjector
import com.fasterxml.jackson.databind.util.TokenBuffer
import com.fasterxml.jackson.databind.{DeserializationContext, JavaType, PropertyName}
import com.google.inject.{BindingAnnotation, ConfigurationException, Key}
import com.twitter.finatra.json.internal.caseclass.annotations._
import com.twitter.finatra.json.internal.caseclass.exceptions.{JsonInjectException, JsonInjectionNotSupportedException}
import com.twitter.finatra.json.internal.caseclass.jackson.ImmutableAnnotations
import com.twitter.finatra.json.internal.caseclass.utils.AnnotationUtils._
import com.twitter.finatra.json.internal.caseclass.utils.FieldInjection.InjectableAnnotations
import java.lang.annotation.Annotation
import javax.inject.Inject
import scala.language.existentials

object FieldInjection {
  private val InjectableAnnotations: Set[Class[_ <: Annotation]] = Set(
    classOf[Inject],
    classOf[com.google.inject.Inject],
    classOf[RouteParamInternal],
    classOf[QueryParamInternal],
    classOf[FormParamInternal],
    classOf[HeaderInternal])
}

class FieldInjection(
  name: String,
  javaType: JavaType,
  parentClass: Class[_],
  annotations: Seq[Annotation]) {

  private lazy val guiceKey = {
    val bindingAnnotations = filterIfAnnotationPresent[BindingAnnotation](annotations)

    //TODO: Convert JavaType into TypeLiteral instead of using javaType.getRawClass
    if (bindingAnnotations.size > 1)
      throw new Exception("Too many binding annotations on " + name)
    else if (bindingAnnotations.size == 1)
      Key.get(javaType.getRawClass, bindingAnnotations.head)
    else
      Key.get(javaType.getRawClass)
  }

  private lazy val beanProperty: ValueInjector = {
    new ValueInjector(
      new PropertyName(name),
      javaType,
      ImmutableAnnotations(annotations),
      /* mutator = */ null,
      /* valueId = */ null)
  }

  /* Public */

  def inject(
    context: DeserializationContext,
    codec: ObjectCodec): Option[Object] = {

    try {
      Option(
        context.findInjectableValue(guiceKey, beanProperty, /* beanInstance = */ null))
    }
    catch {
      case e: IllegalStateException =>
        throw new JsonInjectionNotSupportedException(parentClass, name)
      case e: ConfigurationException =>
        throw new JsonInjectException(parentClass, name, guiceKey, e)
    }
  }

  val isInjectable: Boolean = {
    val injectableAnnotations = filterAnnotations(InjectableAnnotations, annotations)
    assert(
      injectableAnnotations.size <= 1,
      "Only 1 injectable annotation allowed per field. " +
        "We found " + (injectableAnnotations map {_.annotationType}) + " on field " + name)

    injectableAnnotations.nonEmpty
  }
}
