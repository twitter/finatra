package com.twitter.finatra.json.internal.caseclass.validation

object ValidationResult {

  def validate(valid: Boolean, reason: => String) = {
    ValidationResult(valid, reason)
  }

  def valid: ValidationResult = valid("")

  def valid(reason: String) = ValidationResult(valid = true, reason)

  def invalid(reason: String) = ValidationResult(valid = false, reason)
}

case class ValidationResult(
  valid: Boolean,
  reason: String)
