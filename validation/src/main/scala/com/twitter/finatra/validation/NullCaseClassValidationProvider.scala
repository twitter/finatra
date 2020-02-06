package com.twitter.finatra.validation

/**
 * Provides a validation bypass for case class validation.
 */
object NullCaseClassValidationProvider extends CaseClassValidationProvider {
  override def apply(): CaseClassValidator = NullCaseClassValidator
}
