package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext, UnexpectedTypeException}

/**
 * The validator for [[Max]] annotation.
 *
 * Validate if a given value is less than or equal to the value defined in [[Max]] annotation.=
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class MaxConstraintValidator extends ConstraintValidator[Max, Any] {

  @volatile private[this] var maxValue: Long = _

  override def initialize(constraintAnnotation: Max): Unit = {
    this.maxValue = constraintAnnotation.value()
  }

  override def isValid(
    obj: Any,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = obj match {
    case arrayValue: Array[_] =>
      isValid(arrayValue.length, constraintValidatorContext)
    case mapValue: Map[_, _] =>
      isValid(mapValue.size, constraintValidatorContext)
    case traversableValue: Iterable[_] =>
      isValid(traversableValue.size, constraintValidatorContext)
    case iterableWrapper: java.util.Collection[_] =>
      isValid(iterableWrapper.size(), constraintValidatorContext)
    case bigDecimalValue: BigDecimal =>
      isValid(bigDecimalValue, constraintValidatorContext)
    case bigIntValue: BigInt =>
      isValid(bigIntValue, constraintValidatorContext)
    case numberValue: Number =>
      isValid(numberValue, constraintValidatorContext)
    case _ =>
      throw new UnexpectedTypeException(
        s"Class [${obj.getClass.getName}] is not supported by ${this.getClass.getName}")
  }

  /* Private */

  private[this] def isValid(
    value: BigDecimal,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean =
    handleInvalid(
      value <= BigDecimal(this.maxValue),
      value.toString,
      value.longValue,
      constraintValidatorContext)

  private[this] def isValid(
    value: BigInt,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean =
    handleInvalid(
      value <= BigInt(this.maxValue),
      value.toString,
      value.longValue,
      constraintValidatorContext)

  private[this] def isValid(
    value: Number,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean =
    handleInvalid(
      value.doubleValue <= this.maxValue,
      value.toString,
      value.longValue,
      constraintValidatorContext)

  private[this] def isValid(
    value: Int,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean =
    handleInvalid(value <= this.maxValue, value.toString, value.toLong, constraintValidatorContext)

  private[this] def handleInvalid(
    valid: => Boolean,
    value: String,
    valueAsLong: Long,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.ValueTooLarge(maxValue, valueAsLong))
        .withMessageTemplate(s"[$value] is not less than or equal to $maxValue")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }
}
