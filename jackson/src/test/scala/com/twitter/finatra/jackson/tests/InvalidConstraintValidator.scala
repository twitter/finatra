package com.twitter.finatra.jackson.tests

import com.twitter.finatra.validation.{ConstraintValidator, MessageResolver, ValidationResult}
import scala.util.control.NoStackTrace

class InvalidConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[InvalidConstraint, Any](messageResolver) {

  override def isValid(annotation: InvalidConstraint, value: Any): ValidationResult =
    throw new RuntimeException("validator foo error") with NoStackTrace
}
