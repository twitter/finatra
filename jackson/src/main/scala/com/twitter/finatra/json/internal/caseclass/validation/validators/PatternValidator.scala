package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.PatternValidator._
import com.twitter.finatra.validation._

private[finatra] object PatternValidator {
  def errorMessage(resolver: ValidationMessageResolver, value: Any, regex: String): String = {
    resolver.resolve(classOf[Pattern], value, regex)
  }

  def errorMessage(resolver: ValidationMessageResolver): String = {
    resolver.resolve(classOf[Pattern])
  }
}

private[finatra] class PatternValidator(validationMessageResolver: ValidationMessageResolver,
                                        annotation: Pattern)
  extends Validator[Pattern, Any](validationMessageResolver, annotation) {

  private val regexp: String = annotation.regexp()

  override def isValid(value: Any): ValidationResult = {
    value match {
      case arrayValue: Array[_] =>
        validationResult(arrayValue)
      case traversableValue: Traversable[_] =>
        validationResult(traversableValue)
      case stringValue: String =>
        validationResult(stringValue)
      case _ =>
        throw new IllegalArgumentException(s"Class [${value.getClass}}] is not supported by ${this.getClass}")
    }
  }

  private def validationResult(value: Traversable[_]): ValidationResult = {
    if (regexp.isEmpty) {
      return ValidationResult.Invalid(errorMessage(validationMessageResolver), ErrorCode.PatternCannotBeEmpty)
    }
    if (value.forall(x => validate(x.toString))) {
      ValidationResult.Valid
    } else {
      ValidationResult.validate(
        condition = false,
        errorMessage(validationMessageResolver, value, regexp),
        errorCode("[]", regexp)
      )
    }
  }

  private def validationResult(value: String): ValidationResult = {
    if (regexp.isEmpty) {
      return ValidationResult.Invalid(errorMessage(validationMessageResolver), ErrorCode.PatternCannotBeEmpty)
    }
    ValidationResult.validate(
      validate(value),
      errorMessage(validationMessageResolver, value, regexp),
      errorCode(value, regexp)
    )
  }

  private def validate(value: String): Boolean = {
    val regex = regexp.r
    regex.findFirstIn(value) match {
      case None => false
      case _ => true

    }
  }

  private def errorCode(value: String, regex: String) = {
    ErrorCode.PatternNotMatched(value, regex)
  }
}