package com.twitter.finatra.json.internal.caseclass.guice

import com.fasterxml.jackson.databind.{BeanProperty, DeserializationContext, InjectableValues}
import com.google.inject.{Injector, Key}

private[json] class GuiceInjectableValues(injector: Injector) extends InjectableValues {
  override def findInjectableValue(valueId: Object, ctxt: DeserializationContext, forProperty: BeanProperty, beanInstance: Object): Object = {
    val key = valueId.asInstanceOf[Key[_]]
    injector.getInstance(key)
      .asInstanceOf[Object]
  }
}