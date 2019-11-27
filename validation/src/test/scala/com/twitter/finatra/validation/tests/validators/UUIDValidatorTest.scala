package com.twitter.finatra.validation.tests.validators

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.{ErrorCode, UUID, UUIDValidator, ValidationResult, ValidatorTest}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

case class UUIDExample(@UUID uuid: String)

class UUIDValidatorTest extends ValidatorTest with GeneratorDrivenPropertyChecks {

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

  private def errorMessage(value: String) = {
    UUIDValidator.errorMessage(messageResolver, value)
  }
}
