package com.twitter.finatra.internal.marshalling

import com.fasterxml.jackson.databind.{BeanProperty, DeserializationContext, InjectableValues}
import com.google.inject.{Injector, Key}
import com.twitter.finagle.http.Request
import com.twitter.finatra.json.internal.caseclass.annotations._
import java.lang.annotation.Annotation

class RequestInjectableValues(
  request: Request,
  injector: Injector)
  extends InjectableValues {

  private val paramsAnnotation = Seq(
    classOf[RouteParamInternal],
    classOf[QueryParamInternal],
    classOf[FormParamInternal])

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
  override def findInjectableValue(valueId: Object, ctxt: DeserializationContext, forProperty: BeanProperty, beanInstance: Object) = {
    val fieldName = forProperty.getName

    if (hasAnnotation(forProperty, paramsAnnotation))
      if (forProperty.getType.isCollectionLikeType)
        request.params.getAll(fieldName) map {queryParamsResolveRawClass(forProperty, _)}
      else
        request.params.get(fieldName).orNull
    else if (hasAnnotation[HeaderInternal](forProperty))
      Option(request.headers().get(fieldName)).orNull
    else if (isRequest(forProperty))
      request
    else
      injector.getInstance(
        valueId.asInstanceOf[Key[_]]).asInstanceOf[Object]
  }

  /* Private */

  private def hasAnnotation[T <: Annotation : Manifest](beanProperty: BeanProperty): Boolean = {
    val annotClass = manifest[T].runtimeClass.asInstanceOf[Class[_ <: Annotation]]
    beanProperty.getContextAnnotation(annotClass) != null
  }

  private def hasAnnotation(beanProperty: BeanProperty, annotationClasses: Seq[Class[_ <: Annotation]]): Boolean = {
    annotationClasses exists { annotationClass =>
      beanProperty.getContextAnnotation(annotationClass) != null
    }
  }

  private def isRequest(forProperty: BeanProperty): Boolean = {
    forProperty.getType.getRawClass == classOf[Request]
  }

  private def queryParamsResolveRawClass(forProperty: BeanProperty, value: String) = {
    val clazz = forProperty.getType.getContentType.getRawClass
    if (clazz == classOf[java.lang.Long])
      value.toLong
    else if (clazz == classOf[java.lang.Integer])
      value.toInt
    else if (clazz == classOf[java.lang.Short])
      value.toShort
    else if (clazz == classOf[java.lang.Boolean])
      value.toBoolean
    else if (clazz == classOf[java.lang.String])
      value
    else
      null
  }
}
