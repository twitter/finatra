package com.twitter.finatra.validation

import com.twitter.inject.conversions.time._
import com.twitter.util.validation.engine.MethodValidationResult
import org.joda.time.DateTime

@deprecated(
  "Users should prefer to use standard class-level constraints or cross-parameter constraints.",
  "2021-03-05")
object CommonMethodValidations {

  def validateTimeRange(
    startTime: Option[DateTime],
    endTime: Option[DateTime],
    startTimeProperty: String,
    endTimeProperty: String
  ): MethodValidationResult = {

    val rangeDefined = startTime.isDefined && endTime.isDefined
    val partialRange = !rangeDefined && (startTime.isDefined || endTime.isDefined)

    if (rangeDefined)
      validateTimeRange(startTime.get, endTime.get, startTimeProperty, endTimeProperty)
    else if (partialRange)
      MethodValidationResult.Invalid(
        "both %s and %s are required for a valid range".format(startTimeProperty, endTimeProperty),
        Some(ErrorCode.Unknown)
      )
    else
      MethodValidationResult.Valid
  }

  def validateTimeRange(
    startTime: DateTime,
    endTime: DateTime,
    startTimeProperty: String,
    endTimeProperty: String
  ): MethodValidationResult = {

    MethodValidationResult.validIfTrue(
      startTime < endTime,
      "%s [%s] must be after %s [%s]"
        .format(endTimeProperty, endTime.utcIso8601, startTimeProperty, startTime.utcIso8601),
      Some(ErrorCode.Unknown)
    )
  }
}
