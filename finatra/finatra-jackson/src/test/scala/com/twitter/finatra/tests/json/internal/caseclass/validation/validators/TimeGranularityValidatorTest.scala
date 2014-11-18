package com.twitter.finatra.tests.json.internal.caseclass.validation.validators

import com.twitter.finatra.json.ValidationResult
import com.twitter.finatra.json.ValidationResult._
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.validation.validators.TimeGranularityValidator
import com.twitter.finatra.tests.json.ValidatorTest
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime

case class TimeGranularityNanosecondsExample(@TimeGranularity(TimeUnit.NANOSECONDS) timeValue: DateTime)
case class TimeGranularityMicrosecondsExample(@TimeGranularity(TimeUnit.MICROSECONDS) timeValue: DateTime)
case class TimeGranularityMillisecondsExample(@TimeGranularity(TimeUnit.MILLISECONDS) timeValue: DateTime)
case class TimeGranularitySecondsExample(@TimeGranularity(TimeUnit.SECONDS) timeValue: DateTime)
case class TimeGranularityMinutesExample(@TimeGranularity(TimeUnit.MINUTES) timeValue: DateTime)
case class TimeGranularityHoursExample(@TimeGranularity(TimeUnit.HOURS) timeValue: DateTime)
case class TimeGranularityDaysExample(@TimeGranularity(TimeUnit.DAYS) timeValue: DateTime)

class TimeGranularityValidatorTest extends ValidatorTest {

  "time granularity validator" should {

    "pass validation for a day granularity value" in {
      val value = new DateTime("2014-3-26T00:00:00Z")
      validate[TimeGranularityDaysExample](value) should equal(
        valid(
          errorMessage[TimeGranularityDaysExample](value)))
    }

    "fail validation for an invalid day granularity value" in {
      val value = new DateTime("2014-3-26T01:00:00Z")
      validate[TimeGranularityDaysExample](value) should equal(
        invalid(
          errorMessage[TimeGranularityDaysExample](value)))
    }

    "pass validation for a hour granularity value" in {
      val value = new DateTime("2014-3-26T04:00:00Z")
      validate[TimeGranularityHoursExample](value) should equal(
        valid(
          errorMessage[TimeGranularityHoursExample](value)))
    }

    "fail validation for an invalid hour granularity value" in {
      val value = new DateTime("2014-3-26T04:01:00Z")
      validate[TimeGranularityHoursExample](value) should equal(
        invalid(
          errorMessage[TimeGranularityHoursExample](value)))
    }

    "pass validation for a minute granularity value" in {
      val value = new DateTime("2014-3-26T04:07:00Z")
      validate[TimeGranularityMinutesExample](value) should equal(
        valid(
          errorMessage[TimeGranularityMinutesExample](value)))
    }

    "fail validation for an invalid minute granularity value" in {
      val value = new DateTime("2014-3-26T04:07:01Z")
      validate[TimeGranularityMinutesExample](value) should equal(
        invalid(
          errorMessage[TimeGranularityMinutesExample](value)))
    }

    "pass validation for a second granularity value" in {
      val value = new DateTime("2014-3-26T04:07:31.000Z")
      validate[TimeGranularitySecondsExample](value) should equal(
        valid(
          errorMessage[TimeGranularitySecondsExample](value)))
    }

    "fail validation for an invalid second granularity value" in {
      val value = new DateTime("2014-3-26T04:07:31.001Z")
      validate[TimeGranularitySecondsExample](value) should equal(
        invalid(
          errorMessage[TimeGranularitySecondsExample](value)))
    }

    "pass validation for a millisecond granularity value" in {
      val value = new DateTime("2014-3-26T04:07:31.001Z")
      validate[TimeGranularityMillisecondsExample](value) should equal(
        valid(
          errorMessage[TimeGranularityMillisecondsExample](value)))
    }

    "pass validation for a microsecond granularity value" in {
      val value = new DateTime("2014-3-26T04:07:31.001Z")
      validate[TimeGranularityMicrosecondsExample](value) should equal(
        valid(
          errorMessage[TimeGranularityMicrosecondsExample](value)))
    }

    "pass validation for a nanosecond granularity value" in {
      val value = new DateTime("2014-3-26T04:07:31.001Z")
      validate[TimeGranularityNanosecondsExample](value) should equal(
        valid(
          errorMessage[TimeGranularityNanosecondsExample](value)))
    }
  }

  private def validate[C : Manifest](value: Any): ValidationResult = {
    super.validate(manifest[C].erasure, "timeValue", classOf[TimeGranularity], value)
  }

  private def errorMessage[C : Manifest](value: DateTime): String = {
    val annotation = getValidationAnnotation(
      manifest[C].erasure,
      "timeValue",
      classOf[TimeGranularity])

    TimeGranularityValidator.errorMessage(
      messageResolver,
      annotation.value(),
      value)
  }
}
