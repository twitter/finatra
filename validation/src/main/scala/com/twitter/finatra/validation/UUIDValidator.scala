package com.twitter.finatra.validation

import com.twitter.finatra.validation.UUIDValidator._
import com.twitter.util.Try
import java.util.{UUID => JUUID}

private[validation] object UUIDValidator {

  def errorMessage(resolver: ValidationMessageResolver, value: String) = {

    resolver.resolve(classOf[UUID], value)
  }

  def isValid(value: String) = {
    Try(JUUID.fromString(value)).isReturn
  }
}

private[validation] class UUIDValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: UUID
) extends Validator[UUID, String](validationMessageResolver, annotation) {

  override def isValid(value: String) = {
    ValidationResult.validate(
      UUIDValidator.isValid(value),
      errorMessage(validationMessageResolver, value),
      ErrorCode.InvalidUUID(value)
    )
  }
}
