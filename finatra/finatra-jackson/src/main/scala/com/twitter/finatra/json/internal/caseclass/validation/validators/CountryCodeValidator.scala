package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.CountryCodeValidator._
import com.twitter.finatra.validation.{Validator, ValidationMessageResolver, CountryCode, ValidationResult}
import java.util.Locale

object CountryCodeValidator {

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

class CountryCodeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: CountryCode)
  extends Validator[CountryCode, Any](
    validationMessageResolver,
    annotation) {

  private val countryCodes = Locale.getISOCountries.toSet

  /* Public */

  override def isValid(value: Any): ValidationResult = {
    value match {
      case typedValue: Array[_] =>
        validationResult(typedValue)
      case typedValue: Traversable[_] =>
        validationResult(typedValue)
      case anyValue =>
        validationResult(
          Seq(anyValue.toString))
    }
  }

  /* Private */

  private def findInvalidCountryCodes(value: Traversable[_]) = {
    val uppercaseCountryCodes = (value map { _.toString.toUpperCase }).toSet
    uppercaseCountryCodes diff countryCodes
  }

  private def validationResult(value: Traversable[_]) = {
    val invalidCountryCodes = findInvalidCountryCodes(value)
    ValidationResult(
      invalidCountryCodes.isEmpty,
      errorMessage(
        validationMessageResolver,
        value))
  }
}