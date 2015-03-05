package com.twitter.finatra.tests.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.NotEmptyValidator
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{NotEmpty, ValidationResult, ValidatorTest}

case class NotEmptyExample(@NotEmpty stringValue: String)
case class NotEmptySeqExample(@NotEmpty stringValue: Seq[String])
case class NotEmptyInvalidTypeExample(@NotEmpty stringValue: Long)

class NotEmptyValidatorTest extends ValidatorTest {

  "not empty validator" should {

    "pass validation for valid value" in {
      val value = "abc"
      validate[NotEmptyExample](value) should equal(valid)
    }

    "fail validation for invalid value" in {
      val value = ""
      validate[NotEmptyExample](value) should equal(
        invalid(errorMessage))
    }

    "pass validation for all whitespace value" in {
      val value = "    "
      validate[NotEmptyExample](value) should equal(valid)
    }

    "pass validation for valid values in seq" in {
      val value = Seq("abc", "de")
      validate[NotEmptySeqExample](value) should equal(valid)
    }

    "fail validation for empty seq" in {
      val value = Seq()
      validate[NotEmptySeqExample](value) should equal(
        invalid(errorMessage))
    }

    "fail validation for invalid value type" in {
      intercept[IllegalArgumentException] {
        validate[NotEmptyInvalidTypeExample](2)
      }
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "stringValue", classOf[NotEmpty], value)
  }

  private def errorMessage = {
    NotEmptyValidator.errorMessage(messageResolver)
  }
}
