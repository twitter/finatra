package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext}

@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class AssertFalseConstraintValidator
    extends ConstraintValidator[AssertFalse, Boolean] {

  override def isValid(
    obj: Boolean,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val valid = !obj

    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.InvalidBooleanValue(obj))
        .withMessageTemplate("must be false")
        .addConstraintViolation(constraintValidatorContext)
    }

    valid
  }
}
