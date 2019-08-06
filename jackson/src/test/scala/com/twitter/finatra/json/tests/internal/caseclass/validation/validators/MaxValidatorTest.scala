package com.twitter.finatra.json.tests.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.MaxValidator
import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation._
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

case class MaxIntExample(@Max(0) numberValue: Int)
case class MaxDoubleExample(@Max(0) numberValue: Double)
case class MaxFloatExample(@Max(0) numberValue: Float)
case class MaxLongExample(@Max(0) numberValue: Long)
case class MaxBigIntExample(@Max(0) numberValue: BigInt)
case class MaxLargestLongBigIntExample(@Max(Long.MaxValue) numberValue: BigInt)
case class MaxSecondLargestLongBigIntExample(@Max(Long.MaxValue - 1) numberValue: BigInt)
case class MaxSmallestLongBigIntExample(@Max(Long.MinValue) numberValue: BigInt)
case class MaxBigDecimalExample(@Max(0) numberValue: BigDecimal)
case class MaxLargestLongBigDecimalExample(@Max(Long.MaxValue) numberValue: BigDecimal)
case class MaxSecondLargestLongBigDecimalExample(@Max(Long.MaxValue - 1) numberValue: BigDecimal)
case class MaxSmallestLongBigDecimalExample(@Max(Long.MinValue) numberValue: BigDecimal)
case class MaxSeqExample(@Max(100) numberValue: Seq[Int])
case class MaxArrayExample(@Max(100) numberValue: Array[Int])
case class MaxInvalidTypeExample(@Max(100) numberValue: String)

class MaxValidatorTest extends ValidatorTest with GeneratorDrivenPropertyChecks {

