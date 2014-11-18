package com.twitter.finatra.tests.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidationResult
import com.twitter.finatra.json.ValidationResult._
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.validators.SizeValidator
import com.twitter.finatra.tests.json.ValidatorTest


case class SizeArrayExample(@Size(min = 1, max = 5) sizeValue: Array[Int])
case class SizeSeqExample(@Size(min = 1, max = 5) sizeValue: Array[Int])
case class SizeInvalidTypeExample(@Size(min = 1, max = 5) sizeValue: Int)

class SizeValidatorTest extends ValidatorTest {

  "size validator" should {

    "pass validation for array type" in {
      val value = Array(1, 2, 3, 4, 5)
      validate[SizeArrayExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for too few array type" in {
      val value = Array()
      validate[SizeArrayExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "fail validation for too many array type" in {
      val value = Array(1, 2, 3, 4, 5, 6)
      validate[SizeArrayExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for seq type" in {
      val value = Seq(1, 2, 3, 4, 5)
      validate[SizeArrayExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for too few seq type" in {
      val value = Seq()
      validate[SizeArrayExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "fail validation for too many seq type" in {
      val value = Seq(1, 2, 3, 4, 5, 6)
      validate[SizeArrayExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "fail for unsupported class type" in {
      intercept[IllegalArgumentException] {
        validate[SizeInvalidTypeExample](2)
      }
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].erasure, "sizeValue", classOf[Size], value)
  }

  private def errorMessage(value: Any, minValue: Long = 1, maxValue: Long = 5): String = {
    SizeValidator.errorMessage(
      messageResolver,
      value,
      minValue,
      maxValue)
  }

}
