package com.twitter.finatra.json.internal.caseclass.validation

import com.twitter.finatra.conversions.time._
import com.twitter.finatra.json.internal.caseclass.validation.ValidationResult._
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
      invalid(
        "both %s and %s are required for a valid range".format(
          startTimeProperty,
          endTimeProperty))
    else
      valid
  }

  def validateTimeRange(
    startTime: DateTime,
    endTime: DateTime,
    startTimeProperty: String,
    endTimeProperty: String): ValidationResult = {

    ValidationResult(startTime < endTime,
      "%s [%s] must be after %s [%s]".format(
        endTimeProperty,
        endTime.utcIso8601,
        startTimeProperty,
        startTime.utcIso8601))
  }
}