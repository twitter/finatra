package com.twitter.finatra.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.internal.caseclass.validation.validators.TimeGranularityValidator._
import com.twitter.finatra.validation.{ErrorCode, TimeGranularity, ValidationMessageResolver, ValidationResult, Validator}
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._
import org.joda.time.{DateTime, DateTimeZone}

private[finatra] object TimeGranularityValidator {

  def errorMessage(
    resolver: ValidationMessageResolver,
    timeGranularity: TimeUnit,
    value: DateTime) = {

    resolver.resolve(
      classOf[TimeGranularity],
      value,
      singularize(timeGranularity))
  }

  private def singularize(timeUnit: TimeUnit): String = {
    val timeUnitStr = timeUnit.toString.toLowerCase
    timeUnitStr.substring(0, timeUnitStr.length - 1)
  }
}

/**
 * Validates if a given value is of a given time granularity (e.g., days, hours, seconds)
 */
private[finatra] class TimeGranularityValidator(
  validationMessageResolver: ValidationMessageResolver,
  annotation: TimeGranularity)
  extends Validator[TimeGranularity, DateTime](
    validationMessageResolver,
    annotation) {

  private val timeGranularity = annotation.value()

  /* Public */

  override def isValid(value: DateTime) = {
    ValidationResult.validate(
      isGranularity(value),
      errorMessage(
        validationMessageResolver,
        timeGranularity,
        value),
      ErrorCode.InvalidTimeGranularity(value, timeGranularity))
  }

  /* Private */

  private def toNanos(value: Long, timeUnit: TimeUnit): Long = {
    NANOSECONDS.convert(value, timeUnit)
  }

  private def isGranularity(value: DateTime): Boolean = {
    val utcDateTime = value.toDateTime(DateTimeZone.UTC)
    toNanos(utcDateTime.getMillis, MILLISECONDS) % toNanos(1, timeGranularity) == 0
  }
}
