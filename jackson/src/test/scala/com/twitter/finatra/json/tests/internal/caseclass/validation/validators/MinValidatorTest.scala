package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.MinValidator
import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.{ErrorCode, Min, ValidationResult, ValidatorTest}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

case class MinIntExample(@Min(1) numberValue: Int)
case class MinLongExample(@Min(1) numberValue: Long)
case class MinDoubleExample(@Min(1) numberValue: Double)
case class MinFloatExample(@Min(1) numberValue: Float)
case class MinBigIntExample(@Min(1) numberValue: BigInt)
case class MinSmallestLongBigIntExample(@Min(Long.MinValue) numberValue: BigInt)
case class MinSecondSmallestLongBigIntExample(@Min(Long.MinValue + 1) numberValue: BigInt)
case class MinLargestLongBigIntExample(@Min(Long.MaxValue) numberValue: BigInt)
case class MinBigDecimalExample(@Min(1) numberValue: BigDecimal)
case class MinSmallestLongBigDecimalExample(@Min(Long.MinValue) numberValue: BigDecimal)
case class MinSecondSmallestLongBigDecimalExample(@Min(Long.MinValue + 1) numberValue: BigDecimal)
case class MinLargestLongBigDecimalExample(@Min(Long.MaxValue) numberValue: BigDecimal)
case class MinSeqExample(@Min(10) numberValue: Seq[Int])
case class MinArrayExample(@Min(10) numberValue: Array[Int])
case class MinInvalidTypeExample(@Min(10) numberValue: String)

class MinValidatorTest extends ValidatorTest with GeneratorDrivenPropertyChecks {

  test("pass validation for int type") {
    val passValue = Gen.choose(1, Int.MaxValue)

    forAll(passValue) { value: Int =>
      validate[MinIntExample](value) should equal(Valid)
    }
  }

  test("failed validation for int type") {
    val failValue = Gen.choose(Int.MinValue, 0)

    forAll(failValue) { value =>
      validate[MinIntExample](value) should equal(
        Invalid(errorMessage(Integer.valueOf(value)), errorCode(Integer.valueOf(value)))
      )
    }
  }

  test("pass validation for long type") {
    val passValue = Gen.choose(1L, Long.MaxValue)

    forAll(passValue) { value =>
      validate[MinLongExample](value) should equal(Valid)
    }
  }

