package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{NotEmpty, NotEmptyConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.{
  NotEmptyArrayExample,
  NotEmptyExample,
  NotEmptyInvalidTypeExample,
  NotEmptySeqExample
}
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode, ValidationResult}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class NotEmptyConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for valid value") {
    val passValue = Gen.alphaStr.filter(_.length > 0)

    forAll(passValue) { value =>
      validate[NotEmptyExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for invalid value") {
    val failValue = ""

    validate[NotEmptyExample](failValue) should equal(
      Invalid(errorMessage, ErrorCode.ValueCannotBeEmpty)
    )
  }

  test("pass validation for all whitespace value") {
    val whiteSpaceValue = for (n <- Gen.choose(1, 100)) yield Seq.fill(n) { ' ' }

    val passValue = whiteSpaceValue.map(_.mkString)

    forAll(passValue) { value =>
      validate[NotEmptyExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("pass validation for valid values in array") {
    val passValue = Gen.nonEmptyContainerOf[Seq, String](Gen.alphaStr)

    forAll(passValue) { value =>
      validate[NotEmptyArrayExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for empty Array") {
    val failValue = Seq.empty
    validate[NotEmptyArrayExample](failValue) should equal(
      Invalid(errorMessage, ErrorCode.ValueCannotBeEmpty)
    )
  }

  test("pass validation for valid values in seq") {
    val passValue = Gen.nonEmptyContainerOf[Seq, String](Gen.alphaStr)

    forAll(passValue) { value =>
      validate[NotEmptySeqExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for empty seq") {
    val failValue = Seq.empty
    validate[NotEmptySeqExample](failValue) should equal(
      Invalid(errorMessage, ErrorCode.ValueCannotBeEmpty)
    )
  }

  test("fail validation for invalid type") {
    intercept[IllegalArgumentException] {
      validate[NotEmptyInvalidTypeExample](2)
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult =
    super.validate(manifest[C].runtimeClass, "stringValue", classOf[NotEmpty], value)

  private def errorMessage: String = NotEmptyConstraintValidator.errorMessage(messageResolver)
}
