package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{PastTime, PastTimeConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.PastExample
import com.twitter.finatra.validation.{
  ConstraintValidatorTest,
  ErrorCode,
  ValidationResult
}
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class PastTimeConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for valid datetime") {
    val passDateTimeMillis =
      Gen.choose(DateTime.now().minusWeeks(5).getMillis, DateTime.now().getMillis)

    forAll(passDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[PastExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for invalid datetime") {
    val futureDateTimeMillis =
      Gen.choose(DateTime.now().getMillis, DateTime.now().plusWeeks(5).getMillis)

    forAll(futureDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[PastExample](dateTimeValue) should equal(
        Invalid(
          PastTimeConstraintValidator.errorMessage(messageResolver, dateTimeValue),
          ErrorCode.TimeNotPast(dateTimeValue))
      )
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult =
    super.validate(manifest[C].runtimeClass, "dateTime", classOf[PastTime], value)
}
