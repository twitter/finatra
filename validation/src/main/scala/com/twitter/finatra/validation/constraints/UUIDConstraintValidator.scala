package com.twitter.finatra.validation.constraints

import com.twitter.util.Try
import java.util.{UUID => JUUID}
import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object UUIDConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: String): String =
    resolver.resolve(classOf[UUID], value)

  def isValid(value: String): Boolean = Try(JUUID.fromString(value)).isReturn
}

/**
 * The default validator for [[UUID]] annotation.
 *
 * Validate if the value of the field is a UUID.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class UUIDConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[UUID, String](messageResolver) {

  override def isValid(annotation: UUID, value: String): ValidationResult =
    ValidationResult.validate(
      UUIDConstraintValidator.isValid(value),
      UUIDConstraintValidator.errorMessage(messageResolver, value),
      ErrorCode.InvalidUUID(value)
    )
}
