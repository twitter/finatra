package com.twitter.finatra.json.internal.caseclass.validation

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

}
