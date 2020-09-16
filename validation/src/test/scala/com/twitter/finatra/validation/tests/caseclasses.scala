package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.{MethodValidation, ValidationResult}
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import com.twitter.finatra.validation.constraints._

object caseclasses {
  // CountryCode
  case class CountryCodeExample(@CountryCode countryCode: String)
  case class CountryCodeSeqExample(@CountryCode countryCode: Seq[String])
  case class CountryCodeArrayExample(@CountryCode countryCode: Array[String])
  case class CountryCodeInvalidTypeExample(@CountryCode countryCode: Long)
  // FutureTime
  case class FutureExample(@FutureTime dateTime: DateTime)
  // Max
  case class MaxIntExample(@Max(0) numberValue: Int)
  case class MaxDoubleExample(@Max(0) numberValue: Double)
  case class MaxFloatExample(@Max(0) numberValue: Float)
  case class MaxLongExample(@Max(0) numberValue: Long)
  case class MaxBigIntExample(@Max(0) numberValue: BigInt)
  case class MaxLargestLongBigIntExample(@Max(Long.MaxValue) numberValue: BigInt)
  case class MaxSecondLargestLongBigIntExample(@Max(Long.MaxValue - 1) numberValue: BigInt)
  case class MaxSmallestLongBigIntExample(@Max(Long.MinValue) numberValue: BigInt)
  case class MaxBigDecimalExample(@Max(0) numberValue: BigDecimal)
  case class MaxLargestLongBigDecimalExample(@Max(Long.MaxValue) numberValue: BigDecimal)
  case class MaxSecondLargestLongBigDecimalExample(@Max(Long.MaxValue - 1) numberValue: BigDecimal)
  case class MaxSmallestLongBigDecimalExample(@Max(Long.MinValue) numberValue: BigDecimal)
  case class MaxSeqExample(@Max(100) numberValue: Seq[Int])
  case class MaxArrayExample(@Max(100) numberValue: Array[Int])
  case class MaxInvalidTypeExample(@Max(100) numberValue: String)
  // Min
  case class MinIntExample(@Min(1) numberValue: Int)
  case class MinLongExample(@Min(1) numberValue: Long)
  case class MinDoubleExample(@Min(1) numberValue: Double)
  case class MinFloatExample(@Min(1) numberValue: Float)
  case class MinBigIntExample(@Min(1) numberValue: BigInt)
  case class MinSmallestLongBigIntExample(@Min(Long.MinValue) numberValue: BigInt)
  case class MinSecondSmallestLongBigIntExample(@Min(Long.MinValue + 1) numberValue: BigInt)
  case class MinLargestLongBigIntExample(@Min(Long.MaxValue) numberValue: BigInt)
  case class MinBigDecimalExample(@Min(1) numberValue: BigDecimal)
  case class MinSmallestLongBigDecimalExample(@Min(Long.MinValue) numberValue: BigDecimal)
  case class MinSecondSmallestLongBigDecimalExample(@Min(Long.MinValue + 1) numberValue: BigDecimal)
  case class MinLargestLongBigDecimalExample(@Min(Long.MaxValue) numberValue: BigDecimal)
  case class MinSeqExample(@Min(10) numberValue: Seq[Int])
  case class MinArrayExample(@Min(10) numberValue: Array[Int])
  case class MinInvalidTypeExample(@Min(10) numberValue: String)
  // NotEmpty
  case class NotEmptyExample(@NotEmpty stringValue: String)
  case class NotEmptyArrayExample(@NotEmpty stringValue: Array[String])
  case class NotEmptySeqExample(@NotEmpty stringValue: Seq[String])
  case class NotEmptyInvalidTypeExample(@NotEmpty stringValue: Long)
  // OneOf
  case class OneOfExample(@OneOf(value = Array("a", "B", "c")) enumValue: String)
  case class OneOfSeqExample(@OneOf(Array("a", "B", "c")) enumValue: Seq[String])
  case class OneOfInvalidTypeExample(@OneOf(Array("a", "B", "c")) enumValue: Long)
  // PastTime
  case class PastExample(@PastTime dateTime: DateTime)
  // Pattern
  case class NumberPatternExample(@Pattern(regexp = "[0-9]+") stringValue: String)
  case class NumberPatternArrayExample(@Pattern(regexp = "[0-9]+") stringValue: Array[String])
  case class EmptyPatternExample(@Pattern(regexp = "") stringValue: String)
  case class InvalidPatternExample(@Pattern(regexp = "([)") stringValue: String)
  // Range
  case class RangeIntExample(@Range(min = 1, max = 50) pointValue: Int)
  case class RangeLongExample(@Range(min = 1, max = 50) pointValue: Long)
  case class RangeDoubleExample(@Range(min = 1, max = 50) pointValue: Double)
  case class RangeFloatExample(@Range(min = 1, max = 50) pointValue: Float)
  case class RangeBigDecimalExample(@Range(min = 1, max = 50) pointValue: BigDecimal)
  case class RangeBigIntExample(@Range(min = 1, max = 50) pointValue: BigInt)
  case class RangeLargestLongBigDecimalExample(
    @Range(min = 1, max = Long.MaxValue) pointValue: BigDecimal)
  case class RangeLargestLongBigIntExample(@Range(min = 1, max = Long.MaxValue) pointValue: BigInt)
  case class RangeSecondLargestLongBigDecimalExample(
    @Range(min = 1, max = Long.MaxValue - 1) pointValue: BigDecimal)
  case class RangeSecondLargestLongBigIntExample(
    @Range(min = 1, max = Long.MaxValue - 1) pointValue: BigInt)
  case class RangeSmallestLongBigDecimalExample(
    @Range(min = Long.MinValue, max = 5) pointValue: BigDecimal)
  case class RangeSecondSmallestLongBigDecimalExample(
    @Range(min = Long.MinValue + 1, max = 5) pointValue: BigDecimal)
  case class RangeSmallestLongBigIntExample(@Range(min = Long.MinValue, max = 5) pointValue: BigInt)
  case class RangeSecondSmallestLongBigIntExample(
    @Range(min = Long.MinValue + 1, max = 5) pointValue: BigInt)
  // Size
  case class SizeArrayExample(@Size(min = 10, max = 50) sizeValue: Array[Int])
  case class SizeSeqExample(@Size(min = 10, max = 50) sizeValue: Array[Int])
  case class SizeInvalidTypeExample(@Size(min = 10, max = 50) sizeValue: Int)
  case class SizeStringExample(@Size(min = 10, max = 140) sizeValue: String)
  // TimeGranularity
  case class TimeGranularityNanosecondsExample(
    @TimeGranularity(TimeUnit.NANOSECONDS) timeValue: DateTime)
  case class TimeGranularityMicrosecondsExample(
    @TimeGranularity(TimeUnit.MICROSECONDS) timeValue: DateTime)
  case class TimeGranularityMillisecondsExample(
    @TimeGranularity(TimeUnit.MILLISECONDS) timeValue: DateTime)
  case class TimeGranularitySecondsExample(@TimeGranularity(TimeUnit.SECONDS) timeValue: DateTime)
  case class TimeGranularityMinutesExample(@TimeGranularity(TimeUnit.MINUTES) timeValue: DateTime)
  case class TimeGranularityHoursExample(@TimeGranularity(TimeUnit.HOURS) timeValue: DateTime)
  case class TimeGranularityDaysExample(@TimeGranularity(TimeUnit.DAYS) timeValue: DateTime)
  // UUID
  case class UUIDExample(@UUID uuid: String)
  // multiple annotations
  case class User(@NotEmpty id: String, name: String, @OneOf(Array("F", "M")) gender: String) {
    @MethodValidation
    def nameCheck: ValidationResult = {
      ValidationResult.validate(name.length > 0, "name cannot be empty")
    }
  }
  // Other fields not in constructor
  case class Person(@NotEmpty id: String, name: String) {
    val company: String = "Twitter"
    val city: String = "San Francisco"
    val state: String = "California"
  }

  // Customer defined validations
  case class StateValidationExample(@StateConstraint state: String)
  // Integration test
  case class ValidateUserRequest(
    @NotEmpty @Pattern(regexp = "[a-z]+") userName: String,
    @Max(value = 9999) id: Long,
    title: String)
}
