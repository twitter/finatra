package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object MinConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: Any, minValue: Long): String =
    resolver.resolve[Min](value, minValue)
}

/**
 * The validator for [[Min]] annotation.
 *
 * Validate if a given value is greater than or equal to the value defined in [[Min]] annotation.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class MinConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[Min, Any](messageResolver) {

  /* Public */

  override def isValid(annotation: Min, value: Any): ValidationResult = {
    val minValue = annotation.asInstanceOf[Min].value()
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue, minValue)
      case mapValue: Map[_, _] =>
        validationResult(mapValue, minValue)
      case traversableValue: Iterable[_] =>
        validationResult(traversableValue, minValue)
      case bigDecimalValue: BigDecimal =>
        validationResult(bigDecimalValue, minValue)
      case bigIntValue: BigInt =>
        validationResult(bigIntValue, minValue)
      case numberValue: Number =>
        validationResult(numberValue, minValue)
      case _ =>
        throw new IllegalArgumentException(
          s"Class [${value.getClass.getName}] is not supported by ${this.getClass.getName}")
    }
  }

  /* Private */

  private[this] def validationResult(value: Iterable[_], minValue: Long) = {
    val size = value.size
    ValidationResult.validate(
      minValue <= size,
      errorMessage(Integer.valueOf(size), minValue),
      errorCode(Integer.valueOf(size), minValue)
    )
  }

  private[this] def errorMessage(value: Number, minValue: Long) =
    MinConstraintValidator.errorMessage(messageResolver, value, minValue)

  private[this] def errorCode(value: Number, minValue: Long): ErrorCode =
    ErrorCode.ValueTooSmall(minValue, value)

  private[this] def validationResult(value: BigDecimal, minValue: Long) =
    ValidationResult.validate(
      BigDecimal(minValue) <= value,
      errorMessage(value, minValue),
      errorCode(value, minValue))

  private[this] def validationResult(value: BigInt, minValue: Long) =
    ValidationResult.validate(
      BigInt(minValue) <= value,
      errorMessage(value, minValue),
      errorCode(value, minValue))

  private[this] def validationResult(value: Number, minValue: Long) =
    ValidationResult.validate(
      minValue <= value.doubleValue(),
      errorMessage(value, minValue),
      errorCode(value, minValue)
    )
}
