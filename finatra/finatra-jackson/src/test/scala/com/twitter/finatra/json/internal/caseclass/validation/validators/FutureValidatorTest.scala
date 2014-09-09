package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidatorTest
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult._
import org.joda.time.DateTime


case class FutureExample(@FutureTime dateTime: DateTime)

class FutureValidatorTest extends ValidatorTest {

  "future validator" should {

    "pass validation for valid datetime" in {
      val maxDateTime = new DateTime(Long.MaxValue)
      validate[FutureExample](maxDateTime) should equal(
        valid(
          errorMessage(maxDateTime)))
    }

    "fail validation for invalid datetime" in {
      val minDateTime = new DateTime(0)
      validate[FutureExample](minDateTime) should equal(
        invalid(
          errorMessage(minDateTime)))
    }
  }

  private def validate[C : Manifest](value: DateTime): ValidationResult = {
    validate(manifest[C].erasure, "dateTime", classOf[FutureTime], value)
  }

  private def errorMessage(value: DateTime) = {
    FutureTimeValidator.errorMessage(messageResolver, value)
  }
}