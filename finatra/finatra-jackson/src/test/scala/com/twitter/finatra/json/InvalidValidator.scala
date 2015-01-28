package com.twitter.finatra.json

import com.twitter.finatra.tests.json.internal.InvalidValidationInternal
import com.twitter.finatra.validation._

class InvalidValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: InvalidValidationInternal)
  extends Validator[Any, Any](
    validationMessageResolver,
    annotation) {

  override def isValid(value: Any): ValidationResult =
    throw new RuntimeException("validator foo error")
}