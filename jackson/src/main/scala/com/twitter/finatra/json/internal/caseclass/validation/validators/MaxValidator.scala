package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.validation.{ErrorCode, Max, ValidationMessageResolver, ValidationResult, Validator}

private[finatra] object MaxValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: Any,
    maxValue: Long): String = {

    resolver.resolve(classOf[Max], value, maxValue)
  }
}

private[finatra] class MaxValidator(
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
        throw new IllegalArgumentException(s"Class [${value.getClass}] is not supported")
    }
  }

  /* Private */

  private def validationResult(value: Traversable[_]) = {
    ValidationResult.validate(
      value.size <= maxValue,
      errorMessage(Integer.valueOf(value.size)),
      errorCode(Integer.valueOf(value.size)))
  }

  private def validationResult(value: BigDecimal) = {
    ValidationResult.validate(
      value <= BigDecimal(maxValue),
      errorMessage(value),
      errorCode(value))
  }

  private def validationResult(value: BigInt) = {
    ValidationResult.validate(
      value <= BigInt(maxValue),
      errorMessage(value),
      errorCode(value))
  }

  private def validationResult(value: Number) = {
    ValidationResult.validate(
      value.doubleValue() <= maxValue,
      errorMessage(value),
      errorCode(value))
  }

  private def errorMessage(value: Number) = {
    MaxValidator.errorMessage(validationMessageResolver, value, maxValue)
  }

  private def errorCode(value: Number) = {
    ErrorCode.ValueTooLarge(maxValue, value)
  }
}
