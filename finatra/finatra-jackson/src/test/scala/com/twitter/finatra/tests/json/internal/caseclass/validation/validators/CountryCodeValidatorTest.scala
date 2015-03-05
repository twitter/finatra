package com.twitter.finatra.tests.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.CountryCodeValidator
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{CountryCode, ValidationResult, ValidatorTest}

case class CountryCodeExample(@CountryCode countryCode: String)
case class CountryCodeSeqExample(@CountryCode countryCode: Seq[String])
case class CountryCodeArrayExample(@CountryCode countryCode: Array[String])
case class CountryCodeInvalidTypeExample(@CountryCode countryCode: Long)

class CountryCodeValidatorTest extends ValidatorTest {

  "country code validator" should {

    "pass validation for valid country code" in {
      val value = "US"
      validate[CountryCodeExample](value) should equal(valid)
    }

    "fail validation for invalid country code" in {
      val value = "FOO"
      validate[CountryCodeExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for valid country codes in seq" in {
      val value = Seq("US", "JP")
      validate[CountryCodeSeqExample](value) should equal(valid)
    }

    "pass validation for empty seq" in {
      val value = Seq()
      validate[CountryCodeSeqExample](value) should equal(valid)
    }

    "fail validation for invalid country codes in seq" in {
      val value = Seq("USA", "JP")
      validate[CountryCodeSeqExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "pass validation for valid country codes in array" in {
      val value = Array("US", "JP")
      validate[CountryCodeArrayExample](value) should equal(valid)
    }

    "fail validation for invalid country codes in array" in {
      val value = Array("USA", "JP")
      validate[CountryCodeArrayExample](value) should equal(
        invalid(
          errorMessage(value)))
    }

    "fail validation for invalid country code type" in {
      val value = 2
      validate[CountryCodeInvalidTypeExample](value) should equal(
        invalid(
          errorMessage(value)))
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].runtimeClass, "countryCode", classOf[CountryCode], value)
  }

  private def errorMessage(value: Any): String = {
    CountryCodeValidator.errorMessage(messageResolver, value)
  }
}
