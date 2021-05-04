package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.constraints.Min
import com.twitter.finatra.validation.tests.caseclasses._
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.util.validation.conversions.ConstraintViolationOps.RichConstraintViolation
import jakarta.validation.{ConstraintViolation, UnexpectedTypeException}
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class MinConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for int type") {
    val passValue = Gen.choose(1, Int.MaxValue)

    forAll(passValue) { value: Int =>
      validate[MinIntExample](value).isEmpty shouldBe true
    }
  }

  test("failed validation for int type") {
    val failValue = Gen.choose(Int.MinValue, 0)

    forAll(failValue) { value =>
      val violations = validate[MinIntExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(Integer.valueOf(value))
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(Integer.valueOf(value))
    }
  }

  test("pass validation for long type") {
    val passValue = Gen.choose(1L, Long.MaxValue)

    forAll(passValue) { value =>
      validate[MinLongExample](value).isEmpty shouldBe true
    }
  }

  test("failed validation for long type") {
    val failValue = Gen.choose(Long.MinValue, 0L)

    forAll(failValue) { value =>
      val violations = validate[MinLongExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(java.lang.Long.valueOf(value))
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(java.lang.Long.valueOf(value))
    }
  }

  test("pass validation for double type") {
    val passValue = Gen.choose(0.1, Double.MaxValue)

    forAll(passValue) { value =>
      validate[MinDoubleExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for double type") {
    val failValue = Gen.choose(Double.MinValue, 0.0)

    forAll(failValue) { value =>
      val violations = validate[MinDoubleExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(java.lang.Double.valueOf(value))
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value.toLong)
    }
  }

  test("pass validation for float type") {
    val passValue = Gen.choose(0.1F, Float.MaxValue)

    forAll(passValue) { value =>
      validate[MinFloatExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for float type") {
    val failValue = Gen.choose(Float.MinValue, 0.0F)

    forAll(failValue) { value =>
      val violations = validate[MinFloatExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(java.lang.Float.valueOf(value))
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value.toLong)
    }
  }

  test("pass validation for big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](1, Long.MaxValue)
    } yield BigInt(long)

    forAll(passBigIntValue) { value =>
      validate[MinBigIntExample](value).isEmpty shouldBe true
    }
  }

  test("pass validation for very small big int type") {
    val passBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue, Long.MaxValue)
    } yield BigInt(long)

    forAll(passBigIntValue) { value =>
      validate[MinSmallestLongBigIntExample](value).isEmpty shouldBe true
    }
  }

  test("pass validation for very large big int type") {
    val passValue = BigInt(Long.MaxValue)
    validate[MinLargestLongBigIntExample](passValue).isEmpty shouldBe true
  }

  test("fail validation for big int type") {
    val failBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue, 0)
    } yield BigInt(long)

    forAll(failBigIntValue) { value =>
      val violations = validate[MinBigIntExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(value)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value)
    }
  }

  test("fail validation for very small big int type") {
    val value = BigInt(Long.MinValue)
    val violations = validate[MinSecondSmallestLongBigIntExample](value)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("numberValue")
    violations.head.getMessage shouldBe errorMessage(value, minValue = Long.MinValue + 1)
    violations.head.getInvalidValue shouldBe value
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
    payload.isDefined shouldBe true
    payload.get shouldBe errorCode(value, minValue = Long.MinValue + 1)
  }

  test("fail validation for very large big int type") {
    val failBigIntValue: Gen[BigInt] = for {
      long <- Gen.choose[Long](Long.MinValue, Long.MaxValue - 1)
    } yield BigInt(long)

    forAll(failBigIntValue) { value =>
      val violations = validate[MinLargestLongBigIntExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(value, minValue = Long.MaxValue)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value, minValue = Long.MaxValue)
    }
  }

  test("pass validation for big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](1.0, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[MinBigDecimalExample](value).isEmpty shouldBe true
    }
  }

  test("pass validation for very small big decimal type") {
    val passBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, Long.MaxValue)
    } yield BigDecimal(double)

    forAll(passBigDecimalValue) { value =>
      validate[MinSmallestLongBigDecimalExample](value).isEmpty shouldBe true
    }
  }

  test("pass validation for very large big decimal type") {
    val passValue = BigDecimal(Long.MaxValue)
    validate[MinLargestLongBigDecimalExample](passValue).isEmpty shouldBe true
  }

  test("fail validation for big decimal type") {
    val failBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, 0.9)
    } yield BigDecimal(double)

    forAll(failBigDecimalValue) { value =>
      val violations = validate[MinBigDecimalExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(value)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value)
    }
  }

  test("fail validation for very small big decimal type") {
    val value = BigDecimal(Long.MinValue) + 0.1
    val violations = validate[MinSecondSmallestLongBigDecimalExample](value)
    violations.size should equal(1)
    violations.head.getPropertyPath.toString should equal("numberValue")
    violations.head.getMessage shouldBe errorMessage(value, minValue = Long.MinValue + 1)
    violations.head.getInvalidValue shouldBe value
    val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
    payload.isDefined shouldBe true
    payload.get shouldBe errorCode(value.toLong, minValue = Long.MinValue + 1)
  }

  test("fail validation for very large big decimal type") {
    val failBigDecimalValue: Gen[BigDecimal] = for {
      double <- Gen.choose[Double](Long.MinValue, Long.MaxValue - 0.1)
    } yield BigDecimal(double)

    forAll(failBigDecimalValue) { value =>
      val violations = validate[MinLargestLongBigDecimalExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(value, minValue = Long.MaxValue)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value.toLong, minValue = Long.MaxValue)
    }
  }

  test("pass validation for sequence of integers") {
    val passValue = for {
      n <- Gen.containerOfN[Seq, Int](10, Gen.choose(0, 200))
      m <- Gen.containerOf[Seq, Int](Gen.choose(0, 200))
    } yield n ++ m

    forAll(passValue) { value =>
      validate[MinSeqExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for sequence of integers") {
    val failValue = for {
      size <- Gen.choose(0, 9)
    } yield Seq.fill(size) { 0 }

    forAll(failValue) { value =>
      val violations = validate[MinSeqExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(
        value = Integer.valueOf(value.size),
        minValue = 10)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value = Integer.valueOf(value.size), minValue = 10)
    }
  }

  test("pass validation for map of integers") {
    val mapGenerator = for {
      n <- Gen.alphaStr
      m <- Gen.choose(10, 1000)
    } yield (n, m)

    val passValue = Gen.mapOfN[String, Int](100, mapGenerator).suchThat(_.size >= 10)
    forAll(passValue) { value =>
      validate[MinMapExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for map of integers") {
    val mapGenerator = for {
      n <- Gen.alphaStr
      m <- Gen.choose(10, 1000)
    } yield (n, m)

    val failValue = Gen.mapOfN[String, Int](9, mapGenerator)
    forAll(failValue) { value =>
      val violations = validate[MinMapExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(
        value = Integer.valueOf(value.size),
        minValue = 10)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value = Integer.valueOf(value.size), minValue = 10)
    }
  }

  test("pass validation for array of integers") {
    val passValue = for {
      n <- Gen.containerOfN[Array, Int](10, Gen.choose(0, 200))
      m <- Gen.containerOf[Array, Int](Gen.choose(0, 200))
    } yield n ++ m

    forAll(passValue) { value =>
      validate[MinArrayExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for array of integers") {
    val failValue = for {
      size <- Gen.choose(0, 9)
    } yield Array.fill(size) { 0 }

    forAll(failValue) { value =>
      val violations = validate[MinArrayExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("numberValue")
      violations.head.getMessage shouldBe errorMessage(
        value = Integer.valueOf(value.length),
        minValue = 10)
      violations.head.getInvalidValue shouldBe value
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.ValueTooSmall])
      payload.isDefined shouldBe true
      payload.get shouldBe errorCode(value = Integer.valueOf(value.length), minValue = 10)
    }
  }

  test("fail for unsupported class type") {
    intercept[UnexpectedTypeException] {
      validate[MinInvalidTypeExample]("strings are not supported")
    }
  }

  private def validate[T: Manifest](value: Any): Set[ConstraintViolation[T]] = {
    super.validate[Min, T](manifest[T].runtimeClass, "numberValue", value)
  }

  private def errorMessage(value: Number, minValue: Long = 1): String =
    s"[$value] is not greater than or equal to $minValue"

  private def errorCode(value: Number, minValue: Long = 1): ErrorCode =
    ErrorCode.ValueTooSmall(minValue, value)
}
