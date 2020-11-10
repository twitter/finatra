package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}
import java.util.Locale

private[validation] object CountryCodeConstraintValidator {

  /** @see [[https://www.iso.org/iso-3166-country-codes.html ISO 3166]] */
  val CountryCodes: Set[String] = Locale.getISOCountries.toSet

  def errorMessage(resolver: MessageResolver, value: Any): String =
    resolver.resolve[CountryCode](toErrorValue(value))

  private def toErrorValue(value: Any): String =
    value match {
      case arrayValue: Array[_] =>
        arrayValue.mkString(",")
      case traversableValue: Iterable[_] =>
        traversableValue.mkString(",")
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
  import CountryCodeConstraintValidator._

  /* Public */
  override def isValid(annotation: CountryCode, value: Any): ValidationResult =
    value match {
      case typedValue: Array[Any] =>
        validationResult(typedValue)
      case typedValue: Iterable[Any] =>
        validationResult(typedValue)
      case anyValue =>
        validationResult(Seq(anyValue.toString))
    }

  private[this] def validationResult(value: Iterable[Any]) = {
    val invalidCountryCodes = findInvalidCountryCodes(value)
    ValidationResult.validate(
      invalidCountryCodes.isEmpty,
      errorMessage(messageResolver, value),
      ErrorCode.InvalidCountryCodes(invalidCountryCodes)
    )
  }

  /* Private */
  private[this] def findInvalidCountryCodes(values: Traversable[Any]): Set[String] = {
    val uppercaseCountryCodes = values.toSet.map { value: Any =>
      value.toString.toUpperCase
    }
    uppercaseCountryCodes.diff(CountryCodes)
  }
}
