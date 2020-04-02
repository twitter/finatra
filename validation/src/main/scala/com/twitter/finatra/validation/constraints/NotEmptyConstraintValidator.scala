package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object NotEmptyConstraintValidator {

  def errorMessage(resolver: MessageResolver): String =
    resolver.resolve(classOf[NotEmpty])
}

/**
 * The validator for [[NotEmpty]] annotation.
 *
 * Validate if a given value is not empty.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class NotEmptyConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[NotEmpty, Any](messageResolver) {

  import NotEmptyConstraintValidator._

  /* Public */

  override def isValid(annotation: NotEmpty, value: Any): ValidationResult =
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue)
      case traversableValue: Traversable[_] =>
        validationResult(traversableValue)
      case stringValue: String =>
        validationResult(stringValue)
      case _ =>
        throw new IllegalArgumentException(
          s"Class [${value.getClass}}] is not supported by ${this.getClass}")
    }

  /* Private */

  private[this] def validationResult(value: Traversable[_]) =
    ValidationResult.validate(
      value.nonEmpty,
      errorMessage(messageResolver),
      ErrorCode.ValueCannotBeEmpty
    )

  private[this] def validationResult(value: String) =
    ValidationResult.validate(
      value.nonEmpty,
      errorMessage(messageResolver),
      ErrorCode.ValueCannotBeEmpty
    )
}
