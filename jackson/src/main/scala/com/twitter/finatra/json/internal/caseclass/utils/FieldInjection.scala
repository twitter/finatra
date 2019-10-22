package com.twitter.finatra.json.internal.caseclass.utils

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.deser.impl.ValueInjector
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.{DeserializationContext, JavaType, PropertyName}
import com.google.inject.{BindingAnnotation, ConfigurationException, Key}
import com.twitter.finagle.http.Request
import com.twitter.finatra.json.internal.caseclass.exceptions.{JsonInjectException, JsonInjectionNotSupportedException}
import com.twitter.finatra.json.internal.caseclass.jackson.ImmutableAnnotations
import com.twitter.inject.utils.AnnotationUtils._
import com.twitter.finatra.json.internal.caseclass.utils.FieldInjection.InjectableAnnotations
import com.twitter.finatra.request.{FormParam, Header, QueryParam, RouteParam}
import com.twitter.inject.Logging
import java.lang.annotation.Annotation
import javax.inject.Inject

private[json] object FieldInjection {
  private val InjectableAnnotations: Set[Class[_ <: Annotation]] = Set(
    classOf[Inject],
    classOf[com.google.inject.Inject],
    classOf[RouteParam],
    classOf[QueryParam],
    classOf[FormParam],
    classOf[Header]
  )
}

private[json] class FieldInjection(
  name: String,
  javaType: JavaType,
  parentClass: Class[_],
  annotations: Seq[Annotation]
) extends Logging {

  private lazy val guiceKey = {
    val bindingAnnotations = filterIfAnnotationPresent[BindingAnnotation](annotations)

    if (bindingAnnotations.size > 1)
      throw new Exception("Too many binding annotations on " + name)
    else if (bindingAnnotations.size == 1)
      Key.get(JacksonToGuiceTypeConverter.typeOf(javaType), bindingAnnotations.head)
    else {
      Key.get(JacksonToGuiceTypeConverter.typeOf(javaType))
    }
  }

  private lazy val beanProperty: ValueInjector = {
    new ValueInjector(
      new PropertyName(name),
      javaType,
      /* mutator = */ null,
      /* valueId = */ null
    ) {
      // ValueInjector no longer supports passing contextAnnotations as an
      // argument as of jackson 2.9.x
      // https://github.com/FasterXML/jackson-databind/commit/76381c528c9b75265e8b93bf6bb4532e4aa8e957#diff-dbcd29e987f27d95f964a674963a9066R24
      private[this] val contextAnnotations = ImmutableAnnotations(annotations)
      override def getContextAnnotation[A <: Annotation](acls: Class[A]): A = {
        contextAnnotations.get[A](acls)
      }
    }
  }

  /* Public */

  def inject(context: DeserializationContext, codec: ObjectCodec): Option[Object] = {
    try {
      Option(context.findInjectableValue(guiceKey, beanProperty, /* beanInstance = */ null))
    } catch {
      case _: InvalidDefinitionException =>
        throw JsonInjectionNotSupportedException(parentClass, name)
      case e: ConfigurationException =>
        throw JsonInjectException(parentClass, name, guiceKey, e)
    }
  }

  val isInjectable: Boolean = {
    val injectableAnnotations = filterAnnotations(InjectableAnnotations, annotations)
    assert(
      injectableAnnotations.size <= 1,
      "Only 1 injectable annotation allowed per field. " +
        "We found " + (injectableAnnotations map { _.annotationType }) + " on field " + name
    )

    injectableAnnotations.nonEmpty || beanProperty.getType.getRawClass == classOf[Request]
  }
}
