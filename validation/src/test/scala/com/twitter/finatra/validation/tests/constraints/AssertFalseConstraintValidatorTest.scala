package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode, ValidationResult}
import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{AssertFalse, AssertFalseConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.AssertFalseExample
import org.scalacheck.Arbitrary
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AssertFalseConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass for valid values") {
    val passValue = Arbitrary.arbBool.arbitrary.filter(_ == false)

    forAll(passValue) { value =>
      validate[AssertFalseExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail for invalid values") {
    val failValue = Arbitrary.arbBool.arbitrary.filter(_ == true)

    forAll(failValue) { value =>
      validate[AssertFalseExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidBooleanValue(value))
      )
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "boolValue", classOf[AssertFalse], value)
  }

  private def errorMessage(value: Boolean): String = {
    AssertFalseConstraintValidator.errorMessage(messageResolver, value)
  }
}
