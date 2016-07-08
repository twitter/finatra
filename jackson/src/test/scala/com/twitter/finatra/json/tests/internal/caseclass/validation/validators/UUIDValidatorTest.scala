package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.UUIDValidator
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{ErrorCode, UUID, ValidationResult, ValidatorTest}
import java.util.{UUID => JUUID}

case class UUIDExample(
  @UUID uuid: String)

class UUIDValidatorTest extends ValidatorTest {

  "uuid validator" should {

    "pass validation for valid value" in {
      val value = JUUID.randomUUID().toString
      validate[UUIDExample](value) should equal(Valid)
    }

    "fail validation for invalid value" in {
      val value = "bad uuid"
      validate[UUIDExample](value) should equal(
        Invalid(
          errorMessage(value),
        ErrorCode.InvalidUUID(value)))
    }
  }

  private def validate[C : Manifest](value: String): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "uuid", classOf[UUID], value)
  }

  private def errorMessage(value: String) = {
    UUIDValidator.errorMessage(messageResolver, value)
  }
}
