package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.tests.caseclasses.AssertTrueExample
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.finatra.validation.constraints.AssertTrue
import com.twitter.util.validation.conversions.ConstraintViolationOps._
import jakarta.validation.ConstraintViolation
import org.scalacheck.Arbitrary
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AssertTrueConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass for valid values") {
    val passValue = Arbitrary.arbBool.arbitrary.filter(_ == true)

    forAll(passValue) { value =>
      validate[AssertTrueExample](value).isEmpty should be(true)
    }
  }

  test("fail for invalid values") {
    val failValue = Arbitrary.arbBool.arbitrary.filter(_ == false)

    forAll(failValue) { value =>
      val violations = validate[AssertTrueExample](value)
      val violation = violations.head
      violation.getPropertyPath.toString should equal("boolValue")
      violation.getMessage should be("must be true")
      val payload = violation.getDynamicPayload(classOf[ErrorCode.InvalidBooleanValue])
      payload.isDefined should be(true)
      payload.get should equal(ErrorCode.InvalidBooleanValue(false))
    }
  }

  private def validate[T: Manifest](value: Boolean): Set[ConstraintViolation[T]] = {
    super.validate[AssertTrue, T](manifest[T].runtimeClass, "boolValue", value)
  }
}