  test("failed validation for long type") {
    val failValue = Gen.choose(Long.MinValue, 0L)

    forAll(failValue) { value =>
      validate[MinLongExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Long.valueOf(value)),
          errorCode(java.lang.Long.valueOf(value))
        )
      )
    }
  }

  test("pass validation for double type") {
    val passValue = Gen.choose(0.1, Double.MaxValue)

    forAll(passValue) { value =>
      validate[MinDoubleExample](value) should equal(Valid)
    }
  }

  test("fail validation for double type") {
    val failValue = Gen.choose(Double.MinValue, 0.0)

    forAll(failValue) { value =>
      validate[MinDoubleExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Double.valueOf(value)),
          errorCode(java.lang.Double.valueOf(value))
        )
      )
    }
  }

  test("pass validation for float type") {
    val passValue = Gen.choose(0.1F, Float.MaxValue)

    forAll(passValue) { value =>
      validate[MinFloatExample](value) should equal(Valid)
    }
  }

  test("fail validation for float type") {
    val failValue = Gen.choose(Float.MinValue, 0.0F)

    forAll(failValue) { value =>
      validate[MinFloatExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Float.valueOf(value)),
          errorCode(java.lang.Float.valueOf(value))
        )
      )
    }
  }

  test("pass validation for big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](1, Long.MaxValue)
    } yield BigInt(long)

    forAll(passBigIntValue) { value =>
      validate[MinBigIntExample](value) should equal(Valid)
    }
  }

  test("pass validation for very small big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue, Long.MaxValue)
    } yield BigInt(long)

    forAll(passBigIntValue) { value =>
      validate[MinSmallestLongBigIntExample](value) should equal(Valid)
    }
  }

  test("pass validation for very large big int type") {
    val passValue = BigInt(Long.MaxValue)
    validate[MinLargestLongBigIntExample](passValue) should equal(Valid)
  }

  test("fail validation for big int type") {
    val failBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue, 0)
    } yield BigInt(long)

    forAll(failBigIntValue) { value =>
      validate[MinBigIntExample](value) should equal(Invalid(errorMessage(value), errorCode(value)))
    }
  }

  test("fail validation for very small big int type") {
    val value = BigInt(Long.MinValue)
    validate[MinSecondSmallestLongBigIntExample](value) should equal(
      Invalid(
        errorMessage(value, minValue = Long.MinValue + 1),
        errorCode(value, minValue = Long.MinValue + 1)
      )
    )
  }

  test("fail validation for very large big int type") {
    val failBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue, Long.MaxValue - 1)
    } yield BigInt(long)

    forAll(failBigIntValue) { value =>
      validate[MinLargestLongBigIntExample](value) should equal(
        Invalid(
          errorMessage(value, minValue = Long.MaxValue),
          errorCode(value, minValue = Long.MaxValue)
        )
      )
    }
  }

  test("pass validation for big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](1.0, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[MinBigDecimalExample](value) should equal(Valid)
    }
  }

  test("pass validation for very small big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[MinSmallestLongBigDecimalExample](value) should equal(Valid)
    }
  }

  test("pass validation for very large big decimal type") {
    val passValue = BigDecimal(Long.MaxValue)
    validate[MinLargestLongBigDecimalExample](passValue) should equal(Valid)
  }

  test("fail validation for big decimal type") {
    val failBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, 0.9)
    } yield BigDecimal(double)

    forAll(failBigDecimalValue) { value =>
      validate[MinBigDecimalExample](value) should equal(
        Invalid(errorMessage(value), errorCode(value))
      )
    }
  }

  test("fail validation for very small big decimal type") {
    val value = BigDecimal(Long.MinValue) + 0.1
    validate[MinSecondSmallestLongBigDecimalExample](value) should equal(
      Invalid(
        errorMessage(value, minValue = Long.MinValue + 1),
        errorCode(value, minValue = Long.MinValue + 1)
      )
    )
  }

  test("fail validation for very large big decimal type") {
    val failBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, Long.MaxValue - 0.1)
    } yield BigDecimal(double)

    forAll(failBigDecimalValue) { value =>
      validate[MinLargestLongBigDecimalExample](value) should equal(
        Invalid(
          errorMessage(value, minValue = Long.MaxValue),
          errorCode(value, minValue = Long.MaxValue)
        )
      )
    }
  }

  test("pass validation for sequence of integers") {
    val passValue = for {
      n <- Gen.containerOfN[Seq, Int](10, Gen.choose(0, 200))
      m <- Gen.containerOf[Seq, Int](Gen.choose(0, 200))
    } yield n ++ m

    forAll(passValue) { value =>
      validate[MinSeqExample](value) should equal(Valid)
    }
  }

  test("fail validation for sequence of integers") {
    val failValue = for {
      size <- Gen.choose(0, 9)
    } yield Seq.fill(size) { 0 }

    forAll(failValue) { value =>
      validate[MinSeqExample](value) should equal(
        Invalid(
          errorMessage(value = Integer.valueOf(value.size), minValue = 10),
          errorCode(value = Integer.valueOf(value.size), minValue = 10)
        )
      )
    }
  }

  test("pass validation for array of integers") {
    val passValue = for {
      n <- Gen.containerOfN[Array, Int](10, Gen.choose(0, 200))
      m <- Gen.containerOf[Array, Int](Gen.choose(0, 200))
    } yield n ++ m

    forAll(passValue) { value =>
      validate[MinArrayExample](value) should equal(Valid)
    }
  }

  test("fail validation for array of integers") {
    val failValue = for {
      size <- Gen.choose(0, 9)
    } yield Array.fill(size) { 0 }

    forAll(failValue) { value =>
      validate[MinArrayExample](value) should equal(
        Invalid(
          errorMessage(value = Integer.valueOf(value.length), minValue = 10),
          errorCode(value = Integer.valueOf(value.length), minValue = 10)
        )
      )
    }
  }

  test("fail for unsupported class type") {
    intercept[IllegalArgumentException] {
      validate[MinInvalidTypeExample]("strings are not supported")
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "numberValue", classOf[Min], value)
  }

  private def errorMessage(value: Number, minValue: Long = 1): String = {
    MinValidator.errorMessage(messageResolver, value, minValue)
  }

  private def errorCode(value: Number, minValue: Long = 1) = {
    ErrorCode.ValueTooSmall(minValue, value)
  }
}
