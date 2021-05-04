package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.Try
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext}
import java.util.{UUID => JUUID}

private object UUIDConstraintValidator {
  def isValid(value: String): Boolean = Try(JUUID.fromString(value)).isReturn
}

/**
 * The default validator for [[UUID]] annotation.
 *
 * Validate if the value of the field is a UUID.
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class UUIDConstraintValidator extends ConstraintValidator[UUID, String] {

  override def isValid(
    obj: String,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val valid = UUIDConstraintValidator.isValid(obj)

    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.InvalidUUID(obj))
        .withMessageTemplate(s"[$obj] is not a valid UUID")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }
}
