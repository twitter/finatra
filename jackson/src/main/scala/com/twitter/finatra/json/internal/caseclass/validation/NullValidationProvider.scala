package com.twitter.finatra.json.internal.caseclass.validation

/**
 * Provides a validation bypass for case class validation.
 */
private[json] object NullValidationProvider extends ValidationProvider {
  override def apply(): CaseClassValidator = NullCaseClassValidator
}
