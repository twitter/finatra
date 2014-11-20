package com.twitter.finatra.tests.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidationResult._
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.validators.PastTimeValidator._
import com.twitter.finatra.json.{ValidationResult, ValidatorTest}
import org.joda.time.DateTime


case class PastExample(@PastTime dateTime: DateTime)

class PastTimeValidatorTest extends ValidatorTest {

  "past validator" should {

    "pass validation for valid datetime" in {
      val minDateTime = new DateTime(0)
      validate[PastExample](minDateTime) should equal(
        valid(
          errorMessage(messageResolver, minDateTime)))
    }

    "fail validation for invalid datetime" in {
      val maxDateTime = new DateTime(Long.MaxValue)
      validate[PastExample](maxDateTime) should equal(
        invalid(
          errorMessage(messageResolver, maxDateTime)))
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].erasure, "dateTime", classOf[PastTime], value)
  }
}
