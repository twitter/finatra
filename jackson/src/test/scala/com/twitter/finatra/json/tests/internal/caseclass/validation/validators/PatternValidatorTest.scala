package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.PatternValidator
import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.{ErrorCode, Pattern, ValidationResult, ValidatorTest}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

case class NumberPatternExample(@Pattern(regexp = "[0-9]+") stringValue: String)

case class NumberPatternArrayExample(@Pattern(regexp = "[0-9]+") stringValue: Array[String])

case class EmptyPatternExample(@Pattern(regexp = "") stringValue: String)


class PatternValidatorTest extends ValidatorTest with GeneratorDrivenPropertyChecks {

  test("pass validation when regex matches for array type") {
    val passValue = for {
      size <- Gen.choose(10, 50)
    } yield Array.fill(size) {
      Gen.choose(10, 100)
    }
    forAll(passValue) {
      value => validate[NumberPatternArrayExample](value) should equal(Valid)
    }
  }

  test("pass validation when regex matches") {
    validate[NumberPatternExample]("12345") should equal(Valid)

  }

  test("fail validation when regex not matches") {
    validate[NumberPatternExample]("meros") should equal(Invalid(errorMessage("meros", "[0-9]+"), ErrorCode.PatternNotMatched("meros", "[0-9]+")))
  }

  test("fail validation when regex not matches for some items in array") {
    val failValue = for {
      size <- Gen.choose(1, 5)
    } yield Array.fill(size) {
      "invalid"
    }
    forAll(failValue) {
      value => validate[NumberPatternArrayExample](value) should equal(Invalid(errorMessage(value.toString, "[0-9]+"), ErrorCode.PatternNotMatched("[]", "[0-9]+")))
    }
  }

  test("fail validation when given regex empty") {
    validate[EmptyPatternExample]("1234") should equal(Invalid(PatternValidator.errorMessage(messageResolver), ErrorCode.PatternCannotBeEmpty))
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "stringValue", classOf[Pattern], value)
  }

  private def errorMessage(value: String, regex: String): String = {
    PatternValidator.errorMessage(messageResolver, value, regex)
  }
}
