package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object OneOfConstraintValidator {

  def errorMessage(resolver: MessageResolver, oneOfValues: Set[String], value: Any): String =
    resolver.resolve(
      classOf[OneOf],
      toCommaSeparatedValue(value),
      toCommaSeparatedValue(oneOfValues)
    )

  private def toCommaSeparatedValue(value: Any): String =
    value match {
      case arrayValue: Array[_] =>
        arrayValue mkString (",")
      case traversableValue: Traversable[_] =>
        traversableValue mkString (",")
      case anyValue =>
        anyValue.toString
    }
}

/**
 * The validator for [[OneOf]] annotation.
 *
 * Validate if a if one or more values exist in a given set of values defined in [[OneOf]] annotation.
 * The check for existence is case-sensitive by default.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class OneOfConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[OneOf, Any](messageResolver) {

  /* Public */

  override def isValid(annotation: OneOf, value: Any): ValidationResult = {
    val oneOfValues = annotation.asInstanceOf[OneOf].value().toSet
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue, oneOfValues)
      case traversableValue: Traversable[_] =>
        validationResult(traversableValue, oneOfValues)
      case anyValue =>
        validationResult(Seq(anyValue.toString), oneOfValues)
    }
  }

  /* Private */

  private[this] def validationResult(
    value: Traversable[_],
    oneOfValues: Set[String]
  ): ValidationResult = {
    val invalidValues = findInvalidValues(value, oneOfValues)
    ValidationResult.validate(
      invalidValues.isEmpty,
      OneOfConstraintValidator.errorMessage(messageResolver, oneOfValues, value),
      ErrorCode.InvalidValues(invalidValues, oneOfValues)
    )
  }

  private[this] def findInvalidValues(
    value: Traversable[_],
    oneOfValues: Set[String]
  ): Set[String] = {
    val valueAsStrings = value.map(_.toString).toSet
    valueAsStrings diff oneOfValues
  }
}
