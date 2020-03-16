package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}
import java.util.Locale

private[validation] object CountryCodeConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: Any): String =
    resolver.resolve(classOf[CountryCode], toErrorValue(value))

  private def toErrorValue(value: Any): String =
    value match {
      case arrayValue: Array[_] =>
        arrayValue mkString(",")
      case traversableValue: Traversable[_] =>
        traversableValue mkString(",")
      case anyValue =>
        anyValue.toString
    }
}

/**
 * The validator for [[CountryCode]] annotation.
 *
 * Validate if a given value is a 2-letter country code defined in [[https://www.iso.org/iso-3166-country-codes.html ISO 3166]]
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class CountryCodeConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[CountryCode, Any](messageResolver) {

  private val countryCodes = Locale.getISOCountries.toSet

  /* Public */
  override def isValid(annotation: CountryCode, value: Any): ValidationResult =
    value match {
      case typedValue: Array[Any] =>
        validationResult(typedValue)
      case typedValue: Traversable[Any] =>
        validationResult(typedValue)
      case anyValue =>
        validationResult(Seq(anyValue.toString))
    }

  private[this] def validationResult(value: Traversable[Any]) = {
    val invalidCountryCodes = findInvalidCountryCodes(value)
    ValidationResult.validate(
      invalidCountryCodes.isEmpty,
      CountryCodeConstraintValidator.errorMessage(messageResolver, value),
      ErrorCode.InvalidCountryCodes(invalidCountryCodes)
    )
  }

  /* Private */
  private[this] def findInvalidCountryCodes(values: Traversable[Any]): Set[String] = {
    val uppercaseCountryCodes = values.toSet map { value: Any =>
      value.toString.toUpperCase
    }

    uppercaseCountryCodes.diff(countryCodes)
  }
}
