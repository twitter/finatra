package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}
import org.joda.time.DateTime

private[validation] object PastTimeConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: DateTime): String =
    resolver.resolve(classOf[PastTime], value)
}

/**
 * The validator for [[PastTime]] annotation.
 *
 * Validate if a if a datetime is in the past.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class PastTimeConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[PastTime, DateTime](messageResolver) {

  override def isValid(annotation: PastTime, value: DateTime): ValidationResult =
    ValidationResult.validate(
      value.isBeforeNow,
      PastTimeConstraintValidator.errorMessage(messageResolver, value),
      ErrorCode.TimeNotPast(value)
    )
}
