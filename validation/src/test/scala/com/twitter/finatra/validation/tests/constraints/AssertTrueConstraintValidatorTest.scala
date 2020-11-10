package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{AssertTrue, AssertTrueConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.AssertTrueExample
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode, ValidationResult}
import org.scalacheck.Arbitrary
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AssertTrueConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass for valid values") {
    val passValue = Arbitrary.arbBool.arbitrary.filter(_ == true)

    forAll(passValue) { value =>
      validate[AssertTrueExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail for invalid values") {
    val failValue = Arbitrary.arbBool.arbitrary.filter(_ == false)

    forAll(failValue) { value =>
      validate[AssertTrueExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidBooleanValue(value))
      )
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "boolValue", classOf[AssertTrue], value)
  }

  private def errorMessage(value: Boolean): String = {
    AssertTrueConstraintValidator.errorMessage(messageResolver, value)
  }
}
