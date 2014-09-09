package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidatorTest
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult._
import java.util.{UUID => JUUID}

case class UUIDExample(@UUID uuid: String)

class UUIDValidatorTest extends ValidatorTest {

  "uuid validator" should {

    "pass validation for valid value" in {
      val value = JUUID.randomUUID().toString
      validate[UUIDExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for invalid value" in {
      val value = "bad uuid"
      validate[UUIDExample](value) should equal(
        invalid(
          errorMessage(value)))
    }
  }

  private def validate[C : Manifest](value: String): ValidationResult = {
    validate(manifest[C].erasure, "uuid", classOf[UUID], value)
  }

  private def errorMessage(value: String) = {
    UUIDValidator.errorMessage(messageResolver, value)
  }
}