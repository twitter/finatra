package com.twitter.finatra.json.internal.caseclass.jackson

import com.twitter.finatra.validation.{CaseClassValidationProvider, CaseClassValidator}

/**
 * Provides a validation bypass for case class validation.
 */
private[json] object NullCaseClassValidationProvider extends CaseClassValidationProvider {
  override def apply(): CaseClassValidator = NullCaseClassValidator
}
