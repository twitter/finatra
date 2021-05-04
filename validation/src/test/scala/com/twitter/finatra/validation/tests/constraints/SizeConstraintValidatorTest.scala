package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.constraints.Size
import com.twitter.finatra.validation.tests.caseclasses.{
  SizeArrayExample,
  SizeInvalidTypeExample,
  SizeMapExample,
  SizeSeqExample,
  SizeStringExample
}
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.util.validation.conversions.ConstraintViolationOps.RichConstraintViolation
import jakarta.validation.{ConstraintViolation, UnexpectedTypeException}
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class SizeConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for array type") {
    val passValue = for {
      size <- Gen.choose(10, 50)
    } yield Array.fill(size) { 0 }

    forAll(passValue) { value =>
      validate[SizeArrayExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for too few array type") {
    val failValue = for {
      size <- Gen.choose(0, 9)
    } yield Array.fill(size) { 0 }

    forAll(failValue) { value =>
      val violations = validate[SizeArrayExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("sizeValue")
      violations.head.getMessage shouldBe errorMessage(value.length)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.SizeOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe ErrorCode.SizeOutOfRange(value.length, 10, 50)
    }
  }

  test("fail validation for too many array type") {
    val failValue = for {
      size <- Gen.choose(51, 100)
    } yield Array.fill(size) { 0 }

    forAll(failValue) { value =>
      val violations = validate[SizeArrayExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("sizeValue")
      violations.head.getMessage shouldBe errorMessage(value.length)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.SizeOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe ErrorCode.SizeOutOfRange(value.length, 10, 50)
    }
  }

  test("pass validation for seq type") {
    val passValue = for {
      size <- Gen.choose(10, 50)
    } yield Seq.fill(size) { 0 }

    forAll(passValue) { value =>
      validate[SizeSeqExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for too few seq type") {
    val failValue = for {
      size <- Gen.choose(0, 9)
    } yield Seq.fill(size) { 0 }

    forAll(failValue) { value =>
      val violations = validate[SizeSeqExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("sizeValue")
      violations.head.getMessage shouldBe errorMessage(value.size)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.SizeOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe ErrorCode.SizeOutOfRange(value.size, 10, 50)
    }
  }

  test("fail validation for too many seq type") {
    val failValue = for {
      size <- Gen.choose(51, 100)
    } yield Seq.fill(size) { 0 }

    forAll(failValue) { value =>
      val violations = validate[SizeSeqExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("sizeValue")
      violations.head.getMessage shouldBe errorMessage(value.size)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.SizeOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe ErrorCode.SizeOutOfRange(value.size, 10, 50)
    }
  }

  test("pass validation for map type") {
    val mapGenerator = for {
      n <- Gen.alphaStr
      m <- Gen.choose(10, 1000)
    } yield (n, m)

    val passValue = Gen.mapOfN[String, Int](50, mapGenerator).suchThat(_.size >= 10)
    forAll(passValue) { value =>
      validate[SizeMapExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for too few map type") {
    val mapGenerator = for {
      n <- Gen.alphaStr
      m <- Gen.choose(10, 1000)
    } yield (n, m)

    val failValue = Gen.mapOfN[String, Int](9, mapGenerator).suchThat(_.size <= 9)
    forAll(failValue) { value =>
      val violations = validate[SizeMapExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("sizeValue")
      violations.head.getMessage shouldBe errorMessage(value.size)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.SizeOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe ErrorCode.SizeOutOfRange(value.size, 10, 50)
    }
  }

  test("fail validation for too many map type") {
    val mapGenerator = for {
      n <- Gen.alphaStr
      m <- Gen.choose(10, 1000)
    } yield (n, m)

    val failValue = Gen.mapOfN[String, Int](200, mapGenerator).suchThat(_.size >= 51)
    forAll(failValue) { value =>
      val violations = validate[SizeMapExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("sizeValue")
      violations.head.getMessage shouldBe errorMessage(value.size)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.SizeOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe ErrorCode.SizeOutOfRange(value.size, 10, 50)
    }
  }

  test("pass validation for string type") {
    val passValue = for {
      size <- Gen.choose(10, 140)
    } yield List.fill(size) { 'a' }.mkString

    forAll(passValue) { value =>
      validate[SizeStringExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for too few string type") {
    val failValue = for {
      size <- Gen.choose(0, 9)
    } yield List.fill(size) { 'a' }.mkString

    forAll(failValue) { value =>
      val violations = validate[SizeStringExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("sizeValue")
      violations.head.getMessage shouldBe errorMessage(value.length, maxValue = 140)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.SizeOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe ErrorCode.SizeOutOfRange(value.length, 10, 140)
    }
  }

  test("fail validation for too many string type") {
    val failValue = (for {
      size <- Gen.choose(150, 200)
    } yield List.fill(size) { 'a' }.mkString)

    forAll(failValue) { value =>
      val violations = validate[SizeStringExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("sizeValue")
      violations.head.getMessage shouldBe errorMessage(value.length, maxValue = 140)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.SizeOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe ErrorCode.SizeOutOfRange(value.length, 10, 140)
    }
  }

  test("fail for unsupported class type") {
    intercept[UnexpectedTypeException] {
      validate[SizeInvalidTypeExample](2)
    }
  }

  private def validate[T: Manifest](value: Any): Set[ConstraintViolation[T]] = {
    super.validate[Size, T](manifest[T].runtimeClass, "sizeValue", value)
  }

  private def errorMessage(value: Any, minValue: Long = 10, maxValue: Long = 50): String =
    s"size [${value.toString}] is not between $minValue and $maxValue"
}
