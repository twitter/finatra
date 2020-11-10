package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{Pattern, PatternConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.{
  InvalidPatternExample,
  NumberPatternArrayExample,
  NumberPatternExample
}
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode, ValidationResult}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class PatternConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation when regex matches for array type") {
    val passValue = for {
      size <- Gen.choose(10, 50)
    } yield Array.fill(size) {
      Gen.choose(10, 100)
    }
    forAll(passValue) { value =>
      validate[NumberPatternArrayExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("pass validation when regex matches") {
    validate[NumberPatternExample]("12345").isInstanceOf[Valid] shouldBe true
  }

  test("fail validation when regex not matches") {
    validate[NumberPatternExample]("meros") should equal(
      Invalid(errorMessage("meros", "[0-9]+"), ErrorCode.PatternNotMatched("meros", "[0-9]+"))
    )
  }

  test("fail validation when regex not matches for a invalid value in array type") {
    val value = Iterable("invalid", "6666")
    validate[NumberPatternArrayExample](value) should equal(
      Invalid(
        errorMessage(value, "[0-9]+"),
        ErrorCode.PatternNotMatched(value mkString ",", "[0-9]+"))
    )
  }

  test("it should throw exception for invalid class type") {
    the[IllegalArgumentException] thrownBy validate[NumberPatternArrayExample](
      new Object()) should have message
      "Class [java.lang.Object] is not supported by com.twitter.finatra.validation.constraints.PatternConstraintValidator"
  }

  test("pass validation when regex matches for traversable type") {
    forAll(Iterable("1234", "6666")) { value =>
      validate[NumberPatternArrayExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation when regex is invalid") {
    validate[InvalidPatternExample](value = "123") should equal(
      Invalid(
        "java.util.regex.PatternSyntaxException",
        ErrorCode.PatternSyntaxError("Unclosed character class near index 2\n([)\n  ^", "([)"))
    )
  }

  private def validate[C: Manifest](value: Any): ValidationResult =
    super.validate(manifest[C].runtimeClass, "stringValue", classOf[Pattern], value)

  private def errorMessage(value: Any, regex: String): String =
    PatternConstraintValidator.errorMessage(messageResolver, value, regex)
}
