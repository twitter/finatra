package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.NotEmptyValidator._
import com.twitter.finatra.validation.{ErrorCode, NotEmpty, ValidationMessageResolver, ValidationResult, Validator}

private[finatra] object NotEmptyValidator {

  def errorMessage(
    resolver: ValidationMessageResolver) = {

    resolver.resolve(classOf[NotEmpty])
  }
}

private[finatra] class NotEmptyValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: NotEmpty)
  extends Validator[NotEmpty, Any](
    validationMessageResolver,
    annotation) {

  /* Public */

  override def isValid(value: Any): ValidationResult = {
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue)
      case traversableValue: Traversable[_] =>
        validationResult(traversableValue)
      case stringValue: String =>
        validationResult(stringValue)
      case _ =>
        throw new IllegalArgumentException(s"Class [${value.getClass}}] is not supported")
    }
  }

  /* Private */

  private def validationResult(value: Traversable[_]) = {
    ValidationResult.validate(
      value.nonEmpty,
      errorMessage(validationMessageResolver),
      ErrorCode.ValueCannotBeEmpty)
  }

  private def validationResult(value: String) = {
    ValidationResult.validate(
      value.nonEmpty,
      errorMessage(validationMessageResolver),
      ErrorCode.ValueCannotBeEmpty)
  }
}
