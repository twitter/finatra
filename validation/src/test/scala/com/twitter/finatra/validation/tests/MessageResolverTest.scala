package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.MessageResolver
import com.twitter.finatra.validation.constraints._
import com.twitter.inject.Test
import org.joda.time.DateTime

object MessageResolverTest {
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

class MessageResolverTest extends Test {
  import MessageResolverTest._

  val messageResolver = new MessageResolver
  val validationProperties = messageResolver.validationProperties

  test("MessageResolver loads resources properly") {
    val countryCodeMsg =
      messageResolver.resolve(classOf[CountryCode], invalidValidators.countryCode)
    val expectedCountryCodeMsg = errorMessage.countryCode.format(invalidValidators.countryCode)
    countryCodeMsg should equal(expectedCountryCodeMsg)

    val countryCodeMsg1 =
      messageResolver.resolve[CountryCode](invalidValidators.countryCode)
    val expectedCountryCodeMsg1 = errorMessage.countryCode.format(invalidValidators.countryCode)
    countryCodeMsg1 should equal(expectedCountryCodeMsg1)

    val futureTimeMsg =
      messageResolver.resolve(classOf[FutureTime], invalidValidators.futureTime)
    val expectedFutureTimeMsg = errorMessage.futureTime.format(invalidValidators.futureTime)
    futureTimeMsg should equal(expectedFutureTimeMsg)

    val futureTimeMsg1 =
      messageResolver.resolve[FutureTime](invalidValidators.futureTime)
    val expectedFutureTimeMsg1 = errorMessage.futureTime.format(invalidValidators.futureTime)
    futureTimeMsg1 should equal(expectedFutureTimeMsg1)

    val maxMsg =
      messageResolver.resolve(classOf[Max], invalidValidators.max._1, invalidValidators.max._2)
    val expectedMaxMsg = errorMessage.max.format(invalidValidators.max._1, invalidValidators.max._2)
    maxMsg should equal(expectedMaxMsg)

    val maxMsg1 =
      messageResolver.resolve[Max](invalidValidators.max._1, invalidValidators.max._2)
    val expectedMaxMsg1 =
      errorMessage.max.format(invalidValidators.max._1, invalidValidators.max._2)
    maxMsg1 should equal(expectedMaxMsg1)

    val minMsg =
      messageResolver.resolve(classOf[Min], invalidValidators.min._1, invalidValidators.min._2)
    val expectedMinMsg = errorMessage.min.format(invalidValidators.min._1, invalidValidators.min._2)
    minMsg should equal(expectedMinMsg)

    val minMsg1 =
      messageResolver.resolve[Min](invalidValidators.min._1, invalidValidators.min._2)
    val expectedMinMsg1 =
      errorMessage.min.format(invalidValidators.min._1, invalidValidators.min._2)
    minMsg1 should equal(expectedMinMsg1)

    val notEmptyMsg = messageResolver.resolve(classOf[NotEmpty])
    val expectedNotEmptyMsg = errorMessage.notEmpty
    notEmptyMsg should equal(expectedNotEmptyMsg)

    val notEmptyMsg1 = messageResolver.resolve[NotEmpty]()
    val expectedNotEmptyMsg1 = errorMessage.notEmpty
    notEmptyMsg1 should equal(expectedNotEmptyMsg1)

    val oneOfMsg = messageResolver.resolve(
      classOf[OneOf],
      invalidValidators.oneOf._1,
      invalidValidators.oneOf._2)
    val expectedOneOfMsg =
      errorMessage.oneOf.format(invalidValidators.oneOf._1, invalidValidators.oneOf._2)
    oneOfMsg should equal(expectedOneOfMsg)

    val oneOfMsg1 =
      messageResolver.resolve[OneOf](invalidValidators.oneOf._1, invalidValidators.oneOf._2)
    val expectedOneOfMsg1 =
      errorMessage.oneOf.format(invalidValidators.oneOf._1, invalidValidators.oneOf._2)
    oneOfMsg1 should equal(expectedOneOfMsg1)

    val pastTimeMsg =
      messageResolver.resolve(classOf[PastTime], invalidValidators.pastTime)
    val expectedPastTimeMsg = errorMessage.pastTime.format(invalidValidators.pastTime)
    pastTimeMsg should equal(expectedPastTimeMsg)

    val pastTimeMsg1 =
      messageResolver.resolve[PastTime](invalidValidators.pastTime)
    val expectedPastTimeMsg1 = errorMessage.pastTime.format(invalidValidators.pastTime)
    pastTimeMsg1 should equal(expectedPastTimeMsg1)

    val rangeMsg = messageResolver.resolve(
      classOf[Range],
      invalidValidators.range._1,
      invalidValidators.range._2,
      invalidValidators.range._3)
    val expectedRangeMsg = errorMessage.range.format(
      invalidValidators.range._1,
      invalidValidators.range._2,
      invalidValidators.range._3)
    rangeMsg should equal(expectedRangeMsg)

    val rangeMsg1 = messageResolver.resolve[Range](
      invalidValidators.range._1,
      invalidValidators.range._2,
      invalidValidators.range._3)
    val expectedRangeMsg1 = errorMessage.range.format(
      invalidValidators.range._1,
      invalidValidators.range._2,
      invalidValidators.range._3)
    rangeMsg1 should equal(expectedRangeMsg1)

    val sizeMsg = messageResolver.resolve(
      classOf[Size],
      invalidValidators.size._1,
      invalidValidators.size._2,
      invalidValidators.size._3)
    val expectedSizeMsg = errorMessage.size.format(
      invalidValidators.size._1,
      invalidValidators.size._2,
      invalidValidators.size._3)
    sizeMsg should equal(expectedSizeMsg)

    val sizeMsg1 = messageResolver.resolve[Size](
      invalidValidators.size._1,
      invalidValidators.size._2,
      invalidValidators.size._3)
    val expectedSizeMsg1 = errorMessage.size.format(
      invalidValidators.size._1,
      invalidValidators.size._2,
      invalidValidators.size._3)
    sizeMsg1 should equal(expectedSizeMsg1)

    val timeGranularityMsg = messageResolver.resolve(
      classOf[TimeGranularity],
      invalidValidators.timeGranularity._1,
      invalidValidators.timeGranularity._2)
    val expectedTimeGranularityMsg = errorMessage.timeGranularity
      .format(invalidValidators.timeGranularity._1, invalidValidators.timeGranularity._2)
    timeGranularityMsg should equal(expectedTimeGranularityMsg)

    val timeGranularityMsg1 = messageResolver.resolve[TimeGranularity](
      invalidValidators.timeGranularity._1,
      invalidValidators.timeGranularity._2)
    val expectedTimeGranularityMsg1 = errorMessage.timeGranularity
      .format(invalidValidators.timeGranularity._1, invalidValidators.timeGranularity._2)
    timeGranularityMsg1 should equal(expectedTimeGranularityMsg1)

    val uuidMsg = messageResolver.resolve(classOf[UUID], invalidValidators.uuid)
    val expectedUuidMsg = errorMessage.uuid.format(invalidValidators.uuid)
    uuidMsg should equal(expectedUuidMsg)

    val uuidMsg1 = messageResolver.resolve[UUID](invalidValidators.uuid)
    val expectedUuidMsg1 = errorMessage.uuid.format(invalidValidators.uuid)
    uuidMsg1 should equal(expectedUuidMsg1)

    val patternMsg = messageResolver.resolve(
      classOf[Pattern],
      invalidValidators.pattern._1,
      invalidValidators.pattern._2)
    val expectedPatternMsg =
      errorMessage.pattern.format(invalidValidators.pattern._1, invalidValidators.pattern._2)
    patternMsg should equal(expectedPatternMsg)

    val patternMsg1 =
      messageResolver.resolve[Pattern](invalidValidators.pattern._1, invalidValidators.pattern._2)
    val expectedPatternMsg1 =
      errorMessage.pattern.format(invalidValidators.pattern._1, invalidValidators.pattern._2)
    patternMsg1 should equal(expectedPatternMsg1)
  }

}
