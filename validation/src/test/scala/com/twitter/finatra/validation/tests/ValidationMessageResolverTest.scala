package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.ValidationMessageResolver
import com.twitter.finatra.validation._
import com.twitter.inject.Test
import org.joda.time.DateTime

object ValidationMessageResolverTest {
  object invalidValidators {
    val countryCode: String = "NONONO"
    val futureTime: DateTime = new DateTime(1234)
    val max: (Long, Long) = (1L, 2L)
    val min: (Long, Long) = (1L, 0L)
    val oneOf: (Int, Set[Int]) = (1, Set(2))
    val pastTime: DateTime = new DateTime(8888)
    val range: (Int, Int, Int) = (1, 2, 5)
    val size: (Int, Int, Int) = (1, 2, 5)
    val timeGranularity: (DateTime, DateTime) = (new DateTime(1234), new DateTime(12345))
    val uuid: String = "7777"
    val pattern: (String, String) = ("value", "regex")
  }

  object errorMessage {
    val countryCode = "[%s] is not a valid country code"
    val futureTime = "[%s] is not in the future"
    val max = "[%s] is not less than or equal to %s"
    val min = "[%s] is not greater than or equal to %s"
    val notEmpty = "cannot be empty"
    val oneOf = "[%s] is not one of [%s]"
    val pastTime = "[%s] is not in the past"
    val range = "[%s] is not between %s and %s"
    val size = "size [%s] is not between %s and %s"
    val timeGranularity = "[%s] is not %s granularity"
    val uuid = "[%s] is not a valid UUID"
    val pattern = "[%s] does not match regex %s"
  }
}

class ValidationMessageResolverTest extends Test {
  import ValidationMessageResolverTest._

  val validationMessageResolver = new ValidationMessageResolver
  val validationProperties = validationMessageResolver.validationProperties

  test("ValidationMessageResolver loads resources properly") {
    val countryCodeMsg =
      validationMessageResolver.resolve(classOf[CountryCode], invalidValidators.countryCode)
    val expectedCountryCodeMsg = errorMessage.countryCode.format(invalidValidators.countryCode)
    countryCodeMsg should equal(expectedCountryCodeMsg)

    val futureTimeMsg =
      validationMessageResolver.resolve(classOf[FutureTime], invalidValidators.futureTime)
    val expectedFutureTimeMsg = errorMessage.futureTime.format(invalidValidators.futureTime)
    futureTimeMsg should equal(expectedFutureTimeMsg)

    val maxMsg = validationMessageResolver.resolve(
      classOf[Max],
      invalidValidators.max._1,
      invalidValidators.max._2)
    val expectedMaxMsg = errorMessage.max.format(invalidValidators.max._1, invalidValidators.max._2)
    maxMsg should equal(expectedMaxMsg)

    val minMsg = validationMessageResolver.resolve(
      classOf[Min],
      invalidValidators.min._1,
      invalidValidators.min._2)
    val expectedMinMsg = errorMessage.min.format(invalidValidators.min._1, invalidValidators.min._2)
    minMsg should equal(expectedMinMsg)

    val notEmptyMsg = validationMessageResolver.resolve(classOf[NotEmpty])
    val expectedNotEmptyMsg = errorMessage.notEmpty
    notEmptyMsg should equal(expectedNotEmptyMsg)

    val oneOfMsg = validationMessageResolver.resolve(
      classOf[OneOf],
      invalidValidators.oneOf._1,
      invalidValidators.oneOf._2)
    val expectedOneOfMsg =
      errorMessage.oneOf.format(invalidValidators.oneOf._1, invalidValidators.oneOf._2)
    oneOfMsg should equal(expectedOneOfMsg)

    val pastTimeMsg =
      validationMessageResolver.resolve(classOf[PastTime], invalidValidators.pastTime)
    val expectedPastTimeMsg = errorMessage.pastTime.format(invalidValidators.pastTime)
    pastTimeMsg should equal(expectedPastTimeMsg)

    val rangeMsg = validationMessageResolver.resolve(
      classOf[Range],
      invalidValidators.range._1,
      invalidValidators.range._2,
      invalidValidators.range._3)
    val expectedRangeMsg = errorMessage.range.format(
      invalidValidators.range._1,
      invalidValidators.range._2,
      invalidValidators.range._3)
    rangeMsg should equal(expectedRangeMsg)

    val sizeMsg = validationMessageResolver.resolve(
      classOf[Size],
      invalidValidators.size._1,
      invalidValidators.size._2,
      invalidValidators.size._3)
    val expectedSizeMsg = errorMessage.size.format(
      invalidValidators.size._1,
      invalidValidators.size._2,
      invalidValidators.size._3)
    sizeMsg should equal(expectedSizeMsg)

    val timeGranularityMsg = validationMessageResolver.resolve(
      classOf[TimeGranularity],
      invalidValidators.timeGranularity._1,
      invalidValidators.timeGranularity._2)
    val expectedTimeGranularityMsg = errorMessage.timeGranularity
      .format(invalidValidators.timeGranularity._1, invalidValidators.timeGranularity._2)
    timeGranularityMsg should equal(expectedTimeGranularityMsg)

    val uuidMsg = validationMessageResolver.resolve(classOf[UUID], invalidValidators.uuid)
    val expectedUuidMsg = errorMessage.uuid.format(invalidValidators.uuid)
    uuidMsg should equal(expectedUuidMsg)

    val patternMsg = validationMessageResolver.resolve(
      classOf[Pattern],
      invalidValidators.pattern._1,
      invalidValidators.pattern._2)
    val expectedPatternMsg =
      errorMessage.pattern.format(invalidValidators.pattern._1, invalidValidators.pattern._2)
    patternMsg should equal(expectedPatternMsg)
  }

}
