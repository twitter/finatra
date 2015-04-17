package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.UUIDValidator._
import com.twitter.finatra.validation.{UUID, ValidationMessageResolver, ValidationResult, Validator}
import com.twitter.util.Try
import java.util.{UUID => JUUID}

object UUIDValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: String) = {

    resolver.resolve(classOf[UUID], value)
  }

  def isValid(value: String) = {
    Try(JUUID.fromString(value)).isReturn
  }
}

class UUIDValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: UUID)
  extends Validator[UUID, String](
    validationMessageResolver,
    annotation) {

  override def isValid(value: String) = {
    ValidationResult(
      UUIDValidator.isValid(value),
      errorMessage(validationMessageResolver, value))
  }
}
