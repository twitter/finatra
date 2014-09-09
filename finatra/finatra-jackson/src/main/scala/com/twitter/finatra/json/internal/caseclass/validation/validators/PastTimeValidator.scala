package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.validators.PastTimeValidator._
import com.twitter.finatra.json.internal.caseclass.validation.{ValidationMessageResolver, ValidationResult}
import org.joda.time.DateTime

object PastTimeValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: DateTime) = {

    resolver.resolve(classOf[PastTime], value)
  }
}

/**
 * Validates if a datetime is in the past.
 */
class PastTimeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: PastTime)
  extends Validator[PastTime, DateTime](
    validationMessageResolver,
    annotation) {

  override def isValid(value: DateTime) = {
    ValidationResult(
      value.isBeforeNow,
      errorMessage(validationMessageResolver, value))
  }
}

