package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.{ConstraintValidator, MessageResolver, ValidationResult}

class StateConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[StateConstraint, String](messageResolver) {

  override def isValid(annotation: StateConstraint, value: String): ValidationResult =
    ValidationResult.validate(
      value.equalsIgnoreCase("CA"),
      "Please register with state CA"
    )
}
