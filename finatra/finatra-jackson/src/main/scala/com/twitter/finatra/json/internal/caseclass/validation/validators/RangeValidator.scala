package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.annotations.Range
import com.twitter.finatra.json.internal.caseclass.validation.{ValidationMessageResolver, ValidationResult}

object RangeValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: Any,
    minValue: Long,
    maxValue: Long): String = {

    resolver.resolve(classOf[Range], value, minValue, maxValue)
  }
}

class RangeValidator(
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
        throw new IllegalArgumentException("Class [%s] is not supported" format value.getClass)
    }
  }

  /* Private */

  private def validationResult(value: BigDecimal) = {
    ValidationResult(
      BigDecimal(minValue) <= value && value <= BigDecimal(maxValue),
      errorMessage(value))
  }

  private def validationResult(value: BigInt) = {
    ValidationResult(
      BigInt(minValue) <= value && value <= BigInt(maxValue),
      errorMessage(value))
  }

  private def validationResult(value: Number) = {
    val longValue = value.longValue()
    ValidationResult(
      minValue <= longValue && longValue <= maxValue,
      errorMessage(value))
  }

  private def errorMessage(value: Number) = {
    RangeValidator.errorMessage(
      validationMessageResolver,
      value,
      minValue,
      maxValue)
  }
}
