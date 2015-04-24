package com.twitter.finatra.validation

object ValidationResult {

  def apply(isValid: Boolean, failedReason: => String): ValidationResult = {
    if (isValid)
      valid
    else
      invalid(failedReason)
  }

  val valid: ValidationResult = {
    ValidationResult(valid = true)
  }

  def invalid(failedReason: String) = {
    ValidationResult(
      valid = false,
      Some(failedReason))
  }
}

case class ValidationResult(
  valid: Boolean,
  failedReason: Option[String] = None)
