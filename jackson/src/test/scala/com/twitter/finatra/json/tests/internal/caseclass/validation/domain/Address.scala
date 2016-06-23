package com.twitter.finatra.json.tests.internal.caseclass.validation.domain

import com.twitter.finatra.validation.{MethodValidation, NotEmpty, ValidationResult}

case class Address(
  @NotEmpty street: Option[String] = None,
  @NotEmpty city: String,
  @NotEmpty state: String) {

  @MethodValidation
  def validateState = {
    ValidationResult.validate(
      state == "CA" || state == "MD" || state == "WI",
      "state must be one of [CA, MD, WI]")
  }
}
