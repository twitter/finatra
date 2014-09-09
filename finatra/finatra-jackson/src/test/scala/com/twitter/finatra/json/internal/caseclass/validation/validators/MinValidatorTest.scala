package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidatorTest
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult.{invalid, valid}

case class MinIntExample(@Min(1) numberValue: Int)
case class MinLongExample(@Min(1) numberValue: Long)
case class MinBigIntExample(@Min(1) numberValue: BigInt)
case class MinSmallestLongBigIntExample(@Min(Long.MinValue) numberValue: BigInt)
case class MinSecondSmallestLongBigIntExample(@Min(Long.MinValue + 1) numberValue: BigInt)
case class MinLargestLongBigIntExample(@Min(Long.MaxValue) numberValue: BigInt)
case class MinBigDecimalExample(@Min(1) numberValue: BigDecimal)
case class MinSmallestLongBigDecimalExample(@Min(Long.MinValue) numberValue: BigDecimal)
case class MinSecondSmallestLongBigDecimalExample(@Min(Long.MinValue + 1) numberValue: BigDecimal)
case class MinLargestLongBigDecimalExample(@Min(Long.MaxValue) numberValue: BigDecimal)
case class MinSeqExample(@Min(1) numberValue: Seq[Int])
case class MinArrayExample(@Min(1) numberValue: Array[Int])
case class MinInvalidTypeExample(@Min(1) numberValue: String)

class MinValidatorTest extends ValidatorTest {

  "min validator" should {

    "pass validation for int type" in {
      val value = 1
      validate[MinIntExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for int type" in {
      val value = 0
      validate[MinIntExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for long type" in {
      val value = 1L
      validate[MinLongExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "fail validation for long type" in {
      val value = 0L
      validate[MinLongExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for big int type" in {
      val value = BigInt(1)
      validate[MinBigIntExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "pass validation for very small big int type" in {
      val value = BigInt(Long.MinValue)
      validate[MinSmallestLongBigIntExample](value) should equal(
        valid(
          errorMessage(value, minValue = Long.MinValue)))
    }

    "pass validation for very large big int type" in {
      val value = BigInt(Long.MaxValue)
      validate[MinLargestLongBigIntExample](value) should equal(
        valid(
          errorMessage(value, minValue = Long.MaxValue)))
    }

    "fail validation for big int type" in {
      val value = BigInt(0)
      validate[MinBigIntExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "fail validation for very small big int type" in {
      val value = BigInt(Long.MinValue)
      validate[MinSecondSmallestLongBigIntExample](value) should equal(
        invalid(
          errorMessage(value, minValue = Long.MinValue + 1)))
    }

    "fail validation for very large big int type" in {
      val value = BigInt(Long.MaxValue) - 1
      validate[MinLargestLongBigIntExample](value) should equal(
        invalid(
          errorMessage(value, minValue = Long.MaxValue)))
    }

    "pass validation for big decimal type" in {
      val value = BigDecimal(1.0)
      validate[MinBigDecimalExample](value) should equal(
        valid(
          errorMessage(value)))
    }

    "pass validation for very small big decimal type" in {
      val value = BigDecimal(Long.MinValue)
      validate[MinSmallestLongBigDecimalExample](value) should equal(
        valid(
          errorMessage(value, minValue = Long.MinValue)))
    }

    "pass validation for very large big decimal type" in {
      val value = BigDecimal(Long.MaxValue)
      validate[MinLargestLongBigDecimalExample](value) should equal(
        valid(
          errorMessage(value, minValue = Long.MaxValue)))
    }

    "fail validation for big decimal type" in {
      val value = BigDecimal(0.9)
      validate[MinBigDecimalExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "fail validation for very small big decimal type" in {
      val value = BigDecimal(Long.MinValue) + 0.1
      validate[MinSecondSmallestLongBigDecimalExample](value) should equal(
        invalid(
          errorMessage(value, minValue = Long.MinValue + 1)))
    }

    "fail validation for very large big decimal type" in {
      val value = BigDecimal(Long.MaxValue) - 0.1
      validate[MinLargestLongBigDecimalExample](value) should equal(
        invalid(
          errorMessage(value, minValue = Long.MaxValue)))
    }

    "pass validation for sequence of integers" in {
      val value = Seq(10)
      validate[MinSeqExample](value) should equal(
        valid(
          errorMessage(value = value.size)))
    }

    "fail validation for sequence of integers" in {
      val value = Seq()
      validate[MinSeqExample](value) should equal(
        invalid(
          errorMessage(value = value.size)))
    }

    "pass validation for array of integers" in {
      val value = Array(10)
      validate[MinArrayExample](value) should equal(
        valid(
          errorMessage(value = value.length)))
    }

    "fail validation for array of integers" in {
      val value = Array()
      validate[MinArrayExample](value) should equal(
        invalid(
          errorMessage(value = value.length)))
    }

    "fail for unsupported class type" in {
      intercept[IllegalArgumentException] {
        validate[MinInvalidTypeExample]("strings are not supported")
      }
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    validate(manifest[C].erasure, "numberValue", classOf[Min], value)
  }

  private def errorMessage(value: Number, minValue: Long = 1): String = {
    MinValidator.errorMessage(
      messageResolver,
      value,
      minValue)
  }
}