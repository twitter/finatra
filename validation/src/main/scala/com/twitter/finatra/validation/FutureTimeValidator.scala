package com.twitter.finatra.validation

import com.twitter.finatra.validation.FutureTimeValidator._
import org.joda.time.DateTime

private[validation] object FutureTimeValidator {

  def errorMessage(resolver: ValidationMessageResolver, value: DateTime) = {

    resolver.resolve(classOf[FutureTime], value)
  }
}

/**
 * Validates if a datetime is in the future.
 */
private[validation] class FutureTimeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: FutureTime
) extends Validator[FutureTime, DateTime](validationMessageResolver, annotation) {

  override def isValid(value: DateTime) = {
    ValidationResult.validate(
      value.isAfterNow,
      errorMessage(validationMessageResolver, value),
      ErrorCode.TimeNotFuture(value)
    )
  }
}
