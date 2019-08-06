package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.PatternValidator._
import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation._
import com.twitter.util.{Return, Throw, Try}
import scala.util.matching.Regex

private[json] object PatternValidator {
  def errorMessage(resolver: ValidationMessageResolver, value: Any, regex: String): String = {
    resolver.resolve(classOf[Pattern], value, regex)
  }
}

/**
 * Validates whether given [[CharSequence]] value matches with the specified regular expression
 *
 * @example {{{
 *            case class ExampleRequest(@Pattern(regexp= "exampleRegex") exampleValue : String)
 *         }}}
 */
private[json] class PatternValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: Pattern)
    extends Validator[Pattern, Any](validationMessageResolver, annotation) {

  private val regexp: String = annotation.regexp()
  private val regex: Try[Regex] = Try(regexp.r)

  /* Public */

  override def isValid(value: Any): ValidationResult = {
    val validateRegexResult = validateRegex
    if (validateRegexResult.isValid) {
      value match {
        case arrayValue: Array[_] =>
          validationResult(arrayValue)
        case traversableValue: Traversable[_] =>
          validationResult(traversableValue)
        case stringValue: String =>
          validationResult(stringValue)
        case _ =>
          throw new IllegalArgumentException(
            s"Class [${value.getClass}}] is not supported by ${this.getClass}")
      }
    } else validateRegexResult
  }

  /* Private */

  private def validationResult(value: Traversable[_]): ValidationResult = {
    ValidationResult.validate(
      value.forall(x => validateValue(x.toString)),
      errorMessage(validationMessageResolver, value, regexp),
      errorCode(value, regexp)
    )
  }

  private def validationResult(value: String): ValidationResult = {
    ValidationResult.validate(
      validateValue(value),
      errorMessage(validationMessageResolver, value, regexp),
      errorCode(value, regexp)
    )
  }

  // validate the value after validate the regex
  private def validateValue(value: String): Boolean = {
    regex.get().findFirstIn(value) match {
      case None => false
      case _ => true
    }
  }

  private def validateRegex: ValidationResult =
    regex match {
      case Return(_) => Valid()
      case Throw(ex) => Invalid(ex.getClass.getName, errorCode(ex, regexp))
    }

  private def errorCode(t: Throwable, regex: String) = {
    ErrorCode.PatternSyntaxError(t.getMessage, regex)
  }

  private def errorCode(value: String, regex: String) = {
    ErrorCode.PatternNotMatched(value, regex)
  }

  private def errorCode(value: Traversable[_], regex: String) = {
    ErrorCode.PatternNotMatched(value mkString ",", regex)
  }
}
