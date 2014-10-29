package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.ValidationMessageResolver
import com.twitter.finatra.json.internal.caseclass.validation.validators.UUIDValidator._
import com.twitter.finatra.json.{ValidationResult, annotations}
import com.twitter.util.Try
import java.util.UUID._

object UUIDValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: String) = {

    resolver.resolve(classOf[UUID], value)
  }

  def isValid(value: String) = {
    Try(fromString(value)).isReturn
  }
}

class UUIDValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: annotations.UUID)
  extends Validator[annotations.UUID, String](
    validationMessageResolver,
    annotation) {

  override def isValid(value: String) = {
    ValidationResult(
      UUIDValidator.isValid(value),
      errorMessage(validationMessageResolver, value))
  }
}
