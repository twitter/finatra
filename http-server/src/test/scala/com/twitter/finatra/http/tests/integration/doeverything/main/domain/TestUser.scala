package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.validation.constraints.Size
import com.twitter.util.validation.MethodValidation
import com.twitter.util.validation.engine.MethodValidationResult

case class TestUser(@Size(min = 2, max = 20) name: String) {

  @MethodValidation
  def fooCheck: MethodValidationResult = {
    MethodValidationResult.validIfTrue(name != "foo", "name cannot be foo")
  }
}
