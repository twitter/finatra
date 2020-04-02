package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{UUID, UUIDConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.UUIDExample
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode, ValidationResult}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class UUIDConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for valid uuid") {
    val passValue = Gen.uuid

    forAll(passValue) { value =>
      validate[UUIDExample](value.toString).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for valid uuid") {
    val passValue = Gen.alphaStr

    forAll(passValue) { value =>
      validate[UUIDExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidUUID(value))
      )
    }
  }

  private def validate[C: Manifest](value: String): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "uuid", classOf[UUID], value)
  }

  private def errorMessage(value: String): String =
    UUIDConstraintValidator.errorMessage(messageResolver, value)
}
