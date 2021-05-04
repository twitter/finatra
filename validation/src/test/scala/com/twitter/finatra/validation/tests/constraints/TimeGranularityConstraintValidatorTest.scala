package com.twitter.finatra.validation.tests.constraints

import com.twitter.finatra.validation.{ConstraintValidatorTest, ErrorCode}
import com.twitter.finatra.validation.constraints.TimeGranularity
import com.twitter.finatra.validation.constraints.TimeGranularityConstraintValidator.singularize
import com.twitter.finatra.validation.tests.caseclasses._
import com.twitter.util.validation.conversions.ConstraintViolationOps._
import jakarta.validation.ConstraintViolation
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scala.util.control.NonFatal

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
      validate[TimeGranularityDaysExample](dateTimeValue).isEmpty shouldBe true
    }
  }

  test("fail validation for an invalid day granularity value") {
    val dayInvalidGranularity = for {
      hour <- Gen.choose[Long](1, 1000).filter(_ % 24 != 0).filter(_ > 0)
    } yield new DateTime("2014-3-26T00:00:00Z").getMillis +
      java.util.concurrent.TimeUnit.HOURS.toMillis(hour)

    forAll(dayInvalidGranularity) { millisValue =>
      try {
        val dateTimeValue = new DateTime(millisValue)
        val violations = validate[TimeGranularityDaysExample](dateTimeValue)
        violations.size should equal(1)
        violations.head.getPropertyPath.toString should equal("timeValue")
        violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
        val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidTimeGranularity])
        payload.isDefined should be(true)
        payload.get should equal(
          ErrorCode.InvalidTimeGranularity(
            violations.head.getInvalidValue.asInstanceOf[DateTime],
            TimeUnit.DAYS
          )
        )
      } catch {
        case NonFatal(_: IllegalArgumentException) => //do nothing
      }
    }
  }

  test("pass validation for a hour granularity value") {
    val hourGranularity = for {
      hour <- Gen.choose[Long](1, 500)
    } yield new DateTime("2014-3-26T01:00:00Z").getMillis + java.util.concurrent.TimeUnit.HOURS
      .toMillis(hour)

    forAll(hourGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityHoursExample](dateTimeValue).isEmpty shouldBe true
    }
  }

  test("fail validation for an invalid hour granularity value") {
    val hourInvalidGranularity = for {
      min <- Gen.choose[Long](1, 1000).filter(_ % 60 != 0)
    } yield new DateTime("2014-3-26T02:00:00Z").getMillis + java.util.concurrent.TimeUnit.MINUTES
      .toMillis(min)

    forAll(hourInvalidGranularity) { millisValue =>
      try {
        val dateTimeValue = new DateTime(millisValue)
        val violations = validate[TimeGranularityHoursExample](dateTimeValue)
        violations.size should equal(1)
        violations.head.getPropertyPath.toString should equal("timeValue")
        violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
        val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidTimeGranularity])
        payload.isDefined should be(true)
        payload.get should equal(
          ErrorCode.InvalidTimeGranularity(
            violations.head.getInvalidValue.asInstanceOf[DateTime],
            TimeUnit.HOURS
          )
        )
      } catch {
        case NonFatal(_: IllegalArgumentException) => //do nothing
      }
    }
  }

  test("pass validation for a minute granularity value") {
    val minGranularity = for {
      min <- Gen.choose[Long](1, 500)
    } yield new DateTime("2014-3-26T01:10:00Z").getMillis + java.util.concurrent.TimeUnit.MINUTES
      .toMillis(min)

    forAll(minGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityMinutesExample](dateTimeValue).isEmpty shouldBe true
    }
  }

  test("fail validation for an invalid minute granularity value") {
    val minInvalidGranularity = for {
      second <- Gen.choose[Long](1, 1000).filter(_ % 60 != 0)
    } yield new DateTime("2014-3-26T02:20:00Z").getMillis + java.util.concurrent.TimeUnit.SECONDS
      .toMillis(second)

    forAll(minInvalidGranularity) { millisValue =>
      try {
        val dateTimeValue = new DateTime(millisValue)
        val violations = validate[TimeGranularityMinutesExample](dateTimeValue)
        violations.size should equal(1)
        violations.head.getPropertyPath.toString should equal("timeValue")
        violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
        val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidTimeGranularity])
        payload.isDefined should be(true)
        payload.get should equal(
          ErrorCode.InvalidTimeGranularity(
            violations.head.getInvalidValue.asInstanceOf[DateTime],
            TimeUnit.MINUTES)
        )
      } catch {
        case NonFatal(_: IllegalArgumentException) => //do nothing
      }
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
      validate[TimeGranularitySecondsExample](dateTimeValue).isEmpty shouldBe true
    }
  }

  test("fail validation for an invalid second granularity value") {
    val secondInvalidGranularity = for {
      millis <- Gen.choose[Long](1, 999)
    } yield new DateTime("2014-3-26T02:20:20.000Z").getMillis + millis

    forAll(secondInvalidGranularity) { millisValue =>
      try {
        val dateTimeValue = new DateTime(millisValue)
        val violations = validate[TimeGranularitySecondsExample](dateTimeValue)
        violations.size should equal(1)
        violations.head.getPropertyPath.toString should equal("timeValue")
        violations.head.getMessage should be(errorMessage(violations.head.getInvalidValue))
        val payload = violations.head.getDynamicPayload(classOf[ErrorCode.InvalidTimeGranularity])
        payload.isDefined should be(true)
        payload.get should equal(
          ErrorCode.InvalidTimeGranularity(
            violations.head.getInvalidValue.asInstanceOf[DateTime],
            TimeUnit.SECONDS)
        )
      } catch {
        case NonFatal(_: IllegalArgumentException) => //do nothing
      }
    }
  }

  test("pass validation for a millisecond granularity value") {
    val millisGranularity = for {
      millis <- Gen.choose[Long](1, 1000)
    } yield new DateTime("2014-3-26T01:10:10.001Z").getMillis + millis

    forAll(millisGranularity) { millisValue =>
      val dateTimeValue = new DateTime(millisValue)
      validate[TimeGranularityMillisecondsExample](dateTimeValue).isEmpty shouldBe true
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
      validate[TimeGranularityMicrosecondsExample](dateTimeValue).isEmpty shouldBe true
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
      validate[TimeGranularityNanosecondsExample](dateTimeValue).isEmpty shouldBe true
    }
  }

  private def validate[T: Manifest](value: Any): Set[ConstraintViolation[T]] = {
    super.validate[TimeGranularity, T](manifest[T].runtimeClass, "timeValue", value)
  }

  private def errorMessage[T: Manifest](value: Any): String = {
    val annotation =
      getValidationAnnotation(manifest[T].runtimeClass, "timeValue", classOf[TimeGranularity])

    s"[${value.toString}] is not ${singularize(annotation.value())} granularity"
  }
}
