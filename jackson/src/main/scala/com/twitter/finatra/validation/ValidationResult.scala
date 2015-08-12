package com.twitter.finatra.validation

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

  def apply(isValid: Boolean, message: => String, code: ErrorCode = ErrorCode.Unknown): ValidationResult = {
    if (isValid) Valid
    else Invalid(message, code)
  }
}
