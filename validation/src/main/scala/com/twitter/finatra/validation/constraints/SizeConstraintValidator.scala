package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object SizeConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: Any, minValue: Long, maxValue: Long): String =
    resolver.resolve(classOf[Size], toErrorValue(value), minValue, maxValue)

  private def toErrorValue(value: Any): Int =
    value match {
      case arrayValue: Array[_] =>
        arrayValue.length
      case traversableValue: Traversable[_] =>
        traversableValue.size
      case str: String =>
        str.length
      case _ =>
        throw new IllegalArgumentException(s"Class [${value.getClass}] is not supported")
    }
}

/**
 * The validator for [[Size]] annotation.
 *
 * Validate if a if if the size of the field is within the min and max value defined in the annotation.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class SizeConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[Size, Any](messageResolver) {

  /* Public */

  override def isValid(annotation: Size, value: Any): ValidationResult = {
    val sizeAnnotation = annotation.asInstanceOf[Size]
    val minValue: Long = sizeAnnotation.min()
    val maxValue: Long = sizeAnnotation.max()
    val size = value match {
      case arrayValue: Array[_] => arrayValue.length
      case traversableValue: Traversable[_] => traversableValue.size
      case str: String => str.length
      case _ =>
        throw new IllegalArgumentException(s"Class [${value.getClass}] is not supported")
    }

    ValidationResult.validate(
      isValid(size.toLong, minValue, maxValue),
      SizeConstraintValidator.errorMessage(messageResolver, value, minValue, maxValue),
      ErrorCode.SizeOutOfRange(Integer.valueOf(size), minValue, maxValue)
    )
  }

  /* Private */

  private[this] def isValid(value: Long, minValue: Long, maxValue: Long): Boolean =
    minValue <= value && value <= maxValue
}
