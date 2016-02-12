package com.twitter.finatra.http.internal.marshalling

import com.fasterxml.jackson.databind.{BeanProperty, DeserializationContext, InjectableValues, JavaType}
import com.google.inject.{Injector, Key}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.internal.marshalling.RequestInjectableValues.SeqWithSingleEmptyString
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.request.{FormParam, QueryParam, RouteParam, Header}
import java.lang.annotation.Annotation

object RequestInjectableValues {
  val SeqWithSingleEmptyString = Seq("")
}

class RequestInjectableValues(
  objectMapper: FinatraObjectMapper,
  request: Request,
  injector: Injector)
  extends InjectableValues {

  private val requestParamsAnnotation = Seq(
    classOf[RouteParam],
    classOf[QueryParam],
    classOf[FormParam])

  /* Public */

  /**
   * Lookup the key using the data in the Request object or objects in the Guice object graph.
   *
   * @param valueId Guice Key for looking up the value
   * @param ctxt DeserializationContext
   * @param forProperty BeanProperty
   * @param beanInstance Bean instance
   * @return the injected value
   */
  override def findInjectableValue(
    valueId: Object,
    ctxt: DeserializationContext,
    forProperty: BeanProperty,
    beanInstance: Object): Object = {

    val fieldName = forProperty.getName

    if (isRequest(forProperty))
      request
    else if (hasAnnotation(forProperty, requestParamsAnnotation))
      if (forProperty.getType.isCollectionLikeType) {
        val paramsValue = request.params.getAll(fieldName)
        if (paramsValue == SeqWithSingleEmptyString && hasStringTypeParam(forProperty))
          convert(forProperty, Seq())
        else
          convert(forProperty, request.params.getAll(fieldName))
      }
      else
        (request.params.get(fieldName) map { convert(forProperty, _) }).orNull
    else if (hasAnnotation[Header](forProperty))
      (request.headerMap.get(fieldName) map { convert(forProperty, _) }).orNull
    else
      injector.getInstance(
        valueId.asInstanceOf[Key[_]]).asInstanceOf[Object]
  }

  /* Private */

  private def convert(forProperty: BeanProperty, propertyValue: Any): AnyRef = {
    convert(
      forProperty.getType,
      propertyValue)
  }

  private def convert(forType: JavaType, propertyValue: Any): AnyRef = {
    if (forType.getRawClass == classOf[Option[_]])
      if (propertyValue == "")
        None
      else
        Option(
          convert(forType.containedType(0), propertyValue))
    else
      objectMapper.convert(
        propertyValue,
        forType)
  }

  private def hasStringTypeParam(forProperty: BeanProperty): Boolean = {
    forProperty.getType.containedType(0).getRawClass != classOf[String]
  }

  private def hasAnnotation[T <: Annotation : Manifest](beanProperty: BeanProperty): Boolean = {
    val annotClass = manifest[T].runtimeClass.asInstanceOf[Class[_ <: Annotation]]
    beanProperty.getContextAnnotation(annotClass) != null
  }

  private def hasAnnotation(
    beanProperty: BeanProperty,
    annotationClasses: Seq[Class[_ <: Annotation]]): Boolean = {
    annotationClasses exists { annotationClass =>
      beanProperty.getContextAnnotation(annotationClass) != null
    }
  }

  private def isRequest(forProperty: BeanProperty): Boolean = {
    forProperty.getType.getRawClass == classOf[Request]
  }
}
