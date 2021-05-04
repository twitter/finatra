package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.constraints.AssertFalse
import com.twitter.finatra.validation.tests.caseclasses.AssertFalseExample
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.util.validation.conversions.ConstraintViolationOps._
import jakarta.validation.ConstraintViolation
import org.scalacheck.Arbitrary
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AssertFalseConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass for valid values") {
    val passValue = Arbitrary.arbBool.arbitrary.filter(_ == false)

    forAll(passValue) { value =>
      validate[AssertFalseExample](value).isEmpty shouldBe true
    }
  }

  test("fail for invalid values") {
    val failValue = Arbitrary.arbBool.arbitrary.filter(_ == true)

    forAll(failValue) { value =>
      val violations = validate[AssertFalseExample](value)
      violations.size should equal(1)
      val violation = violations.head
      violation.getPropertyPath.toString should equal("boolValue")
      violation.getMessage should be("must be false")
      val payload = violation.getDynamicPayload(classOf[ErrorCode.InvalidBooleanValue])
      payload.isDefined should be(true)
      payload.get should equal(ErrorCode.InvalidBooleanValue(true))
    }
  }

  private def validate[C: Manifest](
    value: Any
  ): Set[ConstraintViolation[C]] = {
    super.validate[AssertFalse, C](manifest[C].runtimeClass, "boolValue", value)
  }
}
