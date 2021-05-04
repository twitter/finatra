package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext}
import org.joda.time.DateTime

/**
 * The validator for [[FutureTime]] annotation.
 *
 * Validate if a datetime is in the future.
 * @note So far there is no test control in place for [[DateTime]].
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class FutureTimeConstraintValidator
    extends ConstraintValidator[FutureTime, DateTime] {

  override def isValid(
    obj: DateTime,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val valid = obj.isAfterNow
    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.TimeNotFuture(obj))
        .withMessageTemplate(s"[${obj.toString}] is not in the future")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }
}
