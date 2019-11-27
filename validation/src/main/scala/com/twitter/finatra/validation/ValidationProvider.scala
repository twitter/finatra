package com.twitter.finatra.validation

import java.lang.annotation.Annotation

/**
 * Trait that defines a factory for returning a CaseClassValidator.
 */
private[finatra] trait ValidationProvider {

  /**
   * Return a CaseClassValidator instance that will be used to provide validation against
   * validation annotations
   *
   * @return a CaseClassValidator instance
   */
  def apply(): CaseClassValidator

  /**
   * @return the Class type of case class validation annotations.
   */
  def validationAnnotation: Class[_ <: Annotation]


}
