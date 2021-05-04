package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext}
import scala.jdk.CollectionConverters._

private[validation] object OneOfConstraintValidator {

  private[validation] def toCommaSeparatedValue(value: Any): String =
    value match {
      case arrayValue: Array[_] =>
        arrayValue.mkString(",")
      case traversableValue: Traversable[_] =>
        traversableValue.mkString(",")
      case iterableWrapper: java.util.Collection[_] =>
        String.join(",", iterableWrapper.asInstanceOf[java.lang.Iterable[_ <: String]])
      case anyValue =>
        anyValue.toString
    }
}

/**
 * The validator for [[OneOf]] annotation.
 *
 * Validate if one or more values exist in a given set of values defined in [[OneOf]] annotation.
 * The check for existence is case-sensitive by default.
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class OneOfConstraintValidator extends ConstraintValidator[OneOf, Any] {
  import OneOfConstraintValidator._

  @volatile private[this] var oneOfValues: Set[String] = _

  override def initialize(constraintAnnotation: OneOf): Unit = {
    this.oneOfValues = constraintAnnotation.value().toSet
  }

  override def isValid(
    obj: Any,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = obj match {
    case arrayValue: Array[_] =>
      findInvalidValues(arrayValue, constraintValidatorContext)
    case traversableValue: Iterable[_] =>
      findInvalidValues(traversableValue, constraintValidatorContext)
    case iterableWrapper: java.util.Collection[_] =>
      findInvalidValues(iterableWrapper.asScala, constraintValidatorContext)
    case anyValue =>
      findInvalidValues(Seq(anyValue.toString), constraintValidatorContext)
  }

  /* Private */

  private[this] def findInvalidValues(
    value: Iterable[_],
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val valueAsStrings = value.map(_.toString).toSet
    val invalidValues = valueAsStrings.diff(this.oneOfValues)
    val valid = invalidValues.isEmpty
    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.InvalidValues(invalidValues, this.oneOfValues))
        .withMessageTemplate(
          s"[${toCommaSeparatedValue(value)}] is not one of [${toCommaSeparatedValue(this.oneOfValues)}]")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }
}
