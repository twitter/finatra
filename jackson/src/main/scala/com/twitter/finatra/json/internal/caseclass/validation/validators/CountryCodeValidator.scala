package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.CountryCodeValidator._
import com.twitter.finatra.validation.{CountryCode, ErrorCode, ValidationMessageResolver, ValidationResult, Validator}
import java.util.Locale

private[finatra] object CountryCodeValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    value: Any) = {

    resolver.resolve(
      classOf[CountryCode],
      toErrorValue(value))
  }

  private def toErrorValue(value: Any) = {
    value match {
      case arrayValue: Array[_] =>
        arrayValue mkString ","
      case traversableValue: Traversable[_] =>
        traversableValue mkString ","
      case anyValue =>
        anyValue.toString
    }
  }
}

private[finatra] class CountryCodeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: CountryCode)
  extends Validator[CountryCode, Any](
    validationMessageResolver,
    annotation) {

  private val countryCodes = Locale.getISOCountries.toSet

  /* Public */

  override def isValid(value: Any): ValidationResult = {
    value match {
      case typedValue: Array[Any] =>
        validationResult(typedValue)
      case typedValue: Traversable[Any] =>
        validationResult(typedValue)
      case anyValue =>
        validationResult(
          Seq(anyValue.toString))
    }
  }

  /* Private */

  private def findInvalidCountryCodes(values: Traversable[Any]) = {
    val uppercaseCountryCodes = values.toSet map { value: Any =>
      value.toString.toUpperCase
    }

    uppercaseCountryCodes diff countryCodes
  }

  private def validationResult(value: Traversable[Any]) = {
    val invalidCountryCodes = findInvalidCountryCodes(value)
    ValidationResult.validate(
      invalidCountryCodes.isEmpty,
      errorMessage(
        validationMessageResolver,
        value),
      ErrorCode.InvalidCountryCodes(invalidCountryCodes))
  }
}