package com.twitter.finatra.http.internal.marshalling

import com.fasterxml.jackson.databind.{BeanProperty, DeserializationContext, InjectableValues, JavaType}
import com.google.inject.{Injector, Key}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.internal.marshalling.RequestInjectableValues.SeqWithSingleEmptyString
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.request.{FormParam, Header, QueryParam, RouteParam}
import java.lang.annotation.Annotation

object RequestInjectableValues {
  val SeqWithSingleEmptyString = Seq("")
}

class RequestInjectableValues(
  objectMapper: FinatraObjectMapper,
  request: Request,
  injector: Injector
) extends InjectableValues {

  private val requestParamsAnnotations: Seq[Class[_ <: Annotation]] =
    Seq(classOf[RouteParam], classOf[QueryParam], classOf[FormParam])

  private val annotations: Seq[Class[_ <: Annotation]] =
    Seq(classOf[RouteParam], classOf[QueryParam], classOf[FormParam], classOf[Header])

  /* Public */

  /**
   * Lookup the key using the data in the Request object or objects in the object graph.
   *
   * @param valueId Key for looking up the value
   * @param ctxt DeserializationContext
   * @param forProperty BeanProperty
   * @param beanInstance Bean instance
   * @return the injected value
   */
  override def findInjectableValue(
    valueId: Object,
    ctxt: DeserializationContext,
    forProperty: BeanProperty,
    beanInstance: Object
  ): Object = {

    val fieldName = fieldNameForAnnotation(forProperty)

    if (isRequest(forProperty)) {
      request
    } else if (hasAnnotation(forProperty, requestParamsAnnotations)) {
      if (forProperty.getType.isCollectionLikeType) {
        request.params.getAll(fieldName) match {
          case propertyValue: Seq[String]
              if propertyValue.nonEmpty || request.params.contains(fieldName) =>
            val value = handleEmptySeq(forProperty, propertyValue)
            val modifiedParamsValue = handleExtendedBooleans(forProperty, value)
            convert(forProperty, modifiedParamsValue)
          case _ => null
        }
      } else {
        request.params.get(fieldName) match {
          case Some(value) =>
            val modifiedParamsValue = handleExtendedBooleans(forProperty, value)
            convert(forProperty, modifiedParamsValue)
          case None => null
        }
      }
    } else if (hasAnnotation[Header](forProperty)) {
      request.headerMap.get(fieldName) match {
        case Some(p) => convert(forProperty, p)
        case None => null
      }
    } else {
      injector.getInstance(valueId.asInstanceOf[Key[_]]).asInstanceOf[Object]
    }
  }

  /* Private */

  private def fieldNameForAnnotation(forProperty: BeanProperty): String = {
    val annotation = findAnnotation(forProperty, annotations)
    annotation match {
      case Some(routeParam: RouteParam) if routeParam.value.nonEmpty =>
        routeParam.value
      case Some(queryParam: QueryParam) if queryParam.value.nonEmpty =>
        queryParam.value
      case Some(formParam: FormParam) if formParam.value.nonEmpty =>
        formParam.value
      case Some(header: Header) if header.value.nonEmpty =>
        header.value
      case _ =>
        forProperty.getName
    }
  }

  private def convert(forProperty: BeanProperty, propertyValue: Any): AnyRef = {
    convert(forProperty.getType, propertyValue)
  }

  private def convert(forType: JavaType, propertyValue: Any): AnyRef = {
    if (forType.getRawClass == classOf[Option[_]])
      if (propertyValue == "")
        None
      else
        Option(convert(forType.containedType(0), propertyValue))
    else
      objectMapper.convert(propertyValue, forType)
  }

  private def handleEmptySeq(forProperty: BeanProperty, propertyValue: Any): Any = {
    if (propertyValue == SeqWithSingleEmptyString &&
      forProperty.getType.containedType(0).getRawClass != classOf[String]) {
      // if a query param is set with an empty value we will get an empty seq of
      // string, yet the property type may not be string
      Seq.empty
    } else {
      propertyValue
    }
  }

  private def handleExtendedBooleans(forProperty: BeanProperty, propertyValue: Any): Any = {
    if (hasAnnotation[QueryParam](forProperty)) {
      val forType = forProperty.getType
      if (forType.getRawClass == classOf[java.lang.Boolean]) {
        matchExtendedBooleans(propertyValue.asInstanceOf[String])
      } else if (isSeqOfBools(forType)) {
        propertyValue.asInstanceOf[Seq[String]].map(matchExtendedBooleans)
      } else {
        propertyValue
      }
    } else {
      propertyValue
    }
  }

  private def matchExtendedBooleans(value: String): String = value match {
    case "t" | "1" => "true"
    case "f" | "0" => "false"
    case _ => value
  }

  private def hasAnnotation[T <: Annotation : Manifest](beanProperty: BeanProperty): Boolean = {
    getAnnotation[T](beanProperty).isDefined
  }

  private def getAnnotation[T <: Annotation : Manifest](beanProperty: BeanProperty): Option[T] = {
    val clazz = manifest[T].runtimeClass.asInstanceOf[Class[_ <: Annotation]]
    Option(beanProperty.getContextAnnotation(clazz)).map(_.asInstanceOf[T])
  }

  private def findAnnotation(
    beanProperty: BeanProperty,
    annotations: Seq[Class[_ <: Annotation]]
  ): Option[Annotation] = {
    annotations
      .find(beanProperty.getContextAnnotation(_) != null)
      .map(beanProperty.getContextAnnotation(_))
  }

  private lazy val isSeqOfBools: (JavaType) => Boolean = { forType =>
    forType.getRawClass == classOf[Seq[_]] &&
    forType.containedType(0).getRawClass == classOf[java.lang.Boolean]
  }

  private lazy val hasAnnotation: (BeanProperty, Seq[Class[_ <: Annotation]]) => Boolean = {
    (beanProperty, annotations) =>
      annotations.exists(beanProperty.getContextAnnotation(_) != null)
  }

  private lazy val isRequest: (BeanProperty) => Boolean = { forProperty =>
    forProperty.getType.getRawClass == classOf[Request]
  }
}
