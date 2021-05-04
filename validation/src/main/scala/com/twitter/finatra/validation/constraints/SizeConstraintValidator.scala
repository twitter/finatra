package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext, UnexpectedTypeException}

/**
 * The validator for [[Size]] annotation.
 *
 * Validate if the size of the field is within the min and max value defined in the annotation.
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class SizeConstraintValidator extends ConstraintValidator[Size, Any] {

  @volatile private[this] var minValue: Long = _
  @volatile private[this] var maxValue: Long = _

  override def initialize(constraintAnnotation: Size): Unit = {
    this.minValue = constraintAnnotation.min()
    this.maxValue = constraintAnnotation.max()
  }

  override def isValid(
    obj: Any,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val size = obj match {
      case arrayValue: Array[_] => arrayValue.length
      case mapValue: Map[_, _] => mapValue.size
      case traversableValue: Iterable[_] => traversableValue.size
      case iterableWrapper: java.util.Collection[_] => iterableWrapper.size()
      case str: String => str.length
      case _ =>
        throw new UnexpectedTypeException(
          s"Class [${obj.getClass.getName}] is not supported by ${this.getClass.getName}")
    }

    val valid = isValid(size.toLong, minValue, maxValue)
    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(
          ErrorCode.SizeOutOfRange(java.lang.Integer.valueOf(size), minValue, maxValue))
        .withMessageTemplate(s"size [${size.toString}] is not between $minValue and $maxValue")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }

  /* Private */

  private[this] def isValid(value: Long, minValue: Long, maxValue: Long): Boolean =
    minValue <= value && value <= maxValue
}
