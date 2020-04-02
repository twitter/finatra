package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object MaxConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: Any, maxValue: Long): String =
    resolver.resolve(classOf[Max], value, maxValue)
}

/**
 * The validator for [[Max]] annotation.
 *
 * Validate if a given value is less than or equal to the value defined in [[Max]] annotation.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class MaxConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[Max, Any](messageResolver) {

  /* Public */

  override def isValid(annotation: Max, value: Any): ValidationResult = {
    val maxValue = annotation.asInstanceOf[Max].value()
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue, maxValue)
      case traversableValue: Traversable[_] =>
        validationResult(traversableValue, maxValue)
      case bigDecimalValue: BigDecimal =>
        validationResult(bigDecimalValue, maxValue)
      case bigIntValue: BigInt =>
        validationResult(bigIntValue, maxValue)
      case numberValue: Number =>
        validationResult(numberValue, maxValue)
      case _ =>
        throw new IllegalArgumentException(
          s"Class [${value.getClass}] is not supported by ${this.getClass}")
    }
  }

  /* Private */

  private[this] def validationResult(value: Traversable[_], maxValue: Long) =
    ValidationResult.validate(
      value.size <= maxValue,
      errorMessage(Integer.valueOf(value.size), maxValue),
      errorCode(Integer.valueOf(value.size), maxValue)
    )

  private[this] def errorMessage(value: Number, maxValue: Long) =
    MaxConstraintValidator.errorMessage(messageResolver, value, maxValue)

  private[this] def errorCode(value: Number, maxValue: Long): ErrorCode =
    ErrorCode.ValueTooLarge(maxValue, value)

  private[this] def validationResult(value: BigDecimal, maxValue: Long) =
    ValidationResult.validate(
      value <= BigDecimal(maxValue),
      errorMessage(value, maxValue),
      errorCode(value, maxValue))

  private[this] def validationResult(value: BigInt, maxValue: Long) =
    ValidationResult.validate(
      value <= BigInt(maxValue),
      errorMessage(value, maxValue),
      errorCode(value, maxValue))

  private[this] def validationResult(value: Number, maxValue: Long) =
    ValidationResult.validate(
      value.doubleValue() <= maxValue,
      errorMessage(value, maxValue),
      errorCode(value, maxValue)
    )
}
