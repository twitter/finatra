package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.constraints.UUID
import com.twitter.finatra.validation.tests.caseclasses.UUIDExample
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.util.validation.conversions.ConstraintViolationOps._
import jakarta.validation.ConstraintViolation
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class UUIDConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for valid uuid") {
    val passValue = Gen.uuid

    forAll(passValue) { value =>
      validate[UUIDExample](value.toString).isEmpty shouldBe true
    }
  }

  test("fail validation for valid uuid") {
    val passValue = Gen.alphaStr

    forAll(passValue) { value =>
      val violations = validate[UUIDExample](value)
      violations.size should equal(1)
      val violation = violations.head
      violation.getPropertyPath.toString should equal("uuid")
      violation.getMessage should be(errorMessage(violation.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidUUID])
      payload.isDefined should be(true)
      payload.get should equal(
        ErrorCode.InvalidUUID(violations.head.getInvalidValue.asInstanceOf[String]))
    }
  }

  private def validate[C: Manifest](
    value: Any
  ): Set[ConstraintViolation[C]] = {
    super.validate[UUID, C](manifest[C].runtimeClass, "uuid", value)
  }

  private def errorMessage(value: Any): String = s"[${value}] is not a valid UUID"
}
