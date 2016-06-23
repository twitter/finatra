package com.twitter.finatra.validation

class InvalidValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: InvalidValidationInternal)
  extends Validator[Any, Any](
    validationMessageResolver,
    annotation) {

  override def isValid(value: Any): ValidationResult =
    throw new RuntimeException("validator foo error")
}