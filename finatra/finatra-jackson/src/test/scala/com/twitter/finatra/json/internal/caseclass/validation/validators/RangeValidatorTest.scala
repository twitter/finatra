package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidatorTest
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult.{invalid, valid}

case class RangeIntExample(@Range(min = 1, max = 5) pointValue: Int)
case class RangeLongExample(@Range(min = 1, max = 5) pointValue: Long)
case class RangeBigDecimalExample(@Range(min = 1, max = 5) pointValue: BigDecimal)
case class RangeBigIntExample(@Range(min = 1, max = 5) pointValue: BigInt)
case class RangeLargestLongBigDecimalExample(@Range(min = 1, max = Long.MaxValue) pointValue: BigDecimal)
case class RangeLargestLongBigIntExample(@Range(min = 1, max = Long.MaxValue) pointValue: BigInt)
case class RangeSecondLargestLongBigDecimalExample(@Range(min = 1, max = Long.MaxValue - 1) pointValue: BigDecimal)
case class RangeSecondLargestLongBigIntExample(@Range(min = 1, max = Long.MaxValue - 1) pointValue: BigInt)
case class RangeSmallestLongBigDecimalExample(@Range(min = Long.MinValue, max = 5) pointValue: BigDecimal)
case class RangeSecondSmallestLongBigDecimalExample(@Range(min = Long.MinValue + 1, max = 5) pointValue: BigDecimal)
case class RangeSmallestLongBigIntExample(@Range(min = Long.MinValue, max = 5) pointValue: BigInt)
case class RangeSecondSmallestLongBigIntExample(@Range(min = Long.MinValue + 1, max = 5) pointValue: BigInt)
case class RangeInvalidTypeExample(@Range(min = 1, max = 5) pointValue: String)


class RangeValidatorTest extends ValidatorTest {

  "range validator" should {

    "pass validation for int type" in {
      val value = 1
      validate[RangeIntExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for int type" in {
      val value = 0
      validate[RangeIntExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for long type" in {
      val value = 1L
      validate[RangeLongExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for long type" in {
      val value = 0L
      validate[RangeLongExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for big decimal type" in {
      val value = BigDecimal(1.0)
      validate[RangeBigDecimalExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for big decimal type" in {
      val value = BigDecimal(0.9)
      validate[RangeBigDecimalExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for big int type" in {
      val value = BigInt(1)
      validate[RangeBigIntExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for big int type" in {
      val value = BigInt(0)
      validate[RangeBigIntExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for very large big decimal type" in {
      val value = BigDecimal(Long.MaxValue)
      validate[RangeLargestLongBigDecimalExample](value) should equal(
        valid(
          errorMessage(value, maxValue = Long.MaxValue)))
    }

    "fail validation for very large big decimal type" in {
      val value = BigDecimal(Long.MaxValue)
      validate[RangeSecondLargestLongBigDecimalExample](value) should equal(
        invalid(
          errorMessage(value, maxValue = Long.MaxValue - 1)))
    }

    "pass validation for very large big int type" in {
      val value = BigInt(Long.MaxValue)
      validate[RangeLargestLongBigIntExample](value) should equal(
        valid(
          errorMessage(value, maxValue = Long.MaxValue)))
    }

    "fail validation for very large big int type" in {
      val value = BigInt(Long.MaxValue)
      validate[RangeSecondLargestLongBigIntExample](value) should equal(
        invalid(
          errorMessage(value, maxValue = Long.MaxValue - 1)))
    }

    "pass validation for very small big int type" in {
      val value = BigInt(Long.MinValue)
      validate[RangeSmallestLongBigIntExample](value) should equal(
        valid(
          errorMessage(value, minValue = Long.MinValue)))
    }

    "fail validation for very small big int type" in {
      val value = BigInt(Long.MinValue)
      validate[RangeSecondSmallestLongBigIntExample](value) should equal(
        invalid(
          errorMessage(value, minValue = Long.MinValue + 1)))
    }

    "pass validation for very small big decimal type" in {
      val value = BigDecimal(Long.MinValue)
      validate[RangeSmallestLongBigDecimalExample](value) should equal(
        valid(
          errorMessage(value, minValue = Long.MinValue)))
    }
    
    "fail validation for a very small big decimal type" in {
      val value = BigDecimal(Long.MinValue)
      validate[RangeSecondSmallestLongBigDecimalExample](value) should equal(
        invalid(
          errorMessage(value, minValue = Long.MinValue + 1)))
    }

    "fail for unsupported class type" in {
      intercept[IllegalArgumentException] {
        validate[RangeInvalidTypeExample]("strings are not supported")
      }
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    validate(manifest[C].erasure, "pointValue", classOf[Range], value)
  }

  private def errorMessage(
    value: Number,
    minValue: Long = 1,
    maxValue: Long = 5): String = {

    RangeValidator.errorMessage(
      messageResolver,
      value,
      minValue,
      maxValue)
  }
}
