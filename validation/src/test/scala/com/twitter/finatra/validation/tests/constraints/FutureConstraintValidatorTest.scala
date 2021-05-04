package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.finatra.validation.constraints.FutureTime
import com.twitter.finatra.validation.tests.caseclasses.FutureExample
import com.twitter.util.validation.conversions.ConstraintViolationOps._
import jakarta.validation.ConstraintViolation
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class FutureConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for valid datetime") {
    val futureDateTimeMillis =
      Gen.choose(DateTime.now().getMillis, DateTime.now().plusWeeks(5).getMillis)

    forAll(futureDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[FutureExample](dateTimeValue).isEmpty shouldBe true
    }
  }

  test("fail validation for invalid datetime") {
    val passDateTimeMillis =
      Gen.choose(DateTime.now().minusWeeks(5).getMillis, DateTime.now().getMillis)

    forAll(passDateTimeMillis) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      val violations = validate[FutureExample](dateTimeValue)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("dateTime")
      violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.TimeNotFuture])
      payload.isDefined should be(true)
      payload.get should equal(
        ErrorCode.TimeNotFuture(violations.head.getInvalidValue.asInstanceOf[DateTime]))
    }
  }

  private def validate[T: Manifest](value: DateTime): Set[ConstraintViolation[T]] = {
    super.validate[FutureTime, T](manifest[T].runtimeClass, "dateTime", value)
  }

  private def errorMessage(value: Any): String = {
    s"[${value.toString}] is not in the future"
  }
}
