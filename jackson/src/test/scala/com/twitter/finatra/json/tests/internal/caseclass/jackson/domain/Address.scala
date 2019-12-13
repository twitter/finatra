package com.twitter.finatra.json.tests.internal.caseclass.jackson.domain

import com.twitter.finatra.validation.NotEmpty
import com.twitter.finatra.validation.{MethodValidation, ValidationResult}

case class Address(
  @NotEmpty street: Option[String] = None,
  @NotEmpty city: String,
  @NotEmpty state: String
) {

  @MethodValidation
  def validateState:ValidationResult = {
    ValidationResult.validate(
      state == "CA" || state == "MD" || state == "WI",
      "state must be one of [CA, MD, WI]"
    )
  }
}
