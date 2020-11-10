package com.twitter.finatra.validation

import com.twitter.util.logging.Logger
import com.twitter.util.{Return, Throw, Try}
import java.lang.annotation.Annotation

sealed trait ValidationResult {
  def path: Path
  def isValid: Boolean
  def annotation: Option[Annotation]
}

object ValidationResult {
  private[this] lazy val logger = Logger(ValidationResult.getClass)

  case class Valid(
    override val annotation: Option[Annotation] = None)
      extends ValidationResult {
    override val isValid: Boolean = true
    override val path: Path = Path.Empty
  }

  case class Invalid(
    message: String,
    code: ErrorCode = ErrorCode.Unknown,
    path: Path = Path.Empty,
    override val annotation: Option[Annotation] = None)
      extends ValidationResult {
    override val isValid = false
  }

  /**
   * Utility for evaluating a condition in order to return a [[ValidationResult]]. Returns
   * [[ValidationResult.Valid]] when the condition is `true`, otherwise if the condition
   * evaluates to `false` or throws an exception a [[ValidationResult.Invalid]] will be returned.
   * In the case of an exception, the `exception.getMessage` is used in place of the given message.
   *
   * @note This will not allow a non-fatal exception to escape. Instead a [[ValidationResult.Invalid]]
   *       will be returned when a non-fatal exception is encountered when evaluating `condition`. As
   *       this equates failure to execute the condition function to a return of `false`.
   *
   * @param condition function to evaluate for validation.
   * @param message function to evaluate for a message when the given condition is `false`.
   * @param code [[ErrorCode]] to use for when the given condition is `false`.
   *
   * @return a [[ValidationResult.Valid]] when the condition is `true` otherwise a [[ValidationResult.Invalid]].
   */
  def validate(
    condition: => Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown
  ): ValidationResult = Try(condition) match {
    case Return(result) if result =>
      Valid()
    case Return(result) if !result =>
      Invalid(message, code)
    case Throw(e) =>
      logger.warn(e.getMessage, e)
      Invalid(e.getMessage, code)
  }

  /**
   * Utility for evaluating the negation of a condition in order to return a [[ValidationResult]].
   * Returns [[ValidationResult.Valid]] when the condition is `false`, otherwise if the condition
   * evaluates to `true` or throws an exception a [[ValidationResult.Invalid]] will be returned.
   * In the case of an exception, the `exception.getMessage` is used in place of the given message.
   *
   * @note This will not allow a non-fatal exception to escape. Instead a [[ValidationResult.Valid]]
   *       will be returned when a non-fatal exception is encountered when evaluating `condition`. As
   *       this equates failure to execute the condition to a return of `false`.
   *
   * @param condition function to evaluate for validation.
   * @param message function to evaluate for a message when the given condition is `true`.
   * @param code [[ErrorCode]] to use for when the given condition is `true`.
   *
   * @return a [[ValidationResult.Valid]] when the condition is `false` or when the condition evaluation
   *         throws a NonFatal exception otherwise a [[ValidationResult.Invalid]].
   */
  def validateNot(
    condition: => Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown
  ): ValidationResult =
    Try(condition) match {
      case Return(result) if !result =>
        Valid()
      case Return(result) if result =>
        Invalid(message, code)
      case Throw(e) =>
        logger.warn(e.getMessage, e)
        Valid()
    }
}
