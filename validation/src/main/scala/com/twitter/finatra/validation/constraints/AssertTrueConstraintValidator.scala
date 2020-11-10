package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object AssertTrueConstraintValidator {
  def errorMessage(resolver: MessageResolver, value: Boolean): String =
    resolver.resolve[AssertTrue](value)
}

private[validation] class AssertTrueConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[AssertTrue, Boolean](messageResolver) {

  override def isValid(
    annotation: AssertTrue,
    value: Boolean
  ): ValidationResult = ValidationResult.validate(
    value,
    AssertTrueConstraintValidator.errorMessage(messageResolver, value),
    ErrorCode.InvalidBooleanValue(value)
  )
}
