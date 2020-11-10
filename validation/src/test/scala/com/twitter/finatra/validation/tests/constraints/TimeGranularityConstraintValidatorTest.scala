package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.constraints.{
  TimeGranularity,
  TimeGranularityConstraintValidator
}
import com.twitter.finatra.validation.tests.caseclasses._
import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode, ValidationResult}
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class TimeGranularityConstraintValidatorTest
    extends ConstraintValidatorTest
    with ScalaCheckDrivenPropertyChecks {

  test("pass validation for a day granularity value") {
    val dayGranularity = for {
      day <- Gen.choose[Long](1, 500)
    } yield new DateTime("2014-3-26T00:00:00Z").getMillis + java.util.concurrent.TimeUnit.DAYS
      .toMillis(day)

    forAll(dayGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityDaysExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for an invalid day granularity value") {
    val dayInvalidGranularity = for {
      hour <- Gen.choose[Long](1, 1000).filter(_ % 24 != 0)
    } yield new DateTime("2014-3-26T00:00:00Z").getMillis + java.util.concurrent.TimeUnit.HOURS
      .toMillis(hour)

    forAll(dayInvalidGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityDaysExample](dateTimeValue) should equal(
        Invalid(
          errorMessage[TimeGranularityDaysExample](dateTimeValue),
          ErrorCode.InvalidTimeGranularity(dateTimeValue, TimeUnit.DAYS)
        )
      )
    }
  }

  test("pass validation for a hour granularity value") {
    val hourGranularity = for {
      hour <- Gen.choose[Long](1, 500)
    } yield new DateTime("2014-3-26T01:00:00Z").getMillis + java.util.concurrent.TimeUnit.HOURS
      .toMillis(hour)

    forAll(hourGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityHoursExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for an invalid hour granularity value") {
    val hourInvalidGranularity = for {
      min <- Gen.choose[Long](1, 1000).filter(_ % 60 != 0)
    } yield new DateTime("2014-3-26T02:00:00Z").getMillis + java.util.concurrent.TimeUnit.MINUTES
      .toMillis(min)

    forAll(hourInvalidGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityHoursExample](dateTimeValue) should equal(
        Invalid(
          errorMessage[TimeGranularityHoursExample](dateTimeValue),
          ErrorCode.InvalidTimeGranularity(dateTimeValue, TimeUnit.HOURS)
        )
      )
    }
  }

  test("pass validation for a minute granularity value") {
    val minGranularity = for {
      min <- Gen.choose[Long](1, 500)
    } yield new DateTime("2014-3-26T01:10:00Z").getMillis + java.util.concurrent.TimeUnit.MINUTES
      .toMillis(min)

    forAll(minGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityMinutesExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for an invalid minute granularity value") {
    val minInvalidGranularity = for {
      second <- Gen.choose[Long](1, 1000).filter(_ % 60 != 0)
    } yield new DateTime("2014-3-26T02:20:00Z").getMillis + java.util.concurrent.TimeUnit.SECONDS
      .toMillis(second)

    forAll(minInvalidGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityMinutesExample](dateTimeValue) should equal(
        Invalid(
          errorMessage[TimeGranularityMinutesExample](dateTimeValue),
          ErrorCode.InvalidTimeGranularity(dateTimeValue, TimeUnit.MINUTES)
        )
      )
    }
  }

  test("pass validation for a second granularity value") {
    val secondGranularity = for {
      second <- Gen.choose[Long](1, 500)
    } yield new DateTime(
      "2014-3-26T01:10:10.000Z").getMillis + java.util.concurrent.TimeUnit.SECONDS
      .toMillis(second)

    forAll(secondGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularitySecondsExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  test("fail validation for an invalid second granularity value") {
    val secondInvalidGranularity = for {
      millis <- Gen.choose[Long](1, 999)
    } yield new DateTime("2014-3-26T02:20:20.000Z").getMillis + millis

    forAll(secondInvalidGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularitySecondsExample](dateTimeValue) should equal(
        Invalid(
          errorMessage[TimeGranularitySecondsExample](dateTimeValue),
          ErrorCode.InvalidTimeGranularity(dateTimeValue, TimeUnit.SECONDS)
        )
      )
    }
  }

  test("pass validation for a millisecond granularity value") {
    val millisGranularity = for {
      millis <- Gen.choose[Long](1, 1000)
    } yield new DateTime("2014-3-26T01:10:10.001Z").getMillis + millis

    forAll(millisGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityMillisecondsExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  test("pass validation for a microsecond granularity value") {
    val microGranularity = for {
      micro <- Gen.choose[Long](1, 1000)
    } yield new DateTime(
      "2014-3-26T01:10:10.001Z").getMillis + java.util.concurrent.TimeUnit.MICROSECONDS
      .toMillis(micro)

    forAll(microGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityMicrosecondsExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  test("pass validation for a nanosecond granularity value") {
    val nanoGranularity = for {
      nano <- Gen.choose[Long](1, 1000)
    } yield new DateTime(
      "2014-3-26T01:10:10.001Z").getMillis + java.util.concurrent.TimeUnit.NANOSECONDS
      .toMillis(nano)

    forAll(nanoGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityNanosecondsExample](dateTimeValue).isInstanceOf[Valid] shouldBe true
    }
  }

  private def validate[T: Manifest](value: Any): ValidationResult =
    super.validate(manifest[T].runtimeClass, "timeValue", classOf[TimeGranularity], value)

  private def errorMessage[T: Manifest](value: DateTime): String = {
    val annotation =
      getValidationAnnotation(manifest[T].runtimeClass, "timeValue", classOf[TimeGranularity])

    TimeGranularityConstraintValidator.errorMessage(messageResolver, annotation.value(), value)
  }
}
