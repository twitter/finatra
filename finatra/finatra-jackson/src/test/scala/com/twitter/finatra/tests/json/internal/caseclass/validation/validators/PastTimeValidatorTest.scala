package com.twitter.finatra.tests.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidatorTest
import com.twitter.finatra.json.internal.caseclass.validation.validators.PastTimeValidator._
import com.twitter.finatra.validation.{PastTime, ValidationResult}
import com.twitter.finatra.validation.ValidationResult._
import org.joda.time.DateTime


class PastTimeValidatorTest extends ValidatorTest {

  "past validator" should {

    "pass validation for valid datetime" in {
      val minDateTime = new DateTime(0)
      validate[PastExample](minDateTime) should equal(valid)
    }

    "fail validation for invalid datetime" in {
      val futureDateTime = DateTime.now().plusDays(1)
      validate[PastExample](futureDateTime) should equal(
        invalid(
          errorMessage(messageResolver, futureDateTime)))
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "dateTime", classOf[PastTime], value)
  }
}

case class PastExample(
  @PastTime dateTime: DateTime)
