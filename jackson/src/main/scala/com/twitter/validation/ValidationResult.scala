package com.twitter.finatra.validation

import com.fasterxml.jackson.core.JsonProcessingException

sealed trait ValidationResult {
  def isValid: Boolean
}

object ValidationResult {
  case object Valid extends ValidationResult {
    override val isValid = true
  }
  
  case class Invalid(message: String, code: ErrorCode = ErrorCode.Unknown) extends ValidationResult {
    override val isValid = false
  }
  
  /**
   * A descriptor for the type of validation error. May be pattern-matched
   * to customize handling specific errors.
   */
  trait ErrorCode

  object ErrorCode {
    case object Unknown extends ErrorCode
    case object RequiredFieldMissing extends ErrorCode
    case class JsonProcessingError(cause: JsonProcessingException) extends ErrorCode
  }

  def apply(isValid: Boolean, message: => String, code: ErrorCode = ErrorCode.Unknown): ValidationResult = {
    if (isValid) Valid
    else Invalid(message, code)
  }
}
