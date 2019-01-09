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

/**
  * Validates whether given [[CharSequence]] value matches with the specified regular expression
  *
  * @example {{{
  *            case class ExampleRequest(@Pattern(regexp= "exampleRegex" exampleValue : String))
  *         }}}
  */
private[finatra] class PatternValidator(validationMessageResolver: ValidationMessageResolver,
                                        annotation: Pattern)
  extends Validator[Pattern, Any](validationMessageResolver, annotation) {

  private val regexp: String = annotation.regexp()

  /* Public */

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

  /* Private */

  private def validationResult(value: Traversable[_]): ValidationResult = {
    ValidationResult.validate(
      value.forall(x => validate(x.toString)),
      errorMessage(validationMessageResolver, value, regexp),
      errorCode(value, regexp)
    )
  }

  private def validationResult(value: String): ValidationResult = {
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

  private def errorCode(value: Traversable[_], regex: String) = {
    ErrorCode.PatternNotMatched(value mkString ",", regex)
  }
}
