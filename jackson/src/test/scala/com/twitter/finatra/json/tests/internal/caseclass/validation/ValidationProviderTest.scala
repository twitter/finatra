package com.twitter.finatra.json.tests.internal.caseclass.validation

import com.twitter.finatra.json.internal.caseclass.validation.{DefaultValidationProvider, ValidationProvider}
import com.twitter.inject.Test

class ValidationProviderTest extends Test {

  val provider: ValidationProvider = DefaultValidationProvider

  test("provider generates new validator instances") {
    val a = provider()
    val b = provider()

    a should not equal (b)
  }

}
