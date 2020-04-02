package com.twitter.finatra.validation.constraints

import org.joda.time.DateTime
import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object FutureTimeConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: DateTime): String =
    resolver.resolve(classOf[FutureTime], value)
}

/**
 * The validator for [[FutureTime]] annotation.
 *
 * Validate if a if a datetime is in the future.
 * @note So far there is no test control in place for [[DateTime]].
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class FutureTimeConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[FutureTime, DateTime](messageResolver) {

  override def isValid(annotation: FutureTime, value: DateTime): ValidationResult =
    ValidationResult.validate(
      value.isAfterNow,
      FutureTimeConstraintValidator.errorMessage(messageResolver, value),
      ErrorCode.TimeNotFuture(value)
    )
}
