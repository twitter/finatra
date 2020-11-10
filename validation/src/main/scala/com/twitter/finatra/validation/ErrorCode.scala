package com.twitter.finatra.validation

import java.util.concurrent.TimeUnit
import org.joda.time.DateTime

/**
 * A descriptor for the type of validation error. May be pattern-matched
 * to customize handling of specific errors.
 */
trait ErrorCode

object ErrorCode {
  case object IllegalArgument extends ErrorCode
  case class InvalidBooleanValue(bool: Boolean) extends ErrorCode
  case class InvalidCountryCodes(codes: Set[String]) extends ErrorCode
  case class InvalidTimeGranularity(time: DateTime, targetGranularity: TimeUnit) extends ErrorCode
  case class InvalidUUID(uuid: String) extends ErrorCode
  case class InvalidValues(invalid: Set[String], valid: Set[String]) extends ErrorCode
  case class PatternNotMatched(value: String, regex: String) extends ErrorCode
  case class PatternSyntaxError(message: String, regex: String) extends ErrorCode
  case object RepeatedCommaSeparatedCollection extends ErrorCode
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
