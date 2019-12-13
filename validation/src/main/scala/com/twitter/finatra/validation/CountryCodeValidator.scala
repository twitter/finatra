package com.twitter.finatra.validation

import com.twitter.finatra.validation.CountryCodeValidator._
import java.util.Locale

private[validation] object CountryCodeValidator {

  def errorMessage(resolver: ValidationMessageResolver, value: Any): String = {

    resolver.resolve(classOf[CountryCode], toErrorValue(value))
  }

  private def toErrorValue(value: Any): String = {
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

private[validation] class CountryCodeValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: CountryCode
) extends Validator[CountryCode, Any](validationMessageResolver, annotation) {

  private val countryCodes = Locale.getISOCountries.toSet

  /* Public */

  override def isValid(value: Any): ValidationResult = {
    value match {
      case typedValue: Array[Any] =>
        validationResult(typedValue)
      case typedValue: Traversable[Any] =>
        validationResult(typedValue)
      case anyValue =>
        validationResult(Seq(anyValue.toString))
    }
  }

  /* Private */

  private def findInvalidCountryCodes(values: Traversable[Any]): Set[String] = {
    val uppercaseCountryCodes = values.toSet map { value: Any =>
      value.toString.toUpperCase
    }

    uppercaseCountryCodes diff countryCodes
  }

  private def validationResult(value: Traversable[Any]) = {
    val invalidCountryCodes = findInvalidCountryCodes(value)
    ValidationResult.validate(
      invalidCountryCodes.isEmpty,
      errorMessage(validationMessageResolver, value),
      ErrorCode.InvalidCountryCodes(invalidCountryCodes)
    )
  }
}
