package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext}
import org.joda.time.DateTime

/**
 * The validator for [[PastTime]] annotation.
 *
 * Validate if a datetime is in the past.
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class PastTimeConstraintValidator
    extends ConstraintValidator[PastTime, DateTime] {

  override def isValid(
    obj: DateTime,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val valid = obj.isBeforeNow

    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.TimeNotPast(obj))
        .withMessageTemplate(s"[${obj.toString}] is not in the past")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }
}
