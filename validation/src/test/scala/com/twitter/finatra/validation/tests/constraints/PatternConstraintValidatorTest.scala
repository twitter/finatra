package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.finatra.validation.constraints.Pattern
import com.twitter.finatra.validation.constraints.PatternConstraintValidator.toErrorValue
import com.twitter.finatra.validation.tests.caseclasses.{
  InvalidPatternExample,
  NumberPatternArrayExample,
  NumberPatternExample
}
import com.twitter.util.validation.conversions.ConstraintViolationOps._
import jakarta.validation.{ConstraintViolation, UnexpectedTypeException}
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
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
      validate[NumberPatternArrayExample](value).isEmpty shouldBe true
    }
  }

  test("pass validation when regex matches") {
    validate[NumberPatternExample]("12345").isEmpty shouldBe true
  }

  test("fail validation when regex not matches") {
    val violations = validate[NumberPatternExample]("meros")
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("stringValue")
    violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue, "[0-9]+"))
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.PatternNotMatched])
    payload.isDefined should be(true)
    payload.get.value.isEmpty should be(false)
    payload.get.regex should equal("[0-9]+")
  }

  test("fail validation when regex not matches for a invalid value in array type") {
    val value = Iterable("invalid", "6666")
    val violations = validate[NumberPatternArrayExample](value)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("stringValue")
    violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue, "[0-9]+"))
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.PatternNotMatched])
    payload.isDefined should be(true)
    payload.get.value.isEmpty should be(false)
    payload.get.regex should equal("[0-9]+")
  }

  test("it should throw exception for invalid class type") {
    the[UnexpectedTypeException] thrownBy validate[NumberPatternArrayExample](
      new Object()) should have message
      "Class [java.lang.Object] is not supported by com.twitter.finatra.validation.constraints.PatternConstraintValidator"
  }

  test("pass validation when regex matches for traversable type") {
    forAll(Iterable("1234", "6666")) { value =>
      validate[NumberPatternArrayExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation when regex is invalid") {
    val violations = validate[InvalidPatternExample](value = "123")
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("stringValue")
    violations.head.getMessage should be("java.util.regex.PatternSyntaxException")
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.PatternSyntaxError])
    payload.isDefined should be(true)
    payload.get should equal(
      ErrorCode.PatternSyntaxError("Unclosed character class near index 2\n([)\n  ^", "([)"))
  }

  private def validate[T: Manifest](value: Any): Set[ConstraintViolation[T]] = {
    super.validate[Pattern, T](manifest[T].runtimeClass, "stringValue", value)
  }

  private def errorMessage(value: Any, regex: String): String =
    s"[${toErrorValue(value)}] does not match regex $regex"
}
