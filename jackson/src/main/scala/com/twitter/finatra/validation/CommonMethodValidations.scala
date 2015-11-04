package com.twitter.finatra.validation

import com.twitter.finatra.conversions.time._
import com.twitter.finatra.validation.ValidationResult._
import org.joda.time.DateTime

object CommonMethodValidations {

  def validateTimeRange(
    startTime: Option[DateTime],
    endTime: Option[DateTime],
    startTimeProperty: String,
    endTimeProperty: String): ValidationResult = {

    val rangeDefined = startTime.isDefined && endTime.isDefined
    val partialRange = !rangeDefined && (startTime.isDefined || endTime.isDefined)

    if (rangeDefined)
      validateTimeRange(startTime.get, endTime.get, startTimeProperty, endTimeProperty)
    else if (partialRange)
      Invalid(
        "both %s and %s are required for a valid range".format(
          startTimeProperty,
          endTimeProperty))
    else
      Valid
  }

  def validateTimeRange(
    startTime: DateTime,
    endTime: DateTime,
    startTimeProperty: String,
    endTimeProperty: String): ValidationResult = {

    ValidationResult.validate(startTime < endTime,
      "%s [%s] must be after %s [%s]".format(
        endTimeProperty,
        endTime.utcIso8601,
        startTimeProperty,
        startTime.utcIso8601))
  }
}