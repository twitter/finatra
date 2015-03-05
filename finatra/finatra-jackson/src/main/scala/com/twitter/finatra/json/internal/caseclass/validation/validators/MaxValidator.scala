package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.validation.{Max, ValidationMessageResolver, ValidationResult, Validator}

object MaxValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: Any,
    maxValue: Long): String = {

    resolver.resolve(classOf[Max], value, maxValue)
  }
}

class MaxValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: Max)
  extends Validator[Max, Any](
    validationMessageResolver,
    annotation) {

  private val maxValue = annotation.value()

  /* Public */

  override def isValid(value: Any): ValidationResult = {
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue)
      case traversableValue: Traversable[_] =>
        validationResult(traversableValue)
      case bigDecimalValue: BigDecimal =>
        validationResult(bigDecimalValue)
      case bigIntValue: BigInt =>
        validationResult(bigIntValue)
      case numberValue: Number =>
        validationResult(numberValue)
      case _ =>
        throw new IllegalArgumentException("Class [%s] is not supported" format value.getClass)
    }
  }

  /* Private */

  private def validationResult(value: Traversable[_]) = {
    ValidationResult(
      value.size <= maxValue,
      errorMessage(value.size))
  }

  private def validationResult(value: BigDecimal) = {
    ValidationResult(
      value <= BigDecimal(maxValue),
      errorMessage(value))
  }

  private def validationResult(value: BigInt) = {
    ValidationResult(
      value <= BigInt(maxValue),
      errorMessage(value))
  }

  private def validationResult(value: Number) = {
    ValidationResult(
      value.longValue() <= maxValue,
      errorMessage(value))
  }

  private def errorMessage(value: Number) = {
    MaxValidator.errorMessage(validationMessageResolver, value, maxValue)
  }
}
