package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{FutureTime, FutureTimeConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.FutureExample
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode, ValidationResult}
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class FutureConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for valid datetime") {
    val futureDateTimeMillis =
      Gen.choose(DateTime.now().getMillis, DateTime.now().plusWeeks(5).getMillis)

    forAll(futureDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[FutureExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for invalid datetime") {
    val passDateTimeMillis =
      Gen.choose(DateTime.now().minusWeeks(5).getMillis, DateTime.now().getMillis)

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
    FutureTimeConstraintValidator.errorMessage(messageResolver, value)
  }
}
