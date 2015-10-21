package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.SizeValidator._
import com.twitter.finatra.validation.{ErrorCode, Size, ValidationMessageResolver, ValidationResult, Validator}

object SizeValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: Any,
    minValue: Long,
    maxValue: Long): String = {

    resolver.resolve(
      classOf[Size],
      toErrorValue(value),
      minValue,
      maxValue)
  }

  private def toErrorValue(value: Any) = {
    value match {
      case arrayValue: Array[_] =>
        arrayValue.length
      case traversableValue: Traversable[_] =>
        traversableValue.size
      case str: String =>
        str.length
      case _ =>
        throw new IllegalArgumentException("Class [%s] is not supported" format value.getClass)
    }
  }
}

class SizeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: Size)
  extends Validator[Size, Any](
    validationMessageResolver,
    annotation) {

  private val minValue: Long = annotation.min()

  private val maxValue: Long = annotation.max()

  /* Public */

  override def isValid(value: Any): ValidationResult = {
    val size = value match {
      case arrayValue: Array[_] => arrayValue.length
      case traversableValue: Traversable[_] => traversableValue.size
      case str: String => str.length
      case _ =>
        throw new IllegalArgumentException("Class [%s] is not supported" format value.getClass)
    }

    ValidationResult(
      isValid(size.toLong),
      errorMessage(
        validationMessageResolver,
        value,
        minValue,
        maxValue),
      ErrorCode.SizeOutOfRange(size, minValue, maxValue))
  }

  /* Private */

  private def isValid(value: Long): Boolean = {
    minValue <= value && value <= maxValue
  }
}
