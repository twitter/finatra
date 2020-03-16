package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{OneOf, OneOfConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.{
  OneOfExample,
  OneOfInvalidTypeExample,
  OneOfSeqExample
}
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode, ValidationResult}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class OneOfConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  val oneOfValues: Set[String] = Set("a", "B", "c")

  test("pass validation for single value") {
    oneOfValues.foreach { value =>
      validate[OneOfExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for single value") {
    val failValue = Gen.alphaStr.filter(!oneOfValues.contains(_))

    forAll(failValue) { value =>
      validate[OneOfExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidValues(Set(value), oneOfValues))
      )
    }
  }

  test("pass validation for seq") {
    val passValue = Gen.nonEmptyContainerOf[Seq, String](Gen.oneOf(oneOfValues.toSeq))

    forAll(passValue) { value =>
      validate[OneOfSeqExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("pass validation for empty seq") {
    val emptySeq = Seq.empty
    validate[OneOfSeqExample](emptySeq).isInstanceOf[Valid] shouldBe true
  }

  test("fail validation for invalid value in seq") {
    val failValue =
      Gen.nonEmptyContainerOf[Seq, String](Gen.alphaStr.filter(!oneOfValues.contains(_)))

    forAll(failValue) { value =>
      validate[OneOfSeqExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidValues(value.toSet, oneOfValues))
      )
    }
  }

  test("fail validation for invalid type") {
    val failValue = Gen.choose(0, 100)

    forAll(failValue) { value =>
      validate[OneOfInvalidTypeExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidValues(Set(value.toString), oneOfValues))
      )
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult =
    super.validate(manifest[C].runtimeClass, "enumValue", classOf[OneOf], value)

  private def errorMessage(value: Any): String =
    OneOfConstraintValidator.errorMessage(messageResolver, oneOfValues, value)
}
