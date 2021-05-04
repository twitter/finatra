package com.twitter.finatra.validation

import com.twitter.util.validation.engine.MethodValidationResult
import java.lang.annotation.Annotation

@deprecated("Use com.twitter.util.validation.MethodValidationResult", "2021-03-25")
object ValidationResult {

  @deprecated("Use com.twitter.util.validation.MethodValidationResult", "2021-03-25")
  object Valid {
    def apply(annotation: Option[Annotation] = None): MethodValidationResult.Valid.type = {
      MethodValidationResult.Valid
    }
  }

  @deprecated("Use com.twitter.util.validation.MethodValidationResult", "2021-03-25")
  object Invalid {
    def apply(
      message: String,
      code: ErrorCode = ErrorCode.Unknown,
      annotation: Option[Annotation] = None
    ): MethodValidationResult.Invalid = {
      MethodValidationResult.Invalid(
        message = message,
        payload = Some(code)
      )
    }
  }

  /**
   * Utility for evaluating a condition in order to return a [[MethodValidationResult]]. Returns
   * [[MethodValidationResult.Valid]] when the condition is `true`, otherwise if the condition
   * evaluates to `false` or throws an exception a [[MethodValidationResult.Invalid]] will be returned.
   * In the case of an exception, the `exception.getMessage` is used in place of the given message.
   *
   * @note This will not allow a non-fatal exception to escape. Instead a [[MethodValidationResult.Invalid]]
   *       will be returned when a non-fatal exception is encountered when evaluating `condition`. As
   *       this equates failure to execute the condition function to a return of `false`.
   *
   * @param condition function to evaluate for validation.
   * @param message function to evaluate for a message when the given condition is `false`.
   * @param code [[ErrorCode]] to use for when the given condition is `false`.
   *
   * @return a [[MethodValidationResult.Valid]] when the condition is `true` otherwise a [[MethodValidationResult.Invalid]].
   */
  @deprecated("Use com.twitter.util.validation.MethodValidationResult#validIfTrue", "2021-03-25")
  def validate(
    condition: => Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown
  ): MethodValidationResult = MethodValidationResult.validIfTrue(
    condition,
    message,
    Some(code)
  )

  /**
   * Utility for evaluating the negation of a condition in order to return a [[MethodValidationResult]].
   * Returns [[MethodValidationResult.Valid]] when the condition is `false`, otherwise if the condition
   * evaluates to `true` or throws an exception a [[MethodValidationResult.Invalid]] will be returned.
   * In the case of an exception, the `exception.getMessage` is used in place of the given message.
   *
   * @note This will not allow a non-fatal exception to escape. Instead a [[MethodValidationResult.Valid]]
   *       will be returned when a non-fatal exception is encountered when evaluating `condition`. As
   *       this equates failure to execute the condition to a return of `false`.
   *
   * @param condition function to evaluate for validation.
   * @param message function to evaluate for a message when the given condition is `true`.
   * @param code [[ErrorCode]] to use for when the given condition is `true`.
   *
   * @return a [[MethodValidationResult.Valid]] when the condition is `false` or when the condition evaluation
   *         throws a NonFatal exception otherwise a [[MethodValidationResult.Invalid]].
   */
  @deprecated("Use com.twitter.util.validation.MethodValidationResult#validIfFalse", "2021-03-25")
  def validateNot(
    condition: => Boolean,
    message: => String,
    code: ErrorCode = ErrorCode.Unknown
  ): MethodValidationResult = MethodValidationResult.validIfFalse(
    condition,
    message,
    Some(code)
  )
}
