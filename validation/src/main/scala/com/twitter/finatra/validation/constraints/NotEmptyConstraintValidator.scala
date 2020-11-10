package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object NotEmptyConstraintValidator {

  def errorMessage(resolver: MessageResolver): String =
    resolver.resolve[NotEmpty]()
}

/**
 * The validator for [[NotEmpty]] annotation. Validate if a given value is not empty.
 *
 * @param messageResolver to resolve error message when validation fails.
 * @note this does not guarantee that items within a "not empty" container are themselves
 *       not null or empty. E.g., a `Seq[String]` may not be empty but some of the contained
 *       Strings may be the empty string or even null. Likewise, a "not empty" `Option`,
 *       could be a `Some(null)`, etc.
 */
private[validation] class NotEmptyConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[NotEmpty, Any](messageResolver) {

  import NotEmptyConstraintValidator._

  /* Public */

  override def isValid(annotation: NotEmpty, value: Any): ValidationResult =
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue)
      case map: Map[_, _] =>
        validationResult(map)
      case traversableValue: Iterable[_] =>
        validationResult(traversableValue)
      case stringValue: String =>
        validationResult(stringValue)
      case _ =>
        throw new IllegalArgumentException(
          s"Class [${value.getClass.getName}] is not supported by ${this.getClass.getName}")
    }

  /* Private */

  private[this] def validationResult(value: Iterable[_]) =
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
