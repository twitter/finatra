package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidatorTest
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult._

case class OneOfExample(@OneOf(value = Array("a", "B", "c")) enumValue: String)
case class OneOfSeqExample(@OneOf(Array("a", "B", "c")) enumValue: Seq[String])
case class OneOfInvalidTypeExample(@OneOf(Array("a", "B", "c")) enumValue: Long)

class OneOfValidatorTest extends ValidatorTest {

  val oneOfValues = Set("a", "B", "c")

  "one of validator" should {

    "pass validation for single value" in {
      val value = "a"
      validate[OneOfExample](value) should equal(
        valid(
          errorMessage(
            value)))
    }

    "fail validation for single value" in {
      val value = "A"
      validate[OneOfExample](value) should equal(
        invalid(
          errorMessage(
            value)))
    }

    "pass validation for seq" in {
      val value = oneOfValues
      validate[OneOfSeqExample](value) should equal(
        valid(
          errorMessage(
            value)))
    }

    "pass validation for empty seq" in {
      val value = Seq()
      validate[OneOfSeqExample](value) should equal(
        valid(
          errorMessage(
            value)))
    }

    "fail validation for invalid value in seq" in {
      val value = Seq("z")
      validate[OneOfSeqExample](value) should equal(
        invalid(
          errorMessage(
            value)))
    }

    "fail validation for invalid type" in {
      val value = 2
      validate[OneOfInvalidTypeExample](value) should equal(
        invalid(
          errorMessage(
            value)))
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    validate(manifest[C].erasure, "enumValue", classOf[OneOf], value)
  }

  private def errorMessage(value: Any): String = {
    OneOfValidator.errorMessage(
      messageResolver,
      oneOfValues,
      value)
  }
}