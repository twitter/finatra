package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.FutureTimeValidator._
import com.twitter.finatra.validation.{FutureTime, ValidationMessageResolver, ValidationResult, Validator}
import org.joda.time.DateTime

object FutureTimeValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: DateTime) = {

    resolver.resolve(classOf[FutureTime], value)
  }
}

/**
 * Validates if a datetime is in the future.
 */
class FutureTimeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: FutureTime)
  extends Validator[FutureTime, DateTime](
    validationMessageResolver,
    annotation) {

  override def isValid(value: DateTime) = {
    ValidationResult(
      value.isAfterNow,
      errorMessage(validationMessageResolver, value))
  }
}

