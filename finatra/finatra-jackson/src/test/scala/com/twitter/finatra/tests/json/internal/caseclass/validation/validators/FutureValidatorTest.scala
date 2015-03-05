package com.twitter.finatra.tests.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.FutureTimeValidator
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{FutureTime, ValidationResult, ValidatorTest}
import org.joda.time.DateTime

class FutureValidatorTest extends ValidatorTest {

  "future validator" should {

    "pass validation for valid datetime" in {
      val futureDateTime = DateTime.now().plusDays(1)
      validate[FutureExample](futureDateTime) should equal(valid)
    }

    "fail validation for invalid datetime" in {
      val minDateTime = new DateTime(0)
      validate[FutureExample](minDateTime) should equal(
        invalid(
          errorMessage(minDateTime)))
    }
  }

  private def validate[C : Manifest](value: DateTime): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "dateTime", classOf[FutureTime], value)
  }

  private def errorMessage(value: DateTime) = {
    FutureTimeValidator.errorMessage(messageResolver, value)
  }
}

case class FutureExample(
  @FutureTime dateTime: DateTime)
