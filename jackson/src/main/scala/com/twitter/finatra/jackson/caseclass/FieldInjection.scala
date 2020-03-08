package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.{BeanProperty, DeserializationContext, JavaType}
import com.twitter.inject.Logging
import java.lang.annotation.Annotation

/**
 * A holder of the field information in order to invoke finding of an
 * injectable value.
 *
 * @see [[InjectableValueId]]
 */
private[caseclass] class FieldInjection(
  name: String,
  javaType: JavaType,
  parentClass: Class[_],
  annotations: Array[Annotation],
  injectableTypes: InjectableTypes)
    extends Logging {

  /** the valueId Object for field injection */
  val valueId: Object = InjectableValueId(name, javaType, annotations)

  // TODO: have a way to register this with the mapper/caseclassdeserializer
  /** is injectable if the field has injection annotations or is of an injectable type */
  val isInjectable: Boolean =
    DefaultInjectableValues.isInjectableField(
        name,
        injectableTypes,
        javaType,
        annotations)

  /* Public */

  def findInjectableValue(
    context: DeserializationContext,
    codec: ObjectCodec,
    forProperty: BeanProperty
  ): Option[Object] = {
    Option(
      context.findInjectableValue(
        /* valueId = */ valueId,
        /* forProperty = */ forProperty,
        /* beanInstance = */ null
      )
    )
  }
}
