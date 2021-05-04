package com.twitter.finatra.validation.tests

import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext}

class StateConstraintValidator extends ConstraintValidator[StateConstraint, String] {

  override def isValid(
    obj: String,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    obj.equalsIgnoreCase("CA")
  }
}
