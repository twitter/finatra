package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.PastTimeValidator._
import com.twitter.finatra.validation.{ErrorCode, PastTime, ValidationMessageResolver, ValidationResult, Validator}
import org.joda.time.DateTime

private[finatra] object PastTimeValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: DateTime) = {

    resolver.resolve(classOf[PastTime], value)
  }
}

/**
 * Validates if a datetime is in the past.
 */
private[finatra] class PastTimeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: PastTime)
  extends Validator[PastTime, DateTime](
    validationMessageResolver,
    annotation) {

  override def isValid(value: DateTime) = {
    ValidationResult.validate(
      value.isBeforeNow,
      errorMessage(validationMessageResolver, value),
      ErrorCode.TimeNotPast(value))
  }
}
