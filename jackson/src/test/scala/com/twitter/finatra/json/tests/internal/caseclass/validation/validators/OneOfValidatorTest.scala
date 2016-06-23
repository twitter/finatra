package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.OneOfValidator
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{ErrorCode, OneOf, ValidationResult, ValidatorTest}

case class OneOfExample(@OneOf(value = Array("a", "B", "c")) enumValue: String)
case class OneOfSeqExample(@OneOf(Array("a", "B", "c")) enumValue: Seq[String])
case class OneOfInvalidTypeExample(@OneOf(Array("a", "B", "c")) enumValue: Long)

class OneOfValidatorTest extends ValidatorTest {

  val oneOfValues = Set("a", "B", "c")

  "one of validator" should {

    "pass validation for single value" in {
      val value = "a"
      validate[OneOfExample](value) should equal(Valid)
    }

    "fail validation for single value" in {
      val value = "A"
      validate[OneOfExample](value) should equal(
        Invalid(
          errorMessage(
            value),
        ErrorCode.InvalidValues(Set(value), oneOfValues)))
    }

    "pass validation for seq" in {
      val value = oneOfValues
      validate[OneOfSeqExample](value) should equal(Valid)
    }

    "pass validation for empty seq" in {
      val value = Seq()
      validate[OneOfSeqExample](value) should equal(Valid)
    }

    "fail validation for invalid value in seq" in {
      val value = Seq("z")
      validate[OneOfSeqExample](value) should equal(
        Invalid(
          errorMessage(
            value),
        ErrorCode.InvalidValues(value.toSet, oneOfValues)))
    }

    "fail validation for invalid type" in {
      val value = 2
      validate[OneOfInvalidTypeExample](value) should equal(
        Invalid(
          errorMessage(
            value),
        ErrorCode.InvalidValues(Set("2"), oneOfValues)))
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "enumValue", classOf[OneOf], value)
  }

  private def errorMessage(value: Any): String = {
    OneOfValidator.errorMessage(
      messageResolver,
      oneOfValues,
      value)
  }
}
