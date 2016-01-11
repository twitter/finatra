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

  @deprecated("Use ValidationResult.validate() to test a condition that must be true, validateNot() otherwise", "2015-11-04")
  def apply(isValid: Boolean, message: => String, code: ErrorCode = ErrorCode.Unknown): ValidationResult = {
    validate(isValid, message, code)
  }

  def validate(
    condition: Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown): ValidationResult = {
    if (condition)
      Valid
    else
      Invalid(message, code)
  }

  def validateNot(
    condition: Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown): ValidationResult = {
    validate(!condition, message, code)
  }
}
