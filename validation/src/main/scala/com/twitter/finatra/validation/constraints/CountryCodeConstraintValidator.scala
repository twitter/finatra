package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext}
import java.util.Locale
import scala.jdk.CollectionConverters._

private[validation] object CountryCodeConstraintValidator {

  /** @see [[https://www.iso.org/iso-3166-country-codes.html ISO 3166]] */
  val CountryCodes: Set[String] = Locale.getISOCountries.toSet

  def toErrorValue(value: Any): String =
    value match {
      case arrayValue: Array[_] =>
        arrayValue.mkString(",")
      case traversableValue: Iterable[_] =>
        traversableValue.mkString(",")
      case iterableWrapper: java.util.Collection[_] =>
        String.join(",", iterableWrapper.asInstanceOf[java.lang.Iterable[_ <: String]])
      case anyValue =>
        anyValue.toString
    }
}

/**
 * The validator for [[CountryCode]] annotation.
 *
 * Validate if a given value is a 2-letter country code defined in [[https://www.iso.org/iso-3166-country-codes.html ISO 3166]]
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class CountryCodeConstraintValidator
    extends ConstraintValidator[CountryCode, Any] {
  import CountryCodeConstraintValidator._

  override def isValid(
    obj: Any,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val invalidCountryCodes = obj match {
      case typedValue: Array[Any] =>
        findInvalidCountryCodes(typedValue)
      case iterableWrapper: java.util.Collection[_] =>
        findInvalidCountryCodes(iterableWrapper.asScala)
      case typedValue: Iterable[Any] =>
        findInvalidCountryCodes(typedValue)
      case anyValue =>
        findInvalidCountryCodes(Seq(anyValue.toString))
    }

    val valid = invalidCountryCodes.isEmpty
    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.InvalidCountryCodes(invalidCountryCodes))
        .addMessageParameter("invalidValue", toErrorValue(obj))
        .withMessageTemplate(s"[{invalidValue}] is not a valid country code")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }

  /* Private */

  private[this] def findInvalidCountryCodes(values: Traversable[Any]): Set[String] = {
    val uppercaseCountryCodes = values.toSet.map { value: Any =>
      value.toString.toUpperCase
    }
    uppercaseCountryCodes.diff(CountryCodes)
  }
}
