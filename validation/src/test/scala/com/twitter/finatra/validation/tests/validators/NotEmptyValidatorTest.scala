package com.twitter.finatra.validation.tests.validators

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.{ErrorCode, NotEmpty, NotEmptyValidator, ValidationResult, ValidatorTest}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

case class NotEmptyExample(@NotEmpty stringValue: String)
case class NotEmptySeqExample(@NotEmpty stringValue: Seq[String])
case class NotEmptyInvalidTypeExample(@NotEmpty stringValue: Long)

class NotEmptyValidatorTest extends ValidatorTest with ScalaCheckDrivenPropertyChecks {

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

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "stringValue", classOf[NotEmpty], value)
  }

  private def errorMessage = {
    NotEmptyValidator.errorMessage(messageResolver)
  }
}
