package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.finatra.validation.constraints.{CountryCode, CountryCodeConstraintValidator}
import com.twitter.finatra.validation.tests.caseclasses.{
  CountryCodeArrayExample,
  CountryCodeExample,
  CountryCodeInvalidTypeExample,
  CountryCodeSeqExample
}
import com.twitter.util.validation.conversions.ConstraintViolationOps._
import jakarta.validation.ConstraintViolation
import java.util.Locale
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class CountryCodeConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  private val countryCodes = Locale.getISOCountries.toSeq

  test("pass validation for valid country code") {
    countryCodes.foreach { value =>
      validate[CountryCodeExample](value).isEmpty should be(true)
    }
  }

  test("fail validation for invalid country code") {
    forAll(genFakeCountryCode) { value =>
      val violations = validate[CountryCodeExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("countryCode")
      violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidCountryCodes])
      payload.isDefined should be(true)
      payload.get.codes.isEmpty should be(false)
    }
  }

  test("pass validation for valid country codes in seq") {
    val passValue = Gen.containerOf[Seq, String](Gen.oneOf(countryCodes))

    forAll(passValue) { value =>
      validate[CountryCodeSeqExample](value).isEmpty should be(true)
    }
  }

  test("pass validation for empty seq") {
    val emptyValue = Seq.empty
    validate[CountryCodeSeqExample](emptyValue).isEmpty should be(true)
  }

  test("fail validation for invalid country codes in seq") {
    val failValue = Gen.nonEmptyContainerOf[Seq, String](genFakeCountryCode)

    forAll(failValue) { value =>
      val violations = validate[CountryCodeSeqExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("countryCode")
      violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidCountryCodes])
      payload.isDefined should be(true)
      payload.get.codes.isEmpty should be(false)
    }
  }

  test("pass validation for valid country codes in array") {
    val passValue = Gen.containerOf[Array, String](Gen.oneOf(countryCodes))

    forAll(passValue) { value =>
      validate[CountryCodeArrayExample](value).isEmpty should be(true)
    }
  }

  test("fail validation for invalid country codes in array") {
    val failValue = Gen.nonEmptyContainerOf[Array, String](genFakeCountryCode)

    forAll(failValue) { value =>
      val violations = validate[CountryCodeArrayExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("countryCode")
      violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidCountryCodes])
      payload.isDefined should be(true)
      payload.get.codes.isEmpty should be(false)
    }
  }

  test("fail validation for invalid country code type") {
    val failValue = Gen.choose[Int](0, 100)

    forAll(failValue) { value =>
      val violations = validate[CountryCodeInvalidTypeExample](value)
      violations.size should equal(1)
      violations.head.getPropertyPath.toString should equal("countryCode")
      violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
      val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidCountryCodes])
      payload.isDefined should be(true)
      payload.get.codes.isEmpty should be(false)
    }
  }

  //generate random uppercase string for fake country code
  private def genFakeCountryCode: Gen[String] = {
    Gen
      .nonEmptyContainerOf[Seq, Char](Gen.alphaUpperChar)
      .map(_.mkString)
      .filter(_.nonEmpty)
      .filter(!countryCodes.contains(_))
  }

  private def validate[T: Manifest](value: Any): Set[ConstraintViolation[T]] = {
    super.validate[CountryCode, T](manifest[T].runtimeClass, "countryCode", value)
  }

  private def errorMessage(value: Any): String = {
    s"[${CountryCodeConstraintValidator.toErrorValue(value)}] is not a valid country code"
  }
}
