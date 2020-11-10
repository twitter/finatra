package com.twitter.finatra.validation.constraints

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._
import org.joda.time.{DateTime, DateTimeZone}
import com.twitter.finatra.validation.{
  ConstraintValidator,
  ErrorCode,
  MessageResolver,
  ValidationResult
}

private[validation] object TimeGranularityConstraintValidator {

  def errorMessage(resolver: MessageResolver, timeGranularity: TimeUnit, value: DateTime): String =
    resolver.resolve[TimeGranularity](value, singularize(timeGranularity))

  private def singularize(timeUnit: TimeUnit): String = {
    val timeUnitStr = timeUnit.toString.toLowerCase
    timeUnitStr.substring(0, timeUnitStr.length - 1)
  }
}

/**
 * The validator for [[TimeGranularity]] annotation.
 *
 * Validates if a given value is of a given time granularity (e.g., days, hours, minutes, seconds).
 * E.g. A granularity of Minute is valid for 10:05:00 and not valid for 10:05:13.
 *
 * @param messageResolver to resolve error message when validation fails.
 */
private[validation] class TimeGranularityConstraintValidator(
  messageResolver: MessageResolver)
    extends ConstraintValidator[TimeGranularity, DateTime](messageResolver) {

  /* Public */

  override def isValid(annotation: TimeGranularity, value: DateTime): ValidationResult = {
    val timeGranularity = annotation.asInstanceOf[TimeGranularity].value()
    ValidationResult.validate(
      isGranularity(value, timeGranularity),
      TimeGranularityConstraintValidator
        .errorMessage(messageResolver, timeGranularity, value),
      ErrorCode.InvalidTimeGranularity(value, timeGranularity)
    )
  }

  /* Private */

  private[this] def isGranularity(value: DateTime, timeGranularity: TimeUnit): Boolean = {
    val utcDateTime = value.toDateTime(DateTimeZone.UTC)
    toNanos(utcDateTime.getMillis, MILLISECONDS) % toNanos(1, timeGranularity) == 0
  }

  private[this] def toNanos(value: Long, timeUnit: TimeUnit): Long =
    NANOSECONDS.convert(value, timeUnit)
}
