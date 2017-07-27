package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.FutureTimeValidator
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{ErrorCode, FutureTime, ValidationResult, ValidatorTest}
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class FutureValidatorTest extends ValidatorTest with GeneratorDrivenPropertyChecks {

  test("pass validation for valid datetime") {
    val futureDateTimeMillis =
      Gen.choose(DateTime.now().getMillis, DateTime.now().plusWeeks(5).getMillis())

    forAll(futureDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[FutureExample](dateTimeValue) should equal(Valid)
    }
  }

  test("fail validation for invalid datetime") {
    val passDateTimeMillis =
      Gen.choose(DateTime.now().minusWeeks(5).getMillis(), DateTime.now().getMillis)

    forAll(passDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[FutureExample](dateTimeValue) should equal(
        Invalid(errorMessage(dateTimeValue), ErrorCode.TimeNotFuture(dateTimeValue))
      )
    }
  }

  private def validate[C: Manifest](value: DateTime): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "dateTime", classOf[FutureTime], value)
  }

  private def errorMessage(value: DateTime) = {
    FutureTimeValidator.errorMessage(messageResolver, value)
  }
}

case class FutureExample(@FutureTime dateTime: DateTime)
