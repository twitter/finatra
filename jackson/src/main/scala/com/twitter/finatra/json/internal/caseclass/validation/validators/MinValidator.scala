package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.validation.{ErrorCode, Min, ValidationMessageResolver, ValidationResult, Validator}

object MinValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: Any,
    minValue: Long): String = {

    resolver.resolve(classOf[Min], value, minValue)
  }
}

class MinValidator(
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
        throw new IllegalArgumentException("Class [%s] is not supported" format value.getClass)
    }
  }

  /* Private */

  private def validationResult(value: Traversable[_]) = {
    ValidationResult(
      minValue <= value.size,
      errorMessage(value.size),
      errorCode(value.size))
  }

  private def validationResult(value: BigDecimal) = {
    ValidationResult(
      BigDecimal(minValue) <= value,
      errorMessage(value),
      errorCode(value))
  }

  private def validationResult(value: BigInt) = {
    ValidationResult(
      BigInt(minValue) <= value,
      errorMessage(value),
      errorCode(value))
  }

  private def validationResult(value: Number) = {
    ValidationResult(
      minValue <= value.longValue(),
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
