package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.databind.BeanProperty
import com.twitter.util.Memoize
import java.lang.annotation.Annotation

private[finatra] object Annotations {
  private case class HasAnnotation(
    beanProperty: BeanProperty,
    annotations: Seq[Class[_ <: Annotation]])

  def hasAnnotation(beanProperty: BeanProperty, annotations: Seq[Class[_ <: Annotation]]): Boolean =
    hasAnnotation(HasAnnotation(beanProperty, annotations))

  private val hasAnnotation: HasAnnotation => Boolean = Memoize { hasAnnotation =>
    hasAnnotation.annotations.exists(hasAnnotation.beanProperty.getContextAnnotation(_) != null)
  }
}
