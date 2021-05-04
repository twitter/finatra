package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.util.validation.MethodValidation
import com.twitter.util.validation.engine.MethodValidationResult

case class TestUserWithInvalidMethodValidation(name: String) {

  @MethodValidation
  def fooCheck: MethodValidationResult = {
    throw new Exception("method validation error")
  }
}
