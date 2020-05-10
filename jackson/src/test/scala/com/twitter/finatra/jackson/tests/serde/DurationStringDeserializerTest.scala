package com.twitter.finatra.jackson.tests.serde

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.caseclass.exceptions.CaseClassMappingException
import com.twitter.inject.Test
import com.twitter.util.Duration

case class CaseClassWithDuration(d: Duration)

class DurationStringDeserializerTest extends Test {

  private[this] val mapper = ScalaObjectMapper()

  test("deserializes values") {
    Seq(
      " 1.second" -> 1.second,
      "+1.second" -> 1.second,
      "-1.second" -> -1.second,
      "1.SECOND" -> 1.second,
      "1.day - 1.second" -> (1.day - 1.second),
      "1.day" -> 1.day,
      "1.microsecond" -> 1.microsecond,
      "1.millisecond" -> 1.millisecond,
      "1.second" -> 1.second,
      "1.second+1.minute  +  1.day" -> (1.second + 1.minute + 1.day),
      "1.second+1.second" -> 2.seconds,
      "2.hours" -> 2.hours,
      "3.days" -> 3.days,
      "321.nanoseconds" -> 321.nanoseconds,
      "65.minutes" -> 65.minutes,
      "876.milliseconds" -> 876.milliseconds,
      "98.seconds" -> 98.seconds,
      "Duration.Bottom" -> Duration.Bottom,
      "Duration.Top" -> Duration.Top,
      "Duration.Undefined" -> Duration.Undefined,
      "duration.TOP" -> Duration.Top
    ) foreach {
      case (s, d) =>
        mapper.parse[CaseClassWithDuration](s"""{"d":"$s"}""") should equal(
          CaseClassWithDuration(d))
    }

    intercept[CaseClassMappingException] {
      mapper.parse[CaseClassWithDuration]("{}")
    }
  }
}
