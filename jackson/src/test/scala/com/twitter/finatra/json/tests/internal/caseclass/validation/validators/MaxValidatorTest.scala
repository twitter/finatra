package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.MaxValidator
import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation._

case class MaxIntExample(@Max(0) numberValue: Int)
case class MaxDoubleExample(@Max(0) numberValue: Double)
case class MaxFloatExample(@Max(0) numberValue: Float)
case class MaxLongExample(@Max(0) numberValue: Long)
case class MaxBigIntExample(@Max(0) numberValue: BigInt)
case class MaxLargestLongBigIntExample(@Max(Long.MaxValue) numberValue: BigInt)
//case class MaxSecondLargestLongBigIntExample(@Max(Long.MaxValue - 1) numberValue: BigInt)
case class MaxSmallestLongBigIntExample(@Max(Long.MinValue) numberValue: BigInt)
case class MaxBigDecimalExample(@Max(0) numberValue: BigDecimal)
case class MaxLargestLongBigDecimalExample(@Max(Long.MaxValue) numberValue: BigDecimal)
//case class MaxSecondLargestLongBigDecimalExample(@Max(Long.MaxValue - 1) numberValue: BigDecimal)
case class MaxSmallestLongBigDecimalExample(@Max(Long.MinValue) numberValue: BigDecimal)
case class MaxSeqExample(@Max(0) numberValue: Seq[Int])
case class MaxArrayExample(@Max(0) numberValue: Array[Int])
case class MaxInvalidTypeExample(@Max(0) numberValue: String)

class MaxValidatorTest extends ValidatorTest {

  "max validator" should {

    "pass validation for int type" in {
      val value = 0
      validate[MaxIntExample](value) should equal(Valid)
    }

    "fail validation for int type" in {
      val value = 1
      validate[MaxIntExample](value) should equal(
        Invalid(
          errorMessage(Integer.valueOf(value)),
          errorCode(Integer.valueOf(value))))
    }

    "pass validation for long type" in {
      val value = 0L
      validate[MaxLongExample](value) should equal(Valid)
    }

    "fail validation for long type" in {
      val value = 1L
      validate[MaxLongExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Long.valueOf(value)),
          errorCode(java.lang.Long.valueOf(value))))
    }

    "pass validation for double type" in {
      val value = 0.0
      validate[MaxDoubleExample](value) should equal(Valid)
    }

    "fail validation for double type" in {
      val value = 0.5
      validate[MaxDoubleExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Double.valueOf(value)),
          errorCode(java.lang.Double.valueOf(value))))
    }

    "pass validation for float type" in {
      val value = 0.0F
      validate[MaxFloatExample](value) should equal(Valid)
    }

    "fail validation for float type" in {
      val value = 0.5F
      validate[MaxFloatExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Float.valueOf(value)),
          errorCode(java.lang.Float.valueOf(value))))
    }

    "pass validation for big int type" in {
      val value = BigInt(0)
      validate[MaxBigIntExample](value) should equal(Valid)
    }

    "pass validation for very small big int type" in {
      val value = BigInt(Long.MinValue)
      validate[MaxSmallestLongBigIntExample](value) should equal(Valid)
    }

    "pass validation for very large big int type" in {
      val value = BigInt(Long.MaxValue)
      validate[MaxLargestLongBigIntExample](value) should equal(Valid)
    }

    "fail validation for big int type" in {
      val value = BigInt(1)
      validate[MaxBigIntExample](value) should equal(
        Invalid(
          errorMessage(value),
          errorCode(value)))
    }

    "fail validation for very small big int type" in {
      val value = BigInt(Long.MinValue) + 1
      validate[MaxSmallestLongBigIntExample](value) should equal(
        Invalid(
          errorMessage(value, maxValue = Long.MinValue),
          errorCode(value, maxValue = Long.MinValue)))
    }

//    "fail validation for very large big int type" in {
//      val value = BigInt(Long.MaxValue)
//      validate[MaxSecondLargestLongBigIntExample](value) should equal(
//        Invalid(
//          errorMessage(value, maxValue = Long.MaxValue - 1)))
//    }

    "pass validation for big decimal type" in {
      val value = BigDecimal(0)
      validate[MaxBigDecimalExample](value) should equal(Valid)
    }

    "pass validation for very small big decimal type" in {
      val value = BigDecimal(Long.MinValue)
      validate[MaxSmallestLongBigDecimalExample](value) should equal(Valid)
    }

    "pass validation for very large big decimal type" in {
      val value = BigDecimal(Long.MaxValue)
      validate[MaxLargestLongBigDecimalExample](value) should equal(Valid)
    }

    "fail validation for big decimal type" in {
      val value = BigDecimal(0.9)
      validate[MaxBigDecimalExample](value) should equal(
        Invalid(
          errorMessage(value),
          errorCode(value)))
    }

    "fail validation for very small big decimal type" in {
      val value = BigDecimal(Long.MinValue) + 0.1
      validate[MaxSmallestLongBigDecimalExample](value) should equal(
        Invalid(
          errorMessage(value, maxValue = Long.MinValue),
          errorCode(value, maxValue = Long.MinValue)))
    }

//    "fail validation for very large big decimal type" in {
//      val value = BigDecimal(Long.MaxValue) - 0.1
//      validate[MaxSecondLargestLongBigDecimalExample](value) should equal(
//        Invalid(
//          errorMessage(value, maxValue = Long.MaxValue - 1)))
//    }

    "pass validation for sequence of integers" in {
      val value = Seq()
      validate[MaxSeqExample](value) should equal(Valid)
    }

    "fail validation for sequence of integers" in {
      val value = Seq(10)
      validate[MaxSeqExample](value) should equal(
        Invalid(
          errorMessage(value = Integer.valueOf(value.size)),
          errorCode(Integer.valueOf(value.size))))
    }

    "pass validation for array of integers" in {
      val value = Array()
      validate[MaxArrayExample](value) should equal(Valid)
    }

    "fail validation for array of integers" in {
      val value = Array(10)
      validate[MaxArrayExample](value) should equal(
        Invalid(
          errorMessage(value = Integer.valueOf(value.length)),
          errorCode(Integer.valueOf(value.length))))
    }

    "fail for unsupported class type" in {
      intercept[IllegalArgumentException] {
        validate[MaxInvalidTypeExample]("strings are not supported")
      }
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "numberValue", classOf[Max], value)
  }

  private def errorMessage(value: Number, maxValue: Long = 0): String = {
    MaxValidator.errorMessage(
      messageResolver,
      value,
      maxValue)
  }

  private def errorCode(value: Number, maxValue: Long = 0) = {
    ErrorCode.ValueTooLarge(maxValue, value)
  }
}
