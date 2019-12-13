package com.twitter.finatra.validation.tests.validators

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.{CountryCode, CountryCodeValidator, ErrorCode, ValidationResult, ValidatorTest}
import java.util.Locale
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

case class CountryCodeExample(@CountryCode countryCode: String)
case class CountryCodeSeqExample(@CountryCode countryCode: Seq[String])
case class CountryCodeArrayExample(@CountryCode countryCode: Array[String])
case class CountryCodeInvalidTypeExample(@CountryCode countryCode: Long)

class CountryCodeValidatorTest extends ValidatorTest with ScalaCheckDrivenPropertyChecks {

  private val countryCodes = Locale.getISOCountries.toSeq

  test("pass validation for valid country code") {
    countryCodes.foreach { value =>
      validate[CountryCodeExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for invalid country code") {
    forAll(genFakeCountryCode) { value =>
      validate[CountryCodeExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidCountryCodes(Set(value)))
      )
    }
  }

  test("pass validation for valid country codes in seq") {
    val passValue = Gen.containerOf[Seq, String](Gen.oneOf(countryCodes))

    forAll(passValue) { value =>
      validate[CountryCodeSeqExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("pass validation for empty seq") {
    val emptyValue = Seq.empty
    validate[CountryCodeSeqExample](emptyValue).isInstanceOf[Valid] shouldBe true
  }

  test("fail validation for invalid country codes in seq") {
    val failValue = Gen.nonEmptyContainerOf[Seq, String](genFakeCountryCode)

    forAll(failValue) { value =>
      validate[CountryCodeSeqExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidCountryCodes(value.toSet))
      )
    }
  }

  test("pass validation for valid country codes in array") {
    val passValue = Gen.containerOf[Array, String](Gen.oneOf(countryCodes))

    forAll(passValue) { value =>
      validate[CountryCodeArrayExample](value).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for invalid country codes in array") {
    val failValue = Gen.nonEmptyContainerOf[Array, String](genFakeCountryCode)

    forAll(failValue) { value =>
      validate[CountryCodeArrayExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidCountryCodes(value.toSet))
      )
    }
  }

  test("fail validation for invalid country code type") {
    val failValue = Gen.choose[Int](0, 100)

    forAll(failValue) { value =>
      validate[CountryCodeInvalidTypeExample](value) should equal(
        Invalid(errorMessage(value), ErrorCode.InvalidCountryCodes(Set(value.toString)))
      )
    }
  }

  //generate random uppercase string for fake country code
  private def genFakeCountryCode: Gen[String] = {
    Gen
      .nonEmptyContainerOf[Seq, Char](Gen.alphaUpperChar)
      .map(_.mkString)
      .filter(!countryCodes.contains(_))
  }

  private def validate[C: Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "countryCode", classOf[CountryCode], value)
  }

  private def errorMessage(value: Any): String = {
    CountryCodeValidator.errorMessage(messageResolver, value)
  }
}
