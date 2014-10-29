package com.twitter.finatra.json.internal.caseclass.validation.domain

import com.twitter.finatra.json.ValidationResult
import com.twitter.finatra.json.annotations.{MethodValidation, NotEmpty}

case class Address(
  @NotEmpty street: Option[String] = None,
  @NotEmpty city: String,
  @NotEmpty state: String) {

  @MethodValidation
  def validateState = {
    ValidationResult(
      state == "CA" || state == "MD" || state == "WI",
      "state can be one of [CA, MD, WI]")
  }
}