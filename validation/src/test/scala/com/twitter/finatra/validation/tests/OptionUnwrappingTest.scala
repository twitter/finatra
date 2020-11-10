package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.Validator
import com.twitter.finatra.validation.tests.caseclasses._
import com.twitter.inject.Test
import com.twitter.inject.conversions.time._
import org.joda.time.DateTime

/**
 * Options are "unwrapped" by default for validation, meaning any supplied constraint
 * annotation is applied to the contained type/value of the Option. The unwrapping
 * happens through the Validator and not in the individual ConstraintValidator thus we
 * test though a Validator here and why these tests are not in the individual ConstraintValidator
 * tests.
 */
class OptionUnwrappingTest extends Test with AssertValidation {

  override protected val validator: Validator = Validator.builder.build()

  test("AssertFalse") {
    assertValidation(
      obj = AssertFalseOptionExample(Some(true)),
      withErrors = Seq(
        "boolValue: [true] is not false"
      )
    )
    assertValidation(obj = AssertFalseOptionExample(None))
  }

  test("AssertTrue") {
    assertValidation(
      obj = AssertTrueOptionExample(Some(false)),
      withErrors = Seq(
        "boolValue: [false] is not true"
      )
    )
    assertValidation(obj = AssertTrueOptionExample(None))
  }

  test("CountryCode") {
    assertValidation(
      obj = CountryCodeOptionExample(Some("11")),
      withErrors = Seq(
        "countryCode: [11] is not a valid country code"
      )
    )
    assertValidation(obj = CountryCodeOptionExample(None))

    assertValidation(
      obj = CountryCodeOptionSeqExample(Some(Seq("11", "22"))),
      withErrors = Seq(
        "countryCode: [11,22] is not a valid country code"
      )
    )
    assertValidation(obj = CountryCodeOptionSeqExample(None))

    assertValidation(
      obj = CountryCodeOptionArrayExample(Some(Array("11", "22"))),
      withErrors = Seq(
        "countryCode: [11,22] is not a valid country code"
      )
    )
    assertValidation(obj = CountryCodeOptionArrayExample(None))

    assertValidation(
      obj = CountryCodeOptionInvalidTypeExample(Some(12345L)),
      withErrors = Seq(
        "countryCode: [12345] is not a valid country code"
      )
    )
    assertValidation(obj = CountryCodeOptionInvalidTypeExample(None))
  }

  test("FutureTime") {
    val notFutureTime: DateTime = DateTime.now().minusWeeks(5)
    assertValidation(
      obj = FutureOptionExample(Some(notFutureTime)),
      withErrors = Seq(
        "dateTime: [%s] is not in the future".format(notFutureTime.utcIso8601)
      )
    )
    assertValidation(obj = FutureOptionExample(None))
  }

  test("Max") {
    assertValidation(
      obj = MaxOptionIntExample(Some(1)),
      withErrors = Seq(
        "numberValue: [1] is not less than or equal to 0"
      )
    )
    assertValidation(MaxOptionIntExample(None))

    assertValidation(
      obj = MaxOptionDoubleExample(Some(1.0d)),
      withErrors = Seq(
        "numberValue: [1.0] is not less than or equal to 0"
      )
    )
    assertValidation(MaxOptionDoubleExample(None))

    assertValidation(
      obj = MaxOptionFloatExample(Some(1.0f)),
      withErrors = Seq(
        "numberValue: [1.0] is not less than or equal to 0"
      )
    )
    assertValidation(MaxOptionFloatExample(None))

    assertValidation(
      obj = MaxOptionLongExample(Some(1L)),
      withErrors = Seq(
        "numberValue: [1] is not less than or equal to 0"
      )
    )
    assertValidation(MaxOptionLongExample(None))

    assertValidation(
      obj = MaxOptionBigIntExample(Some(BigInt(1))),
      withErrors = Seq(
        "numberValue: [1] is not less than or equal to 0"
      )
    )
    assertValidation(MaxOptionBigIntExample(None))

    assertValidation(
      obj = MaxOptionBigDecimalExample(Some(BigDecimal(1.0d))),
      withErrors = Seq(
        "numberValue: [1.0] is not less than or equal to 0"
      )
    )
    assertValidation(MaxOptionBigDecimalExample(None))

    val maxOptionSeq = Option(for (_ <- 1 to 102) yield 9)
    assertValidation(
      obj = MaxOptionSeqExample(maxOptionSeq),
      withErrors = Seq(
        "numberValue: [102] is not less than or equal to 100"
      )
    )
    assertValidation(MaxOptionSeqExample(None))

    val maxOptionMap = Option((for {
      i <- 1 to 102
    } yield { i.toString -> 9 }).toMap)
    assertValidation(
      obj = MaxOptionMapExample(maxOptionMap),
      withErrors = Seq(
        "numberValue: [102] is not less than or equal to 100"
      )
    )
    assertValidation(MaxOptionMapExample(None))

    val maxOptionArray = Option((for (_ <- 1 to 102) yield 9).toArray)
    assertValidation(
      obj = MaxOptionArrayExample(maxOptionArray),
      withErrors = Seq(
        "numberValue: [102] is not less than or equal to 100"
      )
    )
    assertValidation(MaxOptionArrayExample(None))

    val e = intercept[IllegalArgumentException] {
      assertValidation(MaxOptionInvalidTypeExample(Some("foobar")))
    }
    e.getMessage should be(
      "Class [java.lang.String] is not supported by com.twitter.finatra.validation.constraints.MaxConstraintValidator")
    assertValidation(MaxOptionInvalidTypeExample(None))
  }

  test("Min") {
    assertValidation(
      obj = MinOptionIntExample(Some(0)),
      withErrors = Seq(
        "numberValue: [0] is not greater than or equal to 1"
      )
    )
    assertValidation(MinOptionIntExample(None))

    assertValidation(
      obj = MinOptionLongExample(Some(0L)),
      withErrors = Seq(
        "numberValue: [0] is not greater than or equal to 1"
      )
    )
    assertValidation(MinOptionLongExample(None))

    assertValidation(
      obj = MinOptionDoubleExample(Some(0.0d)),
      withErrors = Seq(
        "numberValue: [0.0] is not greater than or equal to 1"
      )
    )
    assertValidation(MinOptionDoubleExample(None))

    assertValidation(
      obj = MinOptionFloatExample(Some(0.0f)),
      withErrors = Seq(
        "numberValue: [0.0] is not greater than or equal to 1"
      )
    )
    assertValidation(MinOptionFloatExample(None))

    assertValidation(
      obj = MinOptionBigIntExample(Some(BigInt(0))),
      withErrors = Seq(
        "numberValue: [0] is not greater than or equal to 1"
      )
    )
    assertValidation(MinOptionBigIntExample(None))

    assertValidation(
      obj = MinOptionBigDecimalExample(Some(BigDecimal(0.0d))),
      withErrors = Seq(
        "numberValue: [0.0] is not greater than or equal to 1"
      )
    )
    assertValidation(MinOptionBigDecimalExample(None))

    assertValidation(
      obj = MinOptionSeqExample(Some(Seq.empty)),
      withErrors = Seq(
        "numberValue: [0] is not greater than or equal to 10"
      )
    )
    assertValidation(MinOptionSeqExample(None))

    assertValidation(
      obj = MinOptionMapExample(Some(Map.empty)),
      withErrors = Seq(
        "numberValue: [0] is not greater than or equal to 10"
      )
    )
    assertValidation(MinOptionMapExample(None))

    assertValidation(
      obj = MinOptionArrayExample(Some(Array.empty)),
      withErrors = Seq(
        "numberValue: [0] is not greater than or equal to 10"
      )
    )
    assertValidation(MinOptionArrayExample(None))

    val e = intercept[IllegalArgumentException] {
      assertValidation(MinOptionInvalidTypeExample(Some("10")))
    }
    e.getMessage should be(
      "Class [java.lang.String] is not supported by com.twitter.finatra.validation.constraints.MinConstraintValidator")
    assertValidation(MinOptionInvalidTypeExample(None))
  }

  test("NotEmpty") {
    assertValidation(
      obj = NotEmptyOptionExample(Some("")),
      withErrors = Seq(
        "stringValue: cannot be empty"
      )
    )
    assertValidation(NotEmptyOptionExample(None))

    assertValidation(
      obj = NotEmptyOptionArrayExample(Some(Array.empty)),
      withErrors = Seq(
        "stringValue: cannot be empty"
      )
    )
    assertValidation(NotEmptyOptionArrayExample(None))

    assertValidation(
      obj = NotEmptyOptionMapExample(Some(Map.empty)),
      withErrors = Seq(
        "stringValue: cannot be empty"
      )
    )
    assertValidation(NotEmptyOptionMapExample(None))

    assertValidation(
      obj = NotEmptyOptionSeqExample(Some(Seq.empty)),
      withErrors = Seq(
        "stringValue: cannot be empty"
      )
    )
    assertValidation(NotEmptyOptionSeqExample(None))

    val e = intercept[IllegalArgumentException] {
      assertValidation(NotEmptyOptionInvalidTypeExample(Some(10L)))
    }
    e.getMessage should be(
      "Class [java.lang.Long] is not supported by com.twitter.finatra.validation.constraints.NotEmptyConstraintValidator")
    assertValidation(NotEmptyOptionInvalidTypeExample(None))
  }

  test("OneOf") {
    assertValidation(
      obj = OneOfOptionExample(Some("g")),
      withErrors = Seq(
        "enumValue: [g] is not one of [a,B,c]"
      )
    )
    assertValidation(OneOfOptionExample(None))

    assertValidation(
      obj = OneOfOptionSeqExample(Some(Seq("g", "h", "i"))),
      withErrors = Seq(
        "enumValue: [g,h,i] is not one of [a,B,c]"
      )
    )
    assertValidation(OneOfOptionSeqExample(None))

    assertValidation(
      obj = OneOfOptionInvalidTypeExample(Some(10L)),
      withErrors = Seq(
        "enumValue: [10] is not one of [a,B,c]"
      )
    )
    assertValidation(OneOfOptionInvalidTypeExample(None))
  }

  test("PastTime") {
    val notPastTime: DateTime = DateTime.now().plusWeeks(5)
    assertValidation(
      obj = PastOptionExample(Some(notPastTime)),
      withErrors = Seq(
        "dateTime: [%s] is not in the past".format(notPastTime.utcIso8601)
      )
    )
    assertValidation(PastOptionExample(None))
  }

  test("Pattern") {
    assertValidation(
      obj = NumberPatternOptionExample(Some("words")),
      withErrors = Seq(
        "stringValue: [words] does not match regex [0-9]+"
      )
    )
    assertValidation(NumberPatternOptionExample(None))

    assertValidation(
      obj = NumberPatternOptionArrayExample(Some(Array("22", "words"))),
      withErrors = Seq(
        "stringValue: [22,words] does not match regex [0-9]+"
      )
    )
    assertValidation(NumberPatternOptionArrayExample(None))

    // empty regex matches everything
    assertValidation(EmptyPatternOptionExample(Some("notEmpty")))
    assertValidation(EmptyPatternOptionExample(None))

    assertValidation(
      obj = InvalidPatternOptionExample(Some("notEmpty")),
      withErrors = Seq(
        "stringValue: java.util.regex.PatternSyntaxException"
      )
    )
    assertValidation(InvalidPatternOptionExample(None))
  }

  test("Range") {
    assertValidation(
      obj = RangeOptionIntExample(Some(55)),
      withErrors = Seq(
        "pointValue: [55] is not between 1 and 50"
      )
    )
    assertValidation(RangeOptionIntExample(None))

    assertValidation(
      obj = RangeOptionLongExample(Some(55L)),
      withErrors = Seq(
        "pointValue: [55] is not between 1 and 50"
      )
    )
    assertValidation(RangeOptionLongExample(None))

    assertValidation(
      obj = RangeOptionDoubleExample(Some(55.0d)),
      withErrors = Seq(
        "pointValue: [55.0] is not between 1 and 50"
      )
    )
    assertValidation(RangeOptionDoubleExample(None))

    assertValidation(
      obj = RangeOptionFloatExample(Some(55.0f)),
      withErrors = Seq(
        "pointValue: [55.0] is not between 1 and 50"
      )
    )
    assertValidation(RangeOptionFloatExample(None))

    assertValidation(
      obj = RangeOptionBigDecimalExample(Some(BigDecimal(55.0d))),
      withErrors = Seq(
        "pointValue: [55.0] is not between 1 and 50"
      )
    )
    assertValidation(RangeOptionBigDecimalExample(None))

    assertValidation(
      obj = RangeOptionBigIntExample(Some(BigInt(55))),
      withErrors = Seq(
        "pointValue: [55] is not between 1 and 50"
      )
    )
    assertValidation(RangeOptionBigIntExample(None))

    val e = intercept[IllegalArgumentException] {
      assertValidation(RangeOptionInvalidTypeExample(Some("hello")))
    }
    e.getMessage should be(
      "Class [java.lang.String] is not supported by com.twitter.finatra.validation.constraints.RangeConstraintValidator")
    assertValidation(RangeOptionInvalidTypeExample(None))
  }

  test("Size") {
    assertValidation(
      obj = SizeOptionArrayExample(Some(Array.empty)),
      withErrors = Seq(
        "sizeValue: size [0] is not between 10 and 50"
      )
    )
    assertValidation(SizeOptionArrayExample(None))

    assertValidation(
      obj = SizeOptionSeqExample(Some(Seq.empty)),
      withErrors = Seq(
        "sizeValue: size [0] is not between 10 and 50"
      )
    )
    assertValidation(SizeOptionSeqExample(None))

    assertValidation(
      obj = SizeOptionMapExample(Some(Map.empty)),
      withErrors = Seq(
        "sizeValue: size [0] is not between 10 and 50"
      )
    )
    assertValidation(SizeOptionMapExample(None))

    assertValidation(
      obj = SizeOptionStringExample(Some("")),
      withErrors = Seq(
        "sizeValue: size [0] is not between 10 and 140"
      )
    )
    assertValidation(SizeOptionStringExample(None))

    val e = intercept[IllegalArgumentException] {
      assertValidation(SizeOptionInvalidTypeExample(Some(10)))
    }
    e.getMessage should be(
      "Class [java.lang.Integer] is not supported by com.twitter.finatra.validation.constraints.SizeConstraintValidator")
    assertValidation(SizeOptionInvalidTypeExample(None))
  }

  test("TimeGranularity") {
    val dateTime = new DateTime("2014-3-26T01:10:10.001Z")

    val nano = java.util.concurrent.TimeUnit.NANOSECONDS.toNanos(dateTime.getMillis)
    assertValidation(TimeGranularityOptionNanosecondsExample(Some(new DateTime(nano))))
    assertValidation(TimeGranularityOptionNanosecondsExample(None))

    val micro = java.util.concurrent.TimeUnit.MICROSECONDS.toMicros(dateTime.getMillis)
    assertValidation(TimeGranularityOptionMicrosecondsExample(Some(new DateTime(micro))))
    assertValidation(TimeGranularityOptionMicrosecondsExample(None))

    val millis = java.util.concurrent.TimeUnit.MILLISECONDS.toMillis(dateTime.getMillis)
    assertValidation(TimeGranularityOptionMillisecondsExample(Some(new DateTime(millis))))
    assertValidation(TimeGranularityOptionMillisecondsExample(None))

    val notSeconds = java.util.concurrent.TimeUnit.MILLISECONDS.toMillis(dateTime.getMillis)
    assertValidation(
      obj = TimeGranularityOptionSecondsExample(Some(new DateTime(notSeconds))),
      withErrors = Seq(
        "timeValue: [2014-03-26T01:10:10.001Z] is not second granularity"
      )
    )
    assertValidation(TimeGranularityOptionSecondsExample(None))

    val notMinutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMillis(dateTime.getMillis)
    assertValidation(
      obj = TimeGranularityOptionMinutesExample(Some(new DateTime(notMinutes))),
      withErrors = Seq(
        "timeValue: [2014-03-26T01:10:10.001Z] is not minute granularity"
      )
    )
    assertValidation(TimeGranularityOptionMinutesExample(None))

    val notHours = java.util.concurrent.TimeUnit.MILLISECONDS.toMillis(dateTime.getMillis)
    assertValidation(
      obj = TimeGranularityOptionHoursExample(Some(new DateTime(notHours))),
      withErrors = Seq(
        "timeValue: [2014-03-26T01:10:10.001Z] is not hour granularity"
      )
    )
    assertValidation(TimeGranularityOptionHoursExample(None))

    val notDays = java.util.concurrent.TimeUnit.MILLISECONDS.toMillis(dateTime.getMillis)
    assertValidation(
      obj = TimeGranularityOptionDaysExample(Some(new DateTime(notDays))),
      withErrors = Seq(
        "timeValue: [2014-03-26T01:10:10.001Z] is not day granularity"
      )
    )
    assertValidation(TimeGranularityOptionDaysExample(None))
  }

  test("UUID") {
    assertValidation(
      obj = UUIDOptionExample(Some("abcd1234")),
      withErrors = Seq(
        "uuid: [abcd1234] is not a valid UUID"
      )
    )
    assertValidation(obj = UUIDOptionExample(None))
  }
}
