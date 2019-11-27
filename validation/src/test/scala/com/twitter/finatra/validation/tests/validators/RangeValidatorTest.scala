package com.twitter.finatra.validation.tests.validators

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.{ErrorCode, Range, RangeValidator, ValidationResult, ValidatorTest}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

case class RangeIntExample(@Range(min = 1, max = 50) pointValue: Int)
case class RangeLongExample(@Range(min = 1, max = 50) pointValue: Long)
case class RangeDoubleExample(@Range(min = 1, max = 50) pointValue: Double)
case class RangeFloatExample(@Range(min = 1, max = 50) pointValue: Float)
case class RangeBigDecimalExample(@Range(min = 1, max = 50) pointValue: BigDecimal)
case class RangeBigIntExample(@Range(min = 1, max = 50) pointValue: BigInt)
case class RangeLargestLongBigDecimalExample(
  @Range(min = 1, max = Long.MaxValue) pointValue: BigDecimal
)
case class RangeLargestLongBigIntExample(@Range(min = 1, max = Long.MaxValue) pointValue: BigInt)
case class RangeSecondLargestLongBigDecimalExample(
  @Range(min = 1, max = Long.MaxValue - 1) pointValue: BigDecimal
)
case class RangeSecondLargestLongBigIntExample(
  @Range(min = 1, max = Long.MaxValue - 1) pointValue: BigInt
)
case class RangeSmallestLongBigDecimalExample(
  @Range(min = Long.MinValue, max = 5) pointValue: BigDecimal
)
case class RangeSecondSmallestLongBigDecimalExample(
  @Range(min = Long.MinValue + 1, max = 5) pointValue: BigDecimal
)
case class RangeSmallestLongBigIntExample(@Range(min = Long.MinValue, max = 5) pointValue: BigInt)
case class RangeSecondSmallestLongBigIntExample(
  @Range(min = Long.MinValue + 1, max = 5) pointValue: BigInt
)
case class RangeInvalidTypeExample(@Range(min = 1, max = 5) pointValue: String)

class RangeValidatorTest extends ValidatorTest with GeneratorDrivenPropertyChecks {

  test("pass validation for int type") {
    val passValue = Gen.choose(1, 50)

    forAll(passValue) { value =>
      validate[RangeIntExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for int type") {
    val smallerValue = Gen.choose(Int.MinValue, 0)
    val largerValue = Gen.choose(51, Int.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))

    forAll(failValue) { value =>
      validate[RangeIntExample](value) should equal(invalid(Integer.valueOf(value)))
    }
  }

  test("pass validation for long type") {
    val passValue = Gen.choose(1L, 50L)

    forAll(passValue) { value =>
      validate[RangeLongExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for long type") {
    val smallerValue = Gen.choose(Long.MinValue, 0L)
    val largerValue = Gen.choose(51L, Long.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))

    forAll(failValue) { value =>
      validate[RangeLongExample](value) should equal(invalid(java.lang.Long.valueOf(value)))
    }
  }

  test("pass validation for double type") {
    val passValue = Gen.choose(1.0, 50.0)

    forAll(passValue) { value =>
      validate[RangeDoubleExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for double type") {
    val smallerValue = Gen.choose(Double.MinValue, 0.0)
    val largerValue = Gen.choose(51.0, Double.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))

    forAll(failValue) { value =>
      validate[RangeDoubleExample](value) should equal(invalid(java.lang.Double.valueOf(value)))
    }
  }

  test("pass validation for float type") {
    val passValue = Gen.choose(1.0F, 50.0F)

    forAll(passValue) { value =>
      validate[RangeFloatExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for float type") {
    val smallerValue = Gen.choose(Float.MinValue, 0.0F)
    val largerValue = Gen.choose(51.0F, Float.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))

    forAll(failValue) { value =>
      validate[RangeFloatExample](value) should equal(invalid(java.lang.Float.valueOf(value)))
    }
  }

  test("pass validation for big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](1.0, 50.0)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[RangeBigDecimalExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for big decimal type") {
    val smallerValue = Gen.choose(Float.MinValue, 0.0F)
    val largerValue = Gen.choose(51.0F, Float.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))
    val failBigDecimalValue = for { value <- failValue } yield BigDecimal.decimal(value)

    forAll(failBigDecimalValue) { value =>
      validate[RangeBigDecimalExample](value) should equal(invalid(value))
    }
  }

  test("pass validation for big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      int <- Gen.choose(1, 50)
    } yield BigInt(int)

    forAll(passBigIntValue) { value =>
      validate[RangeBigIntExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for big int type") {
    val smallerValue = Gen.choose(Int.MinValue, 0)
    val largerValue = Gen.choose(51, Int.MaxValue)
    val failValue = Gen.frequency((1, smallerValue), (1, largerValue))
    val failBigIntValue = for { value <- failValue } yield BigInt(value)

    forAll(failBigIntValue) { value =>
      validate[RangeBigIntExample](value) should equal(invalid(value))
    }
  }

  test("pass validation for very large big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](1.0, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[RangeLargestLongBigDecimalExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for very large big decimal type") {
    val value = BigDecimal(Long.MaxValue)
    validate[RangeSecondLargestLongBigDecimalExample](value) should equal(
      invalid(value, maxValue = Long.MaxValue - 1)
    )
  }

  test("pass validation for very large big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      int <- Gen.choose[Long](1, Long.MaxValue)
    } yield BigInt(int)

    forAll(passBigIntValue) { value =>
      validate[RangeLargestLongBigIntExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for very large big int type") {
    val value = BigInt(Long.MaxValue)
    validate[RangeSecondLargestLongBigIntExample](value) should equal(
      invalid(value, maxValue = Long.MaxValue - 1)
    )
  }

  test("pass validation for very small big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      int <- Gen.choose[Long](Long.MinValue, 5)
    } yield BigInt(int)

    forAll(passBigIntValue) { value =>
      validate[RangeSmallestLongBigIntExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for very small big int type") {
    val value = BigInt(Long.MinValue)
    validate[RangeSecondSmallestLongBigIntExample](value) should equal(
      invalid(value, minValue = Long.MinValue + 1, maxValue = 5)
    )
  }

  test("pass validation for very small big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, 5.0)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[RangeSmallestLongBigDecimalExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for a very small big decimal type") {
    val value = BigDecimal(Long.MinValue)
    validate[RangeSecondSmallestLongBigDecimalExample](value) should equal(
      invalid(value, minValue = Long.MinValue + 1, maxValue = 5)
    )
  }

  test("fail for unsupported class type") {
    intercept[IllegalArgumentException] {
      validate[RangeInvalidTypeExample]("strings are not supported")
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "pointValue", classOf[Range], value)
  }

  private def errorMessage(value: Number, minValue: Long, maxValue: Long): String = {

    RangeValidator.errorMessage(messageResolver, value, minValue, maxValue)
  }

  private def errorCode(value: Number, minValue: Long, maxValue: Long) = {
    ErrorCode.ValueOutOfRange(java.lang.Long.valueOf(value.longValue), minValue, maxValue)
  }

  private def invalid(value: Number, minValue: Long = 1, maxValue: Long = 50) = {
    Invalid(errorMessage(value, minValue, maxValue), errorCode(value, minValue, maxValue))
  }
}
