package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.FutureTimeValidator
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{ErrorCode, FutureTime, ValidationResult, ValidatorTest}
import org.joda.time.DateTime

class FutureValidatorTest extends ValidatorTest {

  "future validator" should {

    "pass validation for valid datetime" in {
      val futureDateTime = DateTime.now().plusDays(5)
      validate[FutureExample](futureDateTime) should equal(Valid)
    }

    "fail validation for invalid datetime" in {
      val pastDateTime = DateTime.now.minusDays(5)
      validate[FutureExample](pastDateTime) should equal(
        Invalid(
          errorMessage(pastDateTime),
          ErrorCode.TimeNotFuture(pastDateTime)))
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
