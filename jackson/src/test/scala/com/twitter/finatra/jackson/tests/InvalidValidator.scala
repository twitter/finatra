package com.twitter.finatra.jackson.tests

import com.twitter.finatra.validation.{ValidationMessageResolver, ValidationResult, Validator}
import scala.util.control.NoStackTrace

class InvalidValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: InvalidValidationInternal
) extends Validator[Any, Any](validationMessageResolver, annotation) {

  override def isValid(value: Any): ValidationResult =
    throw new RuntimeException("validator foo error") with NoStackTrace
}
