package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object RangeConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: Any, minValue: Long, maxValue: Long): String =
    resolver.resolve(classOf[Range], value, minValue, maxValue)
}

/**
 * The validator for [[Range]] annotation.
 *
 * Validate if a if if the value of the field is within the min and max value defined in the annotation.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class RangeConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[Range, Any](messageResolver) {

  /* Public */

  override def isValid(annotation: Range, value: Any): ValidationResult = {
    val rangeAnnotation = annotation.asInstanceOf[Range]
    val minValue = rangeAnnotation.min()
    val maxValue = rangeAnnotation.max()
    value match {
      case bigDecimalValue: BigDecimal =>
        validationResult(bigDecimalValue, minValue, maxValue)
      case bigIntValue: BigInt =>
        validationResult(bigIntValue, minValue, maxValue)
      case numberValue: Number =>
        validationResult(numberValue, minValue, maxValue)
      case _ =>
        throw new IllegalArgumentException(
          s"Class [${value.getClass}] is not supported by ${this.getClass}")
    }
  }

  /* Private */

  private[this] def validationResult(value: BigDecimal, minValue: Long, maxValue: Long) =
    ValidationResult.validate(
      BigDecimal(minValue) <= value && value <= BigDecimal(maxValue),
      errorMessage(value, minValue, maxValue),
      errorCode(value, minValue, maxValue)
    )

  private[this] def errorMessage(value: Number, minValue: Long, maxValue: Long): String =
    RangeConstraintValidator.errorMessage(messageResolver, value, minValue, maxValue)

  private[this] def errorCode(value: Number, minValue: Long, maxValue: Long): ErrorCode =
    ErrorCode.ValueOutOfRange(java.lang.Long.valueOf(value.longValue), minValue, maxValue)

  private[this] def validationResult(value: BigInt, minValue: Long, maxValue: Long) =
    ValidationResult.validate(
      BigInt(minValue) <= value && value <= BigInt(maxValue),
      errorMessage(value, minValue, maxValue),
      errorCode(value, minValue, maxValue)
    )

  private[this] def validationResult(value: Number, minValue: Long, maxValue: Long) = {
    val doubleValue = value.doubleValue()
    ValidationResult.validate(
      minValue <= doubleValue && doubleValue <= maxValue,
      errorMessage(value, minValue, maxValue),
      errorCode(value, minValue, maxValue)
    )
  }
}
