package com.twitter.finatra

import scala.annotation.meta.param

package object validation {
  @deprecated("Use com.twitter.util.validation.MethodValidation", "2021-03-05")
  type MethodValidation = com.twitter.util.validation.MethodValidation @param

  @deprecated("Use com.twitter.util.validation.MethodValidationResult", "2021-03-25")
  type ValidationResult = com.twitter.util.validation.engine.MethodValidationResult
}
