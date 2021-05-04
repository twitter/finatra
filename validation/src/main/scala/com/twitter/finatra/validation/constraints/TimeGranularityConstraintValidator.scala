package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext}
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._
import org.joda.time.{DateTime, DateTimeZone}

private[validation] object TimeGranularityConstraintValidator {

  private[validation] def singularize(timeUnit: TimeUnit): String = {
    val timeUnitStr = timeUnit.toString.toLowerCase
    timeUnitStr.substring(0, timeUnitStr.length - 1)
  }
}

/**
 * The validator for [[TimeGranularity]] annotation.
 *
 * Validates if a given value is of a given time granularity (e.g., days, hours, minutes, seconds).
 * E.g. A granularity of Minute is valid for 10:05:00 and not valid for 10:05:13.
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class TimeGranularityConstraintValidator
    extends ConstraintValidator[TimeGranularity, DateTime] {
  import TimeGranularityConstraintValidator._

  @volatile private[this] var timeGranularity: TimeUnit = _

  override def initialize(constraintAnnotation: TimeGranularity): Unit = {
    this.timeGranularity = constraintAnnotation.value
  }

  override def isValid(
    obj: DateTime,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    val valid = isGranularity(obj, timeGranularity)

    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.InvalidTimeGranularity(obj, timeGranularity))
        .withMessageTemplate(
          s"[${obj.toString}] is not ${singularize(timeGranularity)} granularity")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }

  /* Private */

  private[this] def isGranularity(value: DateTime, timeGranularity: TimeUnit): Boolean = {
    val utcDateTime = value.toDateTime(DateTimeZone.UTC)
    toNanos(utcDateTime.getMillis, MILLISECONDS) % toNanos(1, timeGranularity) == 0
  }

  private[this] def toNanos(value: Long, timeUnit: TimeUnit): Long =
    NANOSECONDS.convert(value, timeUnit)
}
