package com.twitter.finatra.validation

import com.twitter.finatra.validation.PastTimeValidator._
import org.joda.time.DateTime

private[validation] object PastTimeValidator {

  def errorMessage(resolver: ValidationMessageResolver, value: DateTime) = {

    resolver.resolve(classOf[PastTime], value)
  }
}

/**
 * Validates if a datetime is in the past.
 */
private[validation] class PastTimeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: PastTime
) extends Validator[PastTime, DateTime](validationMessageResolver, annotation) {

  override def isValid(value: DateTime) = {
    ValidationResult.validate(
      value.isBeforeNow,
      errorMessage(validationMessageResolver, value),
      ErrorCode.TimeNotPast(value)
    )
  }
}
