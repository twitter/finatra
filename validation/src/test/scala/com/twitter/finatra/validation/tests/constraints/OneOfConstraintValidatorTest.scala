package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.finatra.validation.constraints.{OneOf, OneOfConstraintValidator}
import com.twitter.finatra.validation.constraints.OneOfConstraintValidator.toCommaSeparatedValue
import com.twitter.finatra.validation.tests.caseclasses.{
  OneOfExample,
  OneOfInvalidTypeExample,
  OneOfSeqExample
}
import com.twitter.util.validation.conversions.ConstraintViolationOps._
import jakarta.validation.ConstraintViolation
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class OneOfConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  val oneOfValues: Set[String] = Set("a", "B", "c")

  test("pass validation for single value") {
    oneOfValues.foreach { value =>
      validate[OneOfExample](value).isEmpty shouldBe true
    }
  }

  test("fail validation for single value") {
    val failValue = Gen.alphaStr.filter(!oneOfValues.contains(_))

    forAll(failValue) { value =>
      val violations = validate[OneOfExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("enumValue")
      violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidValues])
      payload.isDefined should be(true)
      payload.get.invalid.isEmpty should be(false)
      payload.get.valid should equal(
        violations.head.getConstraintDescriptor.getAnnotation.asInstanceOf[OneOf].value().toSet)
    }
  }

  test("pass validation for seq") {
    val passValue = Gen.nonEmptyContainerOf[Seq, String](Gen.oneOf(oneOfValues.toSeq))

    forAll(passValue) { value =>
      validate[OneOfSeqExample](value).isEmpty shouldBe true
    }
  }

  test("pass validation for empty seq") {
    val emptySeq = Seq.empty
    validate[OneOfSeqExample](emptySeq).isEmpty shouldBe true
  }

  test("fail validation for invalid value in seq") {
    val failValue =
      Gen.nonEmptyContainerOf[Seq, String](Gen.alphaStr.filter(!oneOfValues.contains(_)))

    forAll(failValue) { value =>
      val violations = validate[OneOfSeqExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("enumValue")
      violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidValues])
      payload.isDefined should be(true)
      payload.get.invalid.isEmpty should be(false)
      payload.get.valid should equal(
        violations.head.getConstraintDescriptor.getAnnotation.asInstanceOf[OneOf].value().toSet)
    }
  }

  test("fail validation for invalid type") {
    val failValue = Gen.choose(0, 100)

    forAll(failValue) { value =>
      val violations = validate[OneOfInvalidTypeExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("enumValue")
      violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidValues])
      payload.isDefined should be(true)
      payload.get.invalid.isEmpty should be(false)
      payload.get.valid should equal(
        violations.head.getConstraintDescriptor.getAnnotation.asInstanceOf[OneOf].value().toSet)
    }
  }

  private def validate[T: Manifest](value: Any): Set[ConstraintViolation[T]] = {
    super.validate[OneOf, T](manifest[T].runtimeClass, "enumValue", value)
  }

  private def errorMessage(value: Any): String = {
    s"[${toCommaSeparatedValue(value)}] is not one of [${toCommaSeparatedValue(oneOfValues)}]"
  }
}
