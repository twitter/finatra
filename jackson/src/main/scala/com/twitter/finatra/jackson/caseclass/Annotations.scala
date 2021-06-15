package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.databind.BeanProperty
import java.lang.annotation.Annotation

private[finatra] object Annotations {

  def hasAnnotation(beanProperty: BeanProperty, annotations: Seq[Class[_ <: Annotation]]): Boolean =
    annotations.exists(beanProperty.getContextAnnotation(_) != null)

}