  test("pass validation for int type") {
    val passValue = Gen.choose(Int.MinValue, 0)

    forAll(passValue) { value: Int =>
      validate[MaxIntExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for int type") {
    val failValue = Gen.choose(1, Int.MaxValue)

    forAll(failValue) { value =>
      validate[MaxIntExample](value) should equal(
        Invalid(errorMessage(Integer.valueOf(value)), errorCode(Integer.valueOf(value)))
      )
    }
  }

  test("pass validation for long type") {
    val passValue = Gen.choose(Long.MinValue, 0L)

    forAll(passValue) { value =>
      validate[MaxLongExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("failed validation for long type") {
    val failValue = Gen.choose(1L, Long.MaxValue)

    forAll(failValue) { value =>
      validate[MaxLongExample](value) == Invalid(
        errorMessage(java.lang.Long.valueOf(value)),
        errorCode(java.lang.Long.valueOf(value))
      )
    }
  }

  test("pass validation for double type") {
    val passValue = Gen.choose(Double.MinValue, 0.0)

    forAll(passValue) { value =>
      validate[MaxDoubleExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for double type") {
    val failValue = Gen.choose(0.1, Double.MaxValue)

    forAll(failValue) { value =>
      validate[MaxDoubleExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Double.valueOf(value)),
          errorCode(java.lang.Double.valueOf(value))
        )
      )
    }
  }

  test("pass validation for float type") {
    val passValue = Gen.choose(Float.MinValue, 0.0F)

    forAll(passValue) { value =>
      validate[MaxFloatExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for float type") {
    val failValue = Gen.choose(0.1F, Float.MaxValue)

    forAll(failValue) { value =>
      validate[MaxFloatExample](value) should equal(
        Invalid(
          errorMessage(java.lang.Float.valueOf(value)),
          errorCode(java.lang.Float.valueOf(value))
        )
      )
    }
  }

  test("pass validation for big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue, 0)
    } yield BigInt(long)

    forAll(passBigIntValue) { value =>
      validate[MaxBigIntExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("pass validation for very small big int type") {
    val passValue = BigInt(Long.MinValue)
    validate[MaxSmallestLongBigIntExample](passValue).isInstanceOf[Valid] shouldBe true
  }

  test("pass validation for very large big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue, Long.MaxValue)
    } yield BigInt(long)

    forAll(passBigIntValue) { value =>
      validate[MaxLargestLongBigIntExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for big int type") {
    val failBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](1, Long.MaxValue)
    } yield BigInt(long)

    forAll(failBigIntValue) { value =>
      validate[MaxBigIntExample](value) should equal(Invalid(errorMessage(value), errorCode(value)))
    }
  }

  test("fail validation for very small big int type") {
    val failBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue + 1, Long.MaxValue)
    } yield BigInt(long)

    forAll(failBigIntValue) { value =>
      validate[MaxSmallestLongBigIntExample](value) should equal(
        Invalid(
          errorMessage(value, maxValue = Long.MinValue),
          errorCode(value, maxValue = Long.MinValue)
        )
      )
    }
  }

  test("fail validation for very large big int type") {
    val value = BigInt(Long.MaxValue)
    validate[MaxSecondLargestLongBigIntExample](value) should equal(
      Invalid(
        errorMessage(value, maxValue = Long.MaxValue - 1),
        errorCode(value, maxValue = Long.MaxValue - 1)
      )
    )
  }

  test("pass validation for big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, 0)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[MaxBigDecimalExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("pass validation for very small big decimal type") {
    val passValue = BigDecimal(Long.MinValue)
    validate[MaxSmallestLongBigDecimalExample](passValue).isInstanceOf[Valid] shouldBe true
  }

  test("pass validation for very large big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[MaxLargestLongBigDecimalExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for big decimal type") {
    val failBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](0.1, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(failBigDecimalValue) { value =>
      validate[MaxBigDecimalExample](value) should equal(
        Invalid(errorMessage(value), errorCode(value))
      )
    }
  }

  test("fail validation for very small big decimal type") {
    val failBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue + 0.1, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(failBigDecimalValue) { value =>
      validate[MaxSmallestLongBigDecimalExample](value) should equal(
        Invalid(
          errorMessage(value, maxValue = Long.MinValue),
          errorCode(value, maxValue = Long.MinValue)
        )
      )
    }
  }

  test("fail validation for very large big decimal type") {
    val value = BigDecimal(Long.MaxValue) - 0.1
    validate[MaxSecondLargestLongBigDecimalExample](value) should equal(
      Invalid(
        errorMessage(value, maxValue = Long.MaxValue - 1),
        errorCode(value, maxValue = Long.MaxValue - 1)
      )
    )
  }

  test("pass validation for sequence of integers") {
    val passValue = for {
      size <- Gen.choose(0, 100)
    } yield Seq.fill(size) { 0 }

    forAll(passValue) { value =>
      validate[MaxSeqExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for sequence of integers") {
    val failValue = for {
      n <- Gen.containerOfN[Seq, Int](100, Gen.choose(0, 200))
      m <- Gen.nonEmptyContainerOf[Seq, Int](Gen.choose(0, 200))
    } yield { n ++ m }

    forAll(failValue) { value =>
      validate[MaxSeqExample](value) should equal(
        Invalid(
          errorMessage(value = Integer.valueOf(value.size), maxValue = 100),
          errorCode(value = Integer.valueOf(value.size), maxValue = 100)
        )
      )
    }
  }

  test("pass validation for array of integers") {
    val passValue = for {
      size <- Gen.choose(0, 100)
    } yield {
      Array.fill(size) { 0 }
    }

    forAll(passValue) { value =>
      validate[MaxArrayExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for array of integers") {
    val failValue = for {
      n <- Gen.containerOfN[Array, Int](100, Gen.choose(0, 200))
      m <- Gen.nonEmptyContainerOf[Array, Int](Gen.choose(0, 200))
    } yield { n ++ m }

    forAll(failValue) { value =>
      validate[MaxArrayExample](value) should equal(
        Invalid(
          errorMessage(value = Integer.valueOf(value.length), maxValue = 100),
          errorCode(value = Integer.valueOf(value.length), maxValue = 100)
        )
      )
    }
  }

  test("fail for unsupported class type") {
    intercept[IllegalArgumentException] {
      validate[MaxInvalidTypeExample]("strings are not supported")
    }
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "numberValue", classOf[Max], value)
  }

  private def errorMessage(value: Number, maxValue: Long = 0): String = {
    MaxValidator.errorMessage(messageResolver, value, maxValue)
  }

  private def errorCode(value: Number, maxValue: Long = 0) = {
    ErrorCode.ValueTooLarge(maxValue, value)
  }
}
