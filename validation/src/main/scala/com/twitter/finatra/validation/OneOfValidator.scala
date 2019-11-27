package com.twitter.finatra.validation

import com.twitter.finatra.validation.OneOfValidator._

private[validation] object OneOfValidator {

  def errorMessage(resolver: ValidationMessageResolver, oneOfValues: Set[String], value: Any) = {

    resolver.resolve(
      classOf[OneOf],
      toCommaSeparatedValue(value),
      toCommaSeparatedValue(oneOfValues)
    )
  }

  private def toCommaSeparatedValue(value: Any) = {
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

/**
 * Validates if one or more values exist in a given set of values.  The check for existence is case-sensitive
 * by default.
 */
private[validation] class OneOfValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: OneOf
) extends Validator[OneOf, Any](validationMessageResolver, annotation) {

  private val oneOfValues = annotation.value().toSet

  /* Public */

  override def isValid(value: Any) = {
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue)
      case traversableValue: Traversable[_] =>
        validationResult(traversableValue)
      case anyValue =>
        validationResult(Seq(anyValue.toString))
    }
  }

  /* Private */

  private def findInvalidValues(value: Traversable[_]) = {
    val valueAsStrings = value.map(_.toString).toSet
    valueAsStrings diff oneOfValues
  }

  private def validationResult(value: Traversable[_]) = {
    val invalidValues = findInvalidValues(value)
    ValidationResult.validate(
      invalidValues.isEmpty,
      errorMessage(validationMessageResolver, oneOfValues, value),
      ErrorCode.InvalidValues(invalidValues, oneOfValues)
    )
  }
}
