package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.{CaseClassValidationProvider, ValidationProvider}
import com.twitter.finatra.validation.ValidationProvider
import com.twitter.inject.Test

class ValidationProviderTest extends Test {

  val provider: ValidationProvider = CaseClassValidationProvider

  test("provider generates new validator instances") {
    val a = provider()
    val b = provider()

    a should not equal b
  }

}
