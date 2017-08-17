package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.PastTimeValidator._
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{ErrorCode, PastTime, ValidationResult, ValidatorTest}
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class PastTimeValidatorTest extends ValidatorTest with GeneratorDrivenPropertyChecks {

  test("pass validation for valid datetime") {
    val passDateTimeMillis =
      Gen.choose(DateTime.now().minusWeeks(5).getMillis(), DateTime.now().getMillis)

    forAll(passDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[PastExample](dateTimeValue) should equal(Valid)
    }
  }

  test("fail validation for invalid datetime") {
    val futureDateTimeMillis =
      Gen.choose(DateTime.now().getMillis, DateTime.now().plusWeeks(5).getMillis())

    forAll(futureDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[PastExample](dateTimeValue) should equal(
        Invalid(errorMessage(messageResolver, dateTimeValue), ErrorCode.TimeNotPast(dateTimeValue))
      )
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "dateTime", classOf[PastTime], value)
  }
}

case class PastExample(@PastTime dateTime: DateTime)
