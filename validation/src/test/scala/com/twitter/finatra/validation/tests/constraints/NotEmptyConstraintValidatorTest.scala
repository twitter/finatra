package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.finatra.validation.constraints.NotEmpty
import com.twitter.finatra.validation.tests.caseclasses.{
  NotEmptyArrayExample,
  NotEmptyExample,
  NotEmptyInvalidTypeExample,
  NotEmptyMapExample,
  NotEmptySeqExample
}
import com.twitter.util.validation.conversions.ConstraintViolationOps.RichConstraintViolation
import jakarta.validation.{ConstraintViolation, UnexpectedTypeException}
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

private object NotEmptyConstraintValidatorTest {
  val ErrorMessage: String = "cannot be empty"
}

class NotEmptyConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {
  import NotEmptyConstraintValidatorTest._

  test("pass validation for valid value") {
    val passValue = Gen.alphaStr.filter(_.length > 0)

    forAll(passValue) { value =>
      validate[NotEmptyExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for invalid value") {
    val failValue = ""

    val violations = validate[NotEmptyExample](failValue)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("stringValue")
    violations.head.getMessage shouldBe ErrorMessage
    violations.head.getInvalidValue shouldBe failValue
    val payload = violations.head.getDynamicPayload(ErrorCode.ValueCannotBeEmpty.getClass)
    payload.isDefined shouldBe true
    payload.get shouldBe ErrorCode.ValueCannotBeEmpty
  }

  test("pass validation for all whitespace value") {
    val whiteSpaceValue = for (n <- Gen.choose(1, 100)) yield Seq.fill(n) { ' ' }

    val passValue = whiteSpaceValue.map(_.mkString)

    forAll(passValue) { value =>
      validate[NotEmptyExample](value).isEmpty shouldBe true
    }
  }

  test("pass validation for valid values in array") {
    val passValue = Gen.nonEmptyContainerOf[Seq, String](Gen.alphaStr)

    forAll(passValue) { value =>
      validate[NotEmptyArrayExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for empty Array") {
    val failValue = Seq.empty
    val violations = validate[NotEmptyArrayExample](failValue)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("stringValue")
    violations.head.getMessage shouldBe ErrorMessage
    violations.head.getInvalidValue shouldBe failValue
    val payload = violations.head.getDynamicPayload(ErrorCode.ValueCannotBeEmpty.getClass)
    payload.isDefined shouldBe true
    payload.get shouldBe ErrorCode.ValueCannotBeEmpty
  }

  test("pass validation for valid values in seq") {
    val passValue = Gen.nonEmptyContainerOf[Seq, String](Gen.alphaStr)

    forAll(passValue) { value =>
      validate[NotEmptySeqExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for empty seq") {
    val failValue = Seq.empty
    val violations = validate[NotEmptySeqExample](failValue)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("stringValue")
    violations.head.getMessage shouldBe ErrorMessage
    violations.head.getInvalidValue shouldBe failValue
    val payload = violations.head.getDynamicPayload(ErrorCode.ValueCannotBeEmpty.getClass)
    payload.isDefined shouldBe true
    payload.get shouldBe ErrorCode.ValueCannotBeEmpty
  }

  test("pass validation for valid values in map") {
    val stringTuplesGen = for {
      n <- Gen.alphaStr
      m <- Gen.alphaStr
    } yield (n, m)

    val passValue = Gen.nonEmptyMap[String, String](stringTuplesGen)
    forAll(passValue) { value =>
      validate[NotEmptyMapExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for empty map") {
    val failValue = Map.empty
    val violations = validate[NotEmptyMapExample](failValue)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("stringValue")
    violations.head.getMessage shouldBe ErrorMessage
    violations.head.getInvalidValue shouldBe failValue
    val payload = violations.head.getDynamicPayload(ErrorCode.ValueCannotBeEmpty.getClass)
    payload.isDefined shouldBe true
    payload.get shouldBe ErrorCode.ValueCannotBeEmpty
  }

  test("fail validation for invalid type") {
    intercept[UnexpectedTypeException] {
      validate[NotEmptyInvalidTypeExample](2)
    }
  }

  private def validate[T: Manifest](value: Any): Set[ConstraintViolation[T]] = {
    super.validate[NotEmpty, T](manifest[T].runtimeClass, "stringValue", value)
  }
}
