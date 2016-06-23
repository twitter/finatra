package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.FutureTimeValidator._
import com.twitter.finatra.validation.{ErrorCode, FutureTime, ValidationMessageResolver, ValidationResult, Validator}
import org.joda.time.DateTime

private[finatra] object FutureTimeValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: DateTime) = {

    resolver.resolve(classOf[FutureTime], value)
  }
}

/**
 * Validates if a datetime is in the future.
 */
private[finatra] class FutureTimeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: FutureTime)
  extends Validator[FutureTime, DateTime](
    validationMessageResolver,
    annotation) {

  override def isValid(value: DateTime) = {
    ValidationResult.validate(
      value.isAfterNow,
      errorMessage(validationMessageResolver, value),
      ErrorCode.TimeNotFuture(value))
  }
}
