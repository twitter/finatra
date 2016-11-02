package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.OneOfValidator
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{ErrorCode, OneOf, ValidationResult, ValidatorTest}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

case class OneOfExample(@OneOf(value = Array("a", "B", "c")) enumValue: String)
case class OneOfSeqExample(@OneOf(Array("a", "B", "c")) enumValue: Seq[String])
case class OneOfInvalidTypeExample(@OneOf(Array("a", "B", "c")) enumValue: Long)

class OneOfValidatorTest
  extends ValidatorTest
  with GeneratorDrivenPropertyChecks {

  val oneOfValues = Set("a", "B", "c")

  "one of validator" should {

    "pass validation for single value" in {
      oneOfValues.foreach { value =>
        validate[OneOfExample](value) should equal(Valid)
      }
    }

    "fail validation for single value" in {
      val failValue = Gen.alphaStr.filter(!oneOfValues.contains(_))

      forAll(failValue) { value =>
        validate[OneOfExample](value) should equal(
          Invalid(
          errorMessage(value),
          ErrorCode.InvalidValues(Set(value), oneOfValues)))
      }
    }

    "pass validation for seq" in {
      val passValue = Gen.nonEmptyContainerOf[Seq, String](Gen.oneOf(oneOfValues.toSeq))

      forAll(passValue) { value =>
        validate[OneOfSeqExample](value) should equal(Valid)
      }
    }

    "pass validation for empty seq" in {
      val emptySeq = Seq.empty
      validate[OneOfSeqExample](emptySeq) should equal(Valid)
    }

    "fail validation for invalid value in seq" in {
      val failValue = Gen.nonEmptyContainerOf[Seq, String](Gen.alphaStr.filter(!oneOfValues.contains(_)))

      forAll(failValue) { value =>
        validate[OneOfSeqExample](value) should equal(
          Invalid(
          errorMessage(value),
          ErrorCode.InvalidValues(value.toSet, oneOfValues)))
      }
    }

    "fail validation for invalid type" in {
      val failValue = Gen.choose(0, 100)

      forAll(failValue) { value =>
        validate[OneOfInvalidTypeExample](value) should equal(
          Invalid(
          errorMessage(value),
          ErrorCode.InvalidValues(Set(value.toString), oneOfValues)))
      }
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "enumValue", classOf[OneOf], value)
  }

  private def errorMessage(value: Any): String = {
    OneOfValidator.errorMessage(
      messageResolver,
      oneOfValues,
      value)
  }
}
