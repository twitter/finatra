package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object AssertFalseConstraintValidator {
  def errorMessage(resolver: MessageResolver, value: Boolean): String =
    resolver.resolve[AssertFalse](value)
}

private[validation] class AssertFalseConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[AssertFalse, Boolean](messageResolver) {

  override def isValid(
    annotation: AssertFalse,
    value: Boolean
  ): ValidationResult = ValidationResult.validate(
    !value,
    AssertFalseConstraintValidator.errorMessage(messageResolver, value),
    ErrorCode.InvalidBooleanValue(value)
  )
}
