package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.util.{Return, Throw, Try}
import scala.util.matching.Regex
import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object PatternConstraintValidator {

  def errorMessage(resolver: MessageResolver, value: Any, regex: String): String =
    resolver.resolve(classOf[Pattern], value, regex)
}

/**
 * The validator for [[Pattern]] annotation.
 *
 * Validates whether given [[CharSequence]] value matches with the specified regular expression.
 *
 * @example {{{
 *            case class ExampleRequest(@Pattern(regexp= "exampleRegex") exampleValue : String)
 * }}}
 */
private[validation] class PatternConstraintValidator(messageResolver: MessageResolver)
    extends ConstraintValidator[Pattern, Any](messageResolver) {

  import PatternConstraintValidator._

  /* Public */

  override def isValid(annotation: Pattern, value: Any): ValidationResult = {
    val regexp: String = annotation.asInstanceOf[Pattern].regexp()
    val regex: Try[Regex] = Try(regexp.r)
    val validateRegexResult = validateRegex(regexp, regex)
    if (validateRegexResult.isValid) {
      value match {
        case arrayValue: Array[_] =>
          validationResult(arrayValue, regexp, regex)
        case traversableValue: Traversable[_] =>
          validationResult(traversableValue, regexp, regex)
        case stringValue: String =>
          validationResult(stringValue, regexp, regex)
        case _ =>
          throw new IllegalArgumentException(
            s"Class [${value.getClass}}] is not supported by ${this.getClass}")
      }
    } else validateRegexResult
  }

  /* Private */

  private[this] def validationResult(
    value: Traversable[_],
    regexp: String,
    regex: Try[Regex]
  ): ValidationResult =
    ValidationResult.validate(
      value.forall(x => validateValue(x.toString, regex)),
      errorMessage(messageResolver, value, regexp),
      errorCode(value, regexp)
    )

  private[this] def errorCode(value: Traversable[_], regex: String): ErrorCode =
    ErrorCode.PatternNotMatched(value.mkString(","), regex)

  private[this] def validationResult(
    value: String,
    regexp: String,
    regex: Try[Regex]
  ): ValidationResult =
    ValidationResult.validate(
      validateValue(value, regex),
      errorMessage(messageResolver, value, regexp),
      errorCode(value, regexp)
    )

  // validate the value after validate the regex
  private[this] def validateValue(value: String, regex: Try[Regex]): Boolean =
    regex.get().findFirstIn(value) match {
      case None => false
      case _ => true
    }

  private[this] def errorCode(value: String, regex: String): ErrorCode =
    ErrorCode.PatternNotMatched(value, regex)

  private[this] def validateRegex(regexp: String, regex: Try[Regex]): ValidationResult =
    regex match {
      case Return(_) => Valid()
      case Throw(ex) => Invalid(ex.getClass.getName, errorCode(ex, regexp))
    }

  private[this] def errorCode(t: Throwable, regex: String): ErrorCode =
    ErrorCode.PatternSyntaxError(t.getMessage, regex)
}
