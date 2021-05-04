package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.constraints.Range
import com.twitter.finatra.validation.tests.caseclasses._
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.util.validation.conversions.ConstraintViolationOps.RichConstraintViolation
import jakarta.validation.{
  ConstraintDefinitionException,
  ConstraintViolation,
  UnexpectedTypeException
}
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class RangeConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for int type") {
    val passValue = Gen.choose(1, 50)

    forAll(passValue) { value =>
      validate[RangeIntExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for int type") {
    val smallerValue = Gen.choose(Int.MinValue, 0)
    val largerValue = Gen.choose(51, Int.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))

    forAll(failValue) { value =>
      val violations =
        validate[RangeIntExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("pointValue")
      violations.head.getMessage shouldBe errorMessage(
        Integer.valueOf(value),
        minValue = 1,
        maxValue = 50)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(Integer.valueOf(value), minValue = 1, maxValue = 50)
    }
  }

  test("pass validation for long type") {
    val passValue = Gen.choose(1L, 50L)

    forAll(passValue) { value =>
      validate[RangeLongExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for long type") {
    val smallerValue = Gen.choose(Long.MinValue, 0L)
    val largerValue = Gen.choose(51L, Long.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))

    forAll(failValue) { value =>
      val violations = validate[RangeLongExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("pointValue")
      violations.head.getMessage shouldBe errorMessage(
        java.lang.Long.valueOf(value),
        minValue = 1,
        maxValue = 50)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(java.lang.Long.valueOf(value), minValue = 1, maxValue = 50)
    }
  }

  test("pass validation for double type") {
    val passValue = Gen.choose(1.0, 50.0)

    forAll(passValue) { value =>
      validate[RangeDoubleExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for double type") {
    val smallerValue = Gen.choose(Double.MinValue, 0.0)
    val largerValue = Gen.choose(51.0, Double.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))

    forAll(failValue) { value =>
      val violations =
        validate[RangeDoubleExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("pointValue")
      violations.head.getMessage shouldBe errorMessage(
        java.lang.Double.valueOf(value),
        minValue = 1,
        maxValue = 50)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value, minValue = 1, maxValue = 50)
    }
  }

  test("pass validation for float type") {
    val passValue = Gen.choose(1.0F, 50.0F)

    forAll(passValue) { value =>
      validate[RangeFloatExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for float type") {
    val smallerValue = Gen.choose(Float.MinValue, 0.0F)
    val largerValue = Gen.choose(51.0F, Float.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))

    forAll(failValue) { value =>
      val violations = validate[RangeFloatExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("pointValue")
      violations.head.getMessage shouldBe errorMessage(
        java.lang.Float.valueOf(value),
        minValue = 1,
        maxValue = 50)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(
        java.lang.Double.valueOf(value.toString),
        minValue = 1,
        maxValue = 50)
    }
  }

  test("pass validation for big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](1.0, 50.0)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[RangeBigDecimalExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for big decimal type") {
    val smallerValue = Gen.choose(Float.MinValue, 0.0F)
    val largerValue = Gen.choose(51.0F, Float.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))
    val failBigDecimalValue = for { value <- failValue } yield BigDecimal.decimal(value)

    forAll(failBigDecimalValue) { value =>
      val violations = validate[RangeBigDecimalExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("pointValue")
      violations.head.getMessage shouldBe errorMessage(value, minValue = 1, maxValue = 50)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value.toDouble, minValue = 1, maxValue = 50)
    }
  }

  test("pass validation for big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      int <- Gen.choose(1, 50)
    } yield BigInt(int)

    forAll(passBigIntValue) { value =>
      validate[RangeBigIntExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for big int type") {
    val smallerValue = Gen.choose(Int.MinValue, 0)
    val largerValue = Gen.choose(51, Int.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))
    val failBigIntValue = for { value <- failValue } yield BigInt(value)

    forAll(failBigIntValue) { value =>
      val violations = validate[RangeBigIntExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("pointValue")
      violations.head.getMessage shouldBe errorMessage(value, minValue = 1, maxValue = 50)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value, minValue = 1, maxValue = 50)
    }
  }

  test("pass validation for very large big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](1.0, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[RangeLargestLongBigDecimalExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for very large big decimal type") {
    val value = BigDecimal(Long.MaxValue)
    val violations = validate[RangeSecondLargestLongBigDecimalExample](value)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("pointValue")
    violations.head.getMessage shouldBe errorMessage(
      value,
      minValue = 1,
      maxValue = Long.MaxValue - 1)
    violations.head.getInvalidValue shouldBe value
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
    payload.isDefined shouldBe true
    payload.get shouldBe errorCode(value, minValue = 1, maxValue = Long.MaxValue - 1)
  }

  test("pass validation for very large big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      int <- Gen.choose[Long](1, Long.MaxValue)
    } yield BigInt(int)

    forAll(passBigIntValue) { value =>
      validate[RangeLargestLongBigIntExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for very large big int type") {
    val value = BigInt(Long.MaxValue)
    val violations = validate[RangeSecondLargestLongBigIntExample](value)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("pointValue")
    violations.head.getMessage shouldBe errorMessage(
      value,
      minValue = 1,
      maxValue = Long.MaxValue - 1)
    violations.head.getInvalidValue shouldBe value
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
    payload.isDefined shouldBe true
    payload.get shouldBe errorCode(value, minValue = 1, maxValue = Long.MaxValue - 1)
  }

  test("pass validation for very small big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      int <- Gen.choose[Long](Long.MinValue, 5)
    } yield BigInt(int)

    forAll(passBigIntValue) { value =>
      validate[RangeSmallestLongBigIntExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for very small big int type") {
    val value = BigInt(Long.MinValue)
    val violations = validate[RangeSecondSmallestLongBigIntExample](value)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("pointValue")
    violations.head.getMessage shouldBe errorMessage(
      value,
      minValue = Long.MinValue + 1,
      maxValue = 5)
    violations.head.getInvalidValue shouldBe value
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
    payload.isDefined shouldBe true
    payload.get shouldBe errorCode(value, minValue = Long.MinValue + 1, maxValue = 5)
  }

  test("pass validation for very small big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, 5.0)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[RangeSmallestLongBigDecimalExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for a very small big decimal type") {
    val value = BigDecimal(Long.MinValue)
    val violations = validate[RangeSecondSmallestLongBigDecimalExample](value)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("pointValue")
    violations.head.getMessage shouldBe errorMessage(
      value,
      minValue = Long.MinValue + 1,
      maxValue = 5)
    violations.head.getInvalidValue shouldBe value
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueOutOfRange])
    payload.isDefined shouldBe true
    payload.get shouldBe errorCode(value, minValue = Long.MinValue + 1, maxValue = 5)
  }

  test("fail for unsupported class type") {
    intercept[UnexpectedTypeException] {
      validate[RangeInvalidTypeExample]("strings are not supported")
    }
  }

  test("fail for invalid range") {
    val e = intercept[ConstraintDefinitionException] {
      validate[RangeInvalidRangeExample](3)
    }
    e.getMessage should be("invalid range: 5 > 1")
  }

  private def validate[T: Manifest](value: Any): Set[ConstraintViolation[T]] = {
    super.validate[Range, T](manifest[T].runtimeClass, "pointValue", value)
  }

  private def errorMessage(value: Number, minValue: Long, maxValue: Long): String =
    s"[${value.toString}] is not between $minValue and $maxValue"

  private def errorCode(value: Number, minValue: Long, maxValue: Long): ErrorCode =
    ErrorCode.ValueOutOfRange(value.doubleValue(), minValue, maxValue)
}
