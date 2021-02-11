package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.validation.constraints.Size
import com.twitter.finatra.validation.{MethodValidation, ValidationResult}

case class TestUser(@Size(min = 2, max = 20) name: String) {

  @MethodValidation
  def fooCheck = {
    ValidationResult.validate(name != "foo", "name cannot be foo")
  }
}
