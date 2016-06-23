package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.MinValidator
import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.{ErrorCode, Min, ValidationResult, ValidatorTest}

case class MinIntExample(@Min(1) numberValue: Int)
case class MinLongExample(@Min(1) numberValue: Long)
case class MinDoubleExample(@Min(1) numberValue: Double)
case class MinFloatExample(@Min(1) numberValue: Float)
case class MinBigIntExample(@Min(1) numberValue: BigInt)
case class MinSmallestLongBigIntExample(@Min(Long.MinValue) numberValue: BigInt)
//case class MinSecondSmallestLongBigIntExample(@Min(Long.MinValue + 1) numberValue: BigInt)
case class MinLargestLongBigIntExample(@Min(Long.MaxValue) numberValue: BigInt)
case class MinBigDecimalExample(@Min(1) numberValue: BigDecimal)
case class MinSmallestLongBigDecimalExample(@Min(Long.MinValue) numberValue: BigDecimal)
//case class MinSecondSmallestLongBigDecimalExample(@Min(Long.MinValue + 1) numberValue: BigDecimal)
case class MinLargestLongBigDecimalExample(@Min(Long.MaxValue) numberValue: BigDecimal)
case class MinSeqExample(@Min(1) numberValue: Seq[Int])
case class MinArrayExample(@Min(1) numberValue: Array[Int])
case class MinInvalidTypeExample(@Min(1) numberValue: String)

class MinValidatorTest extends ValidatorTest {

  "min validator" should {

    "pass validation for int type" in {
      val value = 1
      validate[MinIntExample](value) should equal(Valid)
    }

    "fail validation for int type" in {
      val value = 0
      validate[MinIntExample](value) should equal(
        Invalid(
          errorMessage(Integer.valueOf(value)),
          errorCode(Integer.valueOf(value))))
    }

    "pass validation for long type" in {
      val value = 1L
      validate[MinLongExample](value) should equal(Valid)
    }

    "fail validation for long type" in {
      val value = 0L
      validate[MinLongExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Long.valueOf(value)),
          errorCode(java.lang.Long.valueOf(value))))
    }

    "pass validation for double type" in {
      val value = 1.0
      validate[MinDoubleExample](value) should equal(Valid)
    }

    "fail validation for double type" in {
      val value = 0.5
      validate[MinDoubleExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Double.valueOf(value)),
          errorCode(java.lang.Double.valueOf(value))))
    }

    "pass validation for float type" in {
      val value = 1.0F
      validate[MinFloatExample](value) should equal(Valid)
    }

    "fail validation for float type" in {
      val value = 0.5F
      validate[MinFloatExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Float.valueOf(value)),
          errorCode(java.lang.Float.valueOf(value))))
    }

    "pass validation for big int type" in {
      val value = BigInt(1)
      validate[MinBigIntExample](value) should equal(Valid)
    }

    "pass validation for very small big int type" in {
      val value = BigInt(Long.MinValue)
      validate[MinSmallestLongBigIntExample](value) should equal(Valid)
    }

    "pass validation for very large big int type" in {
      val value = BigInt(Long.MaxValue)
      validate[MinLargestLongBigIntExample](value) should equal(Valid)
    }

    "fail validation for big int type" in {
      val value = BigInt(0)
      validate[MinBigIntExample](value) should equal(
        Invalid(
          errorMessage(value),
          errorCode(value)))
    }

//    "fail validation for very small big int type" in {
//      val value = BigInt(Long.MinValue)
//      validate[MinSecondSmallestLongBigIntExample](value) should equal(
//        Invalid(
//          errorMessage(value, minValue = Long.MinValue + 1)))
//    }

    "fail validation for very large big int type" in {
      val value = BigInt(Long.MaxValue) - 1
      validate[MinLargestLongBigIntExample](value) should equal(
        Invalid(
          errorMessage(value, minValue = Long.MaxValue),
          errorCode(value, minValue = Long.MaxValue)))
    }

    "pass validation for big decimal type" in {
      val value = BigDecimal(1.0)
      validate[MinBigDecimalExample](value) should equal(Valid)
    }

    "pass validation for very small big decimal type" in {
      val value = BigDecimal(Long.MinValue)
      validate[MinSmallestLongBigDecimalExample](value) should equal(Valid)
    }

    "pass validation for very large big decimal type" in {
      val value = BigDecimal(Long.MaxValue)
      validate[MinLargestLongBigDecimalExample](value) should equal(Valid)
    }

    "fail validation for big decimal type" in {
      val value = BigDecimal(0.9)
      validate[MinBigDecimalExample](value) should equal(
        Invalid(
          errorMessage(value),
          errorCode(value)))
    }

//    "fail validation for very small big decimal type" in {
//      val value = BigDecimal(Long.MinValue) + 0.1
//      validate[MinSecondSmallestLongBigDecimalExample](value) should equal(
//        Invalid(
//          errorMessage(value, minValue = Long.MinValue + 1)))
//    }

    "fail validation for very large big decimal type" in {
      val value = BigDecimal(Long.MaxValue) - 0.1
      validate[MinLargestLongBigDecimalExample](value) should equal(
        Invalid(
          errorMessage(value, minValue = Long.MaxValue),
          errorCode(value, minValue = Long.MaxValue)))
    }

    "pass validation for sequence of integers" in {
      val value = Seq(10)
      validate[MinSeqExample](value) should equal(Valid)
    }

    "fail validation for sequence of integers" in {
      val value = Seq()
      validate[MinSeqExample](value) should equal(
        Invalid(
          errorMessage(value = Integer.valueOf(value.size)),
          errorCode(value = Integer.valueOf(value.size))))
    }

    "pass validation for array of integers" in {
      val value = Array(10)
      validate[MinArrayExample](value) should equal(Valid)
    }

    "fail validation for array of integers" in {
      val value = Array()
      validate[MinArrayExample](value) should equal(
        Invalid(
          errorMessage(value = Integer.valueOf(value.length)),
          errorCode(value = Integer.valueOf(value.length))))
    }

    "fail for unsupported class type" in {
      intercept[IllegalArgumentException] {
        validate[MinInvalidTypeExample]("strings are not supported")
      }
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "numberValue", classOf[Min], value)
  }

  private def errorMessage(value: Number, minValue: Long = 1): String = {
    MinValidator.errorMessage(
      messageResolver,
      value,
      minValue)
  }
  
  private def errorCode(value: Number, minValue: Long = 1) = {
    ErrorCode.ValueTooSmall(minValue, value)
  }
}
