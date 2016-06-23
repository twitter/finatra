package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.validation.{ErrorCode, Range, ValidationMessageResolver, ValidationResult, Validator}

private[finatra] object RangeValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: Any,
    minValue: Long,
    maxValue: Long): String = {

    resolver.resolve(classOf[Range], value, minValue, maxValue)
  }
}

private[finatra] class RangeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: Range)
  extends Validator[Range, Any](
    validationMessageResolver,
    annotation) {

  private val minValue = annotation.min()
  private val maxValue = annotation.max()

  /* Public */

  override def isValid(value: Any): ValidationResult = {
    value match {
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

  private def validationResult(value: BigDecimal) = {
    ValidationResult.validate(
      BigDecimal(minValue) <= value && value <= BigDecimal(maxValue),
      errorMessage(value),
      errorCode(value))
  }

  private def validationResult(value: BigInt) = {
    ValidationResult.validate(
      BigInt(minValue) <= value && value <= BigInt(maxValue),
      errorMessage(value),
      errorCode(value))
  }

  private def validationResult(value: Number) = {
    val doubleValue = value.doubleValue()
    ValidationResult.validate(
      minValue <= doubleValue && doubleValue <= maxValue,
      errorMessage(value),
      errorCode(value))
  }

  private def errorMessage(value: Number) = {
    RangeValidator.errorMessage(
      validationMessageResolver,
      value,
      minValue,
      maxValue)
  }

  private def errorCode(value: Number) = {
    ErrorCode.ValueOutOfRange(java.lang.Long.valueOf(value.longValue), minValue, maxValue)
  }
}
