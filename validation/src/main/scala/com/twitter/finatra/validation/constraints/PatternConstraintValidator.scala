package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import com.twitter.util.{Return, Throw, Try}
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext, UnexpectedTypeException}
import scala.util.matching.Regex

private[validation] object PatternConstraintValidator {

  private[validation] def toErrorValue(value: Any): String =
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
 * The validator for [[Pattern]] annotation.
 *
 * Validates whether given [[CharSequence]] value matches with the specified regular expression.
 *
 * @example
 * {{{
 *   case class ExampleRequest(@Pattern(regexp= "exampleRegex") exampleValue : String)
 * }}}
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class PatternConstraintValidator extends ConstraintValidator[Pattern, Any] {
  import PatternConstraintValidator._

  @volatile private[this] var pattern: Pattern = _

  override def initialize(constraintAnnotation: Pattern): Unit = {
    this.pattern = constraintAnnotation
  }

  override def isValid(
    obj: Any,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val regexp: String = pattern.regexp()
    val regex: Try[Regex] = Try(regexp.r)
    val validateRegexResult = validateRegex(regexp, regex, constraintValidatorContext)
    if (validateRegexResult) {
      obj match {
        case arrayValue: Array[_] =>
          validationResult(arrayValue, regexp, regex, constraintValidatorContext)
        case traversableValue: Iterable[_] =>
          validationResult(traversableValue, regexp, regex, constraintValidatorContext)
        case stringValue: String =>
          validationResult(stringValue, regexp, regex, constraintValidatorContext)
        case _ =>
          throw new UnexpectedTypeException(
            s"Class [${obj.getClass.getName}] is not supported by ${this.getClass.getName}")
      }
    } else validateRegexResult
  }

  /* Private */

  private[this] def validationResult(
    value: Iterable[_],
    regexp: String,
    regex: Try[Regex],
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val valid = value.isEmpty || value.forall(x => validateValue(x.toString, regex))
    if (!valid) errorCode(value, regexp, constraintValidatorContext)
    valid
  }

  private[this] def errorCode(
    value: Iterable[_],
    regex: String,
    constraintValidatorContext: ConstraintValidatorContext
  ): Unit = {
    TwitterConstraintValidatorContext
      .withDynamicPayload(ErrorCode.PatternNotMatched(toErrorValue(value), regex))
      .withMessageTemplate(s"[${toErrorValue(value)}] does not match regex $regex")
      .addConstraintViolation(constraintValidatorContext)
  }

  private[this] def validationResult(
    value: String,
    regexp: String,
    regex: Try[Regex],
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val valid = validateValue(value, regex)
    if (!valid) errorCode(value, regexp, constraintValidatorContext)
    valid
  }

  // validate the value after validate the regex
  private[this] def validateValue(value: String, regex: Try[Regex]): Boolean =
    regex.get().findFirstIn(value) match {
      case None => false
      case _ => true
    }

  private[this] def errorCode(
    value: String,
    regex: String,
    constraintValidatorContext: ConstraintValidatorContext
  ): Unit = {
    TwitterConstraintValidatorContext
      .withDynamicPayload(ErrorCode.PatternNotMatched(toErrorValue(value), regex))
      .withMessageTemplate(s"[${toErrorValue(value)}] does not match regex $regex")
      .addConstraintViolation(constraintValidatorContext)
  }

  private[this] def validateRegex(
    regexp: String,
    regex: Try[Regex],
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean =
    regex match {
      case Return(_) => true
      case Throw(ex) =>
        errorCode(ex, regexp, constraintValidatorContext)
        false
    }

  private[this] def errorCode(
    t: Throwable,
    regex: String,
    constraintValidatorContext: ConstraintValidatorContext
  ): Unit = {
    TwitterConstraintValidatorContext
      .withDynamicPayload(ErrorCode.PatternSyntaxError(t.getMessage, regex))
      .withMessageTemplate(t.getClass.getName)
      .addConstraintViolation(constraintValidatorContext)
  }
}
