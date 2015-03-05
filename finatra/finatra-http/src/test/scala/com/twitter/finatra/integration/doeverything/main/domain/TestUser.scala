package com.twitter.finatra.integration.doeverything.main.domain

import com.twitter.finatra.validation.{MethodValidation, Size, ValidationResult}

case class TestUser(
  @Size(min = 2, max = 20) name: String) {

  @MethodValidation
  def fooCheck = {
    ValidationResult(
      name != "foo",
      "name cannot be foo")
  }
}
