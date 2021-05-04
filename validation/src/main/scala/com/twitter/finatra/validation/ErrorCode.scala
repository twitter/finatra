package com.twitter.finatra.validation

import jakarta.validation.Payload
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime

/**
 * A descriptor for the type of validation error. May be pattern-matched
 * to customize handling of specific errors.
 */
@deprecated("No replacement.", "2021-03-25")
trait ErrorCode extends Payload

/**
 * Users can obtain the same information by inspecting returned `ConstraintViolation` instances.
 * The invalid value is returned from `violation#getInvalidValue` and the configured annotation
 * which specified configuration values can be obtained from `violation#getConstraintDescriptor#getAnnotation`.
 *
 * @deprecated There is no direct replacement for the classes and objects defined here.
 *             These should have likely been defined with their associated validator which
 *             may have prevented their usage from being disassociated from their intended
 *             behavior and meaning. They were also improperly exposed as part of the Jackson
 *             JSON parsing API via being returned as part of the `ValidationResult.Invalid`
 *             type in the `CaseClassFieldMappingException` which has been cleaned up thus
 *             making having specifically typed "error codes" no longer necessary.
 *             Lastly, having a single collection of "error codes" ultimately leads to the library
 *             containing the error codes becoming a kitchen sink as new error codes are added.
 */
@deprecated("No replacement.", "2021-03-25")
object ErrorCode {

  case object IllegalArgument extends ErrorCode
  case class InvalidBooleanValue(bool: Boolean) extends ErrorCode
  case class InvalidCountryCodes(codes: Set[String]) extends ErrorCode
  case class InvalidTimeGranularity(time: DateTime, targetGranularity: TimeUnit) extends ErrorCode
  case class InvalidUUID(uuid: String) extends ErrorCode
  case class InvalidValues(invalid: Set[String], valid: Set[String]) extends ErrorCode
  case class PatternNotMatched(value: String, regex: String) extends ErrorCode
  case class PatternSyntaxError(message: String, regex: String) extends ErrorCode
  case object RequiredFieldMissing extends ErrorCode
  case class SizeOutOfRange(size: Number, min: Long, max: Long) extends ErrorCode
  case class TimeNotFuture(time: DateTime) extends ErrorCode
  case class TimeNotPast(time: DateTime) extends ErrorCode
  case object Unknown extends ErrorCode
  case object ValueCannotBeEmpty extends ErrorCode
  case class ValueOutOfRange(value: Number, min: Long, max: Long) extends ErrorCode
  case class ValueTooLarge(maxValue: Long, value: Number) extends ErrorCode
  case class ValueTooSmall(minValue: Long, value: Number) extends ErrorCode
}
