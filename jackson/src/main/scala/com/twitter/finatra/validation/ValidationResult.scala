package com.twitter.finatra.validation

import java.lang.annotation.Annotation

sealed trait ValidationResult {
  def isValid: Boolean
  def annotation: Option[Annotation]
}

object ValidationResult {
  object Valid {
    def apply(annotation: Option[Annotation] = None) = new Valid(annotation)
  }

  case class Valid(
    override val annotation: Option[Annotation] = None
  ) extends ValidationResult {
    override val isValid: Boolean = true
  }

  case class Invalid(
    message: String,
    code: ErrorCode = ErrorCode.Unknown,
    override val annotation: Option[Annotation] = None
  ) extends ValidationResult {
    override val isValid = false
  }

  @deprecated(
    "Use ValidationResult.validate() to test a condition that must be true, validateNot() otherwise",
    "2015-11-04"
  )
  def apply(
    isValid: Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown
  ): ValidationResult = {
    validate(isValid, message, code)
  }

  def validate(
    condition: Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown
  ): ValidationResult = {
    if (condition)
      Valid()
    else
      Invalid(message, code)
  }

  def validateNot(
    condition: Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown
  ): ValidationResult = {
    validate(!condition, message, code)
  }
}
