package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{
  ConstraintDefinitionException,
  ConstraintValidator,
  ConstraintValidatorContext,
  UnexpectedTypeException
}

/**
 * The validator for [[Range]] annotation.
 *
 * Validate if the value of the field is within the min and max value defined in the annotation.
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class RangeConstraintValidator extends ConstraintValidator[Range, Any] {

  @volatile private[this] var minValue: Long = _
  @volatile private[this] var maxValue: Long = _

  override def initialize(constraintAnnotation: Range): Unit = {
    this.minValue = constraintAnnotation.min()
    this.maxValue = constraintAnnotation.max()
  }

  override def isValid(
    obj: Any,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    assertValidRange(minValue, maxValue)
    val valid = obj match {
      case bigDecimalValue: BigDecimal =>
        validationResult(bigDecimalValue, minValue, maxValue)
      case bigIntValue: BigInt =>
        validationResult(bigIntValue, minValue, maxValue)
      case numberValue: Number =>
        validationResult(numberValue, minValue, maxValue)
      case _ =>
        throw new UnexpectedTypeException(
          s"Class [${obj.getClass.getName}] is not supported by ${this.getClass.getName}")
    }

    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(
          ErrorCode
            .ValueOutOfRange(java.lang.Double.valueOf(obj.toString), minValue, maxValue))
        .withMessageTemplate(s"[${obj.toString}] is not between $minValue and $maxValue")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }

  /* Private */

  private[this] def validationResult(value: BigDecimal, minValue: Long, maxValue: Long): Boolean =
    BigDecimal(minValue) <= value && value <= BigDecimal(maxValue)

  private[this] def validationResult(value: BigInt, minValue: Long, maxValue: Long): Boolean =
    BigInt(minValue) <= value && value <= BigInt(maxValue)

  private[this] def validationResult(value: Number, minValue: Long, maxValue: Long): Boolean = {
    val doubleValue = value.doubleValue()
    minValue <= doubleValue && doubleValue <= maxValue
  }

  /** Asserts that the start is less than the end */
  private[this] def assertValidRange(startIndex: Long, endIndex: Long): Unit =
    if (startIndex > endIndex)
      throw new ConstraintDefinitionException(s"invalid range: $startIndex > $endIndex")
}
