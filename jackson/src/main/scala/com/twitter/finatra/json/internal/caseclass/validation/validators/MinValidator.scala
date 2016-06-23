package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.validation.{ErrorCode, Min, ValidationMessageResolver, ValidationResult, Validator}

private[finatra] object MinValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: Any,
    minValue: Long): String = {

    resolver.resolve(classOf[Min], value, minValue)
  }
}

private[finatra] class MinValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: Min)
  extends Validator[Min, Any](
    validationMessageResolver,
    annotation) {

  private val minValue = annotation.value()

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
      minValue <= value.size,
      errorMessage(Integer.valueOf(value.size)),
      errorCode(Integer.valueOf(value.size)))
  }

  private def validationResult(value: BigDecimal) = {
    ValidationResult.validate(
      BigDecimal(minValue) <= value,
      errorMessage(value),
      errorCode(value))
  }

  private def validationResult(value: BigInt) = {
    ValidationResult.validate(
      BigInt(minValue) <= value,
      errorMessage(value),
      errorCode(value))
  }

  private def validationResult(value: Number) = {
    ValidationResult.validate(
      minValue <= value.doubleValue(),
      errorMessage(value),
      errorCode(value))
  }

  private def errorMessage(value: Number) = {
    MinValidator.errorMessage(validationMessageResolver, value, minValue)
  }

  private def errorCode(value: Number) = {
    ErrorCode.ValueTooSmall(minValue, value)
  }
}
