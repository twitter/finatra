package com.twitter.finatra.validation.tests

import com.fasterxml.jackson.annotation.JsonCreator
import com.twitter.finatra.validation.CommonMethodValidations
import com.twitter.finatra.validation.constraints._
import com.twitter.util.Try
import com.twitter.util.validation.MethodValidation
import com.twitter.util.validation.engine.MethodValidationResult
import jakarta.validation.Valid
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import scala.beans.BeanProperty

object caseclasses {
  // AssertFalse
  case class AssertFalseExample(@AssertFalse boolValue: Boolean)
  case class AssertFalseOptionExample(@AssertFalse boolValue: Option[Boolean])
  // AssertTrue
  case class AssertTrueExample(@AssertTrue boolValue: Boolean)
  case class AssertTrueOptionExample(@AssertTrue boolValue: Option[Boolean])
  // CountryCode
  case class CountryCodeExample(@CountryCode countryCode: String)
  case class CountryCodeOptionExample(@CountryCode countryCode: Option[String])
  case class CountryCodeSeqExample(@CountryCode countryCode: Seq[String])
  case class CountryCodeOptionSeqExample(@CountryCode countryCode: Option[Seq[String]])
  case class CountryCodeArrayExample(@CountryCode countryCode: Array[String])
  case class CountryCodeOptionArrayExample(@CountryCode countryCode: Option[Array[String]])
  case class CountryCodeInvalidTypeExample(@CountryCode countryCode: Long)
  case class CountryCodeOptionInvalidTypeExample(@CountryCode countryCode: Option[Long])
  // FutureTime
  case class FutureExample(@FutureTime dateTime: DateTime)
  case class FutureOptionExample(@FutureTime dateTime: Option[DateTime])
  // Max
  case class MaxIntExample(@Max(0) numberValue: Int)
  case class MaxOptionIntExample(@Max(0) numberValue: Option[Int])
  case class MaxDoubleExample(@Max(0) numberValue: Double)
  case class MaxOptionDoubleExample(@Max(0) numberValue: Option[Double])
  case class MaxFloatExample(@Max(0) numberValue: Float)
  case class MaxOptionFloatExample(@Max(0) numberValue: Option[Float])
  case class MaxLongExample(@Max(0) numberValue: Long)
  case class MaxOptionLongExample(@Max(0) numberValue: Option[Long])
  case class MaxBigIntExample(@Max(0) numberValue: BigInt)
  case class MaxOptionBigIntExample(@Max(0) numberValue: Option[BigInt])
  case class MaxLargestLongBigIntExample(@Max(Long.MaxValue) numberValue: BigInt)
  case class MaxSecondLargestLongBigIntExample(@Max(Long.MaxValue - 1) numberValue: BigInt)
  case class MaxSmallestLongBigIntExample(@Max(Long.MinValue) numberValue: BigInt)
  case class MaxBigDecimalExample(@Max(0) numberValue: BigDecimal)
  case class MaxOptionBigDecimalExample(@Max(0) numberValue: Option[BigDecimal])
  case class MaxLargestLongBigDecimalExample(@Max(Long.MaxValue) numberValue: BigDecimal)
  case class MaxSecondLargestLongBigDecimalExample(@Max(Long.MaxValue - 1) numberValue: BigDecimal)
  case class MaxSmallestLongBigDecimalExample(@Max(Long.MinValue) numberValue: BigDecimal)
  case class MaxSeqExample(@Max(100) numberValue: Seq[Int])
  case class MaxOptionSeqExample(@Max(100) numberValue: Option[Seq[Int]])
  case class MaxMapExample(@Max(100) numberValue: Map[String, Int])
  case class MaxOptionMapExample(@Max(100) numberValue: Option[Map[String, Int]])
  case class MaxArrayExample(@Max(100) numberValue: Array[Int])
  case class MaxOptionArrayExample(@Max(100) numberValue: Option[Array[Int]])
  case class MaxInvalidTypeExample(@Max(100) numberValue: String)
  case class MaxOptionInvalidTypeExample(@Max(100) numberValue: Option[String])
  // Min
  case class MinIntExample(@Min(1) numberValue: Int)
  case class MinOptionIntExample(@Min(1) numberValue: Option[Int])
  case class MinLongExample(@Min(1) numberValue: Long)
  case class MinOptionLongExample(@Min(1) numberValue: Option[Long])
  case class MinDoubleExample(@Min(1) numberValue: Double)
  case class MinOptionDoubleExample(@Min(1) numberValue: Option[Double])
  case class MinFloatExample(@Min(1) numberValue: Float)
  case class MinOptionFloatExample(@Min(1) numberValue: Option[Float])
  case class MinBigIntExample(@Min(1) numberValue: BigInt)
  case class MinOptionBigIntExample(@Min(1) numberValue: Option[BigInt])
  case class MinSmallestLongBigIntExample(@Min(Long.MinValue) numberValue: BigInt)
  case class MinSecondSmallestLongBigIntExample(@Min(Long.MinValue + 1) numberValue: BigInt)
  case class MinLargestLongBigIntExample(@Min(Long.MaxValue) numberValue: BigInt)
  case class MinBigDecimalExample(@Min(1) numberValue: BigDecimal)
  case class MinOptionBigDecimalExample(@Min(1) numberValue: Option[BigDecimal])
  case class MinSmallestLongBigDecimalExample(@Min(Long.MinValue) numberValue: BigDecimal)
  case class MinSecondSmallestLongBigDecimalExample(@Min(Long.MinValue + 1) numberValue: BigDecimal)
  case class MinLargestLongBigDecimalExample(@Min(Long.MaxValue) numberValue: BigDecimal)
  case class MinSeqExample(@Min(10) numberValue: Seq[Int])
  case class MinOptionSeqExample(@Min(10) numberValue: Option[Seq[Int]])
  case class MinMapExample(@Min(10) numberValue: Map[String, Int])
  case class MinOptionMapExample(@Min(10) numberValue: Option[Map[String, Int]])
  case class MinArrayExample(@Min(10) numberValue: Array[Int])
  case class MinOptionArrayExample(@Min(10) numberValue: Option[Array[Int]])
  case class MinInvalidTypeExample(@Min(10) numberValue: String)
  case class MinOptionInvalidTypeExample(@Min(10) numberValue: Option[String])
  // NotEmpty
  case class NotEmptyExample(@NotEmpty stringValue: String)
  case class NotEmptyOptionExample(@NotEmpty stringValue: Option[String])
  case class NotEmptyArrayExample(@NotEmpty stringValue: Array[String])
  case class NotEmptyOptionArrayExample(@NotEmpty stringValue: Option[Array[String]])
  case class NotEmptyMapExample(@NotEmpty stringValue: Map[String, String])
  case class NotEmptyOptionMapExample(@NotEmpty stringValue: Option[Map[String, String]])
  case class NotEmptySeqExample(@NotEmpty stringValue: Seq[String])
  case class NotEmptyOptionSeqExample(@NotEmpty stringValue: Option[Seq[String]])
  case class NotEmptyInvalidTypeExample(@NotEmpty stringValue: Long)
  case class NotEmptyOptionInvalidTypeExample(@NotEmpty stringValue: Option[Long])
  // OneOf
  case class OneOfExample(@OneOf(value = Array("a", "B", "c")) enumValue: String)
  case class OneOfOptionExample(@OneOf(value = Array("a", "B", "c")) enumValue: Option[String])
  case class OneOfSeqExample(@OneOf(Array("a", "B", "c")) enumValue: Seq[String])
  case class OneOfOptionSeqExample(@OneOf(Array("a", "B", "c")) enumValue: Option[Seq[String]])
  case class OneOfInvalidTypeExample(@OneOf(Array("a", "B", "c")) enumValue: Long)
  case class OneOfOptionInvalidTypeExample(@OneOf(Array("a", "B", "c")) enumValue: Option[Long])
  // PastTime
  case class PastExample(@PastTime dateTime: DateTime)
  case class PastOptionExample(@PastTime dateTime: Option[DateTime])
  // Pattern
  case class NumberPatternExample(@Pattern(regexp = "[0-9]+") stringValue: String)
  case class NumberPatternOptionExample(@Pattern(regexp = "[0-9]+") stringValue: Option[String])
  case class NumberPatternArrayExample(@Pattern(regexp = "[0-9]+") stringValue: Array[String])
  case class NumberPatternOptionArrayExample(
    @Pattern(regexp = "[0-9]+") stringValue: Option[Array[String]])
  case class EmptyPatternExample(@Pattern(regexp = "") stringValue: String)
  case class EmptyPatternOptionExample(@Pattern(regexp = "") stringValue: Option[String])
  case class InvalidPatternExample(@Pattern(regexp = "([)") stringValue: String)
  case class InvalidPatternOptionExample(@Pattern(regexp = "([)") stringValue: Option[String])
  // Range
  case class RangeIntExample(@Range(min = 1, max = 50) pointValue: Int)
  case class RangeOptionIntExample(@Range(min = 1, max = 50) pointValue: Option[Int])
  case class RangeLongExample(@Range(min = 1, max = 50) pointValue: Long)
  case class RangeOptionLongExample(@Range(min = 1, max = 50) pointValue: Option[Long])
  case class RangeDoubleExample(@Range(min = 1, max = 50) pointValue: Double)
  case class RangeOptionDoubleExample(@Range(min = 1, max = 50) pointValue: Option[Double])
  case class RangeFloatExample(@Range(min = 1, max = 50) pointValue: Float)
  case class RangeOptionFloatExample(@Range(min = 1, max = 50) pointValue: Option[Float])
  case class RangeBigDecimalExample(@Range(min = 1, max = 50) pointValue: BigDecimal)
  case class RangeOptionBigDecimalExample(@Range(min = 1, max = 50) pointValue: Option[BigDecimal])
  case class RangeBigIntExample(@Range(min = 1, max = 50) pointValue: BigInt)
  case class RangeOptionBigIntExample(@Range(min = 1, max = 50) pointValue: Option[BigInt])
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
  case class RangeInvalidTypeExample(@Range(min = 1, max = 5) pointValue: String)
  case class RangeOptionInvalidTypeExample(@Range(min = 1, max = 5) pointValue: Option[String])
  case class RangeInvalidRangeExample(@Range(min = 5, max = 1) pointValue: Int)
  // Size
  case class SizeArrayExample(@Size(min = 10, max = 50) sizeValue: Array[Int])
  case class SizeOptionArrayExample(@Size(min = 10, max = 50) sizeValue: Option[Array[Int]])
  case class SizeSeqExample(@Size(min = 10, max = 50) sizeValue: Seq[Int])
  case class SizeOptionSeqExample(@Size(min = 10, max = 50) sizeValue: Option[Seq[Int]])
  case class SizeMapExample(@Size(min = 10, max = 50) sizeValue: Map[String, Int])
  case class SizeOptionMapExample(@Size(min = 10, max = 50) sizeValue: Option[Map[String, Int]])
  case class SizeStringExample(@Size(min = 10, max = 140) sizeValue: String)
  case class SizeOptionStringExample(@Size(min = 10, max = 140) sizeValue: Option[String])
  case class SizeInvalidTypeExample(@Size(min = 10, max = 50) sizeValue: Int)
  case class SizeOptionInvalidTypeExample(@Size(min = 10, max = 50) sizeValue: Option[Int])
  // TimeGranularity
  case class TimeGranularityNanosecondsExample(
    @TimeGranularity(TimeUnit.NANOSECONDS) timeValue: DateTime)
  case class TimeGranularityOptionNanosecondsExample(
    @TimeGranularity(TimeUnit.NANOSECONDS) timeValue: Option[DateTime])
  case class TimeGranularityMicrosecondsExample(
    @TimeGranularity(TimeUnit.MICROSECONDS) timeValue: DateTime)
  case class TimeGranularityOptionMicrosecondsExample(
    @TimeGranularity(TimeUnit.MICROSECONDS) timeValue: Option[DateTime])
  case class TimeGranularityMillisecondsExample(
    @TimeGranularity(TimeUnit.MILLISECONDS) timeValue: DateTime)
  case class TimeGranularityOptionMillisecondsExample(
    @TimeGranularity(TimeUnit.MILLISECONDS) timeValue: Option[DateTime])
  case class TimeGranularitySecondsExample(@TimeGranularity(TimeUnit.SECONDS) timeValue: DateTime)
  case class TimeGranularityOptionSecondsExample(
    @TimeGranularity(TimeUnit.SECONDS) timeValue: Option[DateTime])
  case class TimeGranularityMinutesExample(@TimeGranularity(TimeUnit.MINUTES) timeValue: DateTime)
  case class TimeGranularityOptionMinutesExample(
    @TimeGranularity(TimeUnit.MINUTES) timeValue: Option[DateTime])
  case class TimeGranularityHoursExample(@TimeGranularity(TimeUnit.HOURS) timeValue: DateTime)
  case class TimeGranularityOptionHoursExample(
    @TimeGranularity(TimeUnit.HOURS) timeValue: Option[DateTime])
  case class TimeGranularityDaysExample(@TimeGranularity(TimeUnit.DAYS) timeValue: DateTime)
  case class TimeGranularityOptionDaysExample(
    @TimeGranularity(TimeUnit.DAYS) timeValue: Option[DateTime])
  // UUID
  case class UUIDExample(@UUID uuid: String)
  case class UUIDOptionExample(@UUID uuid: Option[String])

  // multiple annotations
  case class User(
    @NotEmpty id: String,
    name: String,
    @OneOf(Array("F", "M", "Other")) gender: String) {
    @MethodValidation(fields = Array("name"))
    def nameCheck: MethodValidationResult = {
      MethodValidationResult.validIfTrue(name.length > 0, "cannot be empty")
    }
  }
  // Other fields not in constructor
  case class Person(@NotEmpty id: String, @NotEmpty name: String, @Valid address: Address) {
    val company: String = "Twitter"
    val city: String = "San Francisco"
    val state: String = "California"

    @MethodValidation(fields = Array("address"))
    def validateAddress: MethodValidationResult =
      MethodValidationResult.validIfTrue(address.state.nonEmpty, "state must be specified")
  }

  case class NoConstructorParams() {
    @BeanProperty @NotEmpty var id: String = ""
  }

  case class AnnotatedInternalFields(
    @NotEmpty id: String,
    @Size(min = 1, max = 100) bigString: String) {
    @NotEmpty val company: String = "" // this will be validated
  }

  case class AnnotatedBeanProperties(@Min(1) numbers: Seq[Int]) {
    @BeanProperty @NotEmpty var field1: String = "default"
  }

  case class Address(
    line1: String,
    line2: Option[String] = None,
    city: String,
    @StateConstraint state: String,
    zipcode: String,
    additionalPostalCode: Option[String] = None)

  // Customer defined validations
  case class StateValidationExample(@StateConstraint state: String)
  // Integration test
  case class ValidateUserRequest(
    @NotEmpty @Pattern(regexp = "[a-z]+") userName: String,
    @Max(value = 9999) id: Long,
    title: String)

  case class NestedUser(
    @NotEmpty id: String,
    @Valid person: Person,
    @OneOf(Array("F", "M", "Other")) gender: String,
    job: String) {
    @MethodValidation(fields = Array("job"))
    def jobCheck: MethodValidationResult = {
      MethodValidationResult.validIfTrue(job.length > 0, "cannot be empty")
    }
  }

  case class Division(
    name: String,
    @Valid team: Seq[Group])

  case class Persons(
    @NotEmpty id: String,
    @Min(1) @Valid people: Seq[Person])

  case class Group(
    @NotEmpty id: String,
    @Min(1) @Valid people: Seq[Person])

  case class CustomerAccount(
    @NotEmpty accountName: String,
    @Valid customer: Option[User])

  case class CollectionOfCollection(
    @NotEmpty id: String,
    @Valid @Min(1) people: Seq[Persons])

  case class CollectionWithArray(
    @NotEmpty @Size(min = 1, max = 30) names: Array[String],
    @NotEmpty @Max(2) users: Array[User])

  // cycle -- impossible to instantiate without using null to terminate somewhere
  case class A(@NotEmpty id: String, @Valid b: B)
  case class B(@NotEmpty id: String, @Valid c: C)
  case class C(@NotEmpty id: String, @Valid a: A)
  // another cycle -- fails as we detect the D type in F
  case class D(@NotEmpty id: String, @Valid e: E)
  case class E(@NotEmpty id: String, @Valid f: F)
  case class F(@NotEmpty id: String, @Valid d: Option[D])
  // last one -- fails as we detect the G type in I
  case class G(@NotEmpty id: String, @Valid h: H)
  case class H(@NotEmpty id: String, @Valid i: I)
  case class I(@NotEmpty id: String, @Valid g: Seq[G])

  // no validation -- no annotations
  object TestJsonCreator {
    @JsonCreator
    def apply(s: String): TestJsonCreator = TestJsonCreator(s.toInt)
  }
  case class TestJsonCreator(int: Int)

  // no validation -- no annotations
  object TestJsonCreator2 {
    @JsonCreator
    def apply(strings: Seq[String]): TestJsonCreator2 = TestJsonCreator2(strings.map(_.toInt))
  }
  case class TestJsonCreator2(ints: Seq[Int], default: String = "Hello, World")

  // no validation -- annotation is on secondary constructor
  object TestJsonCreatorWithValidation {
    @JsonCreator
    def apply(@NotEmpty s: String): TestJsonCreatorWithValidation =
      TestJsonCreatorWithValidation(s.toInt)
  }
  case class TestJsonCreatorWithValidation(int: Int)

  // this should validate per the primary constructor annotation
  object TestJsonCreatorWithValidations {
    @JsonCreator
    def apply(@NotEmpty s: String): TestJsonCreatorWithValidations =
      TestJsonCreatorWithValidations(s.toInt)
  }
  case class TestJsonCreatorWithValidations(@OneOf(Array("42", "137")) int: Int)

  // no validation -- no annotations
  case class CaseClassWithMultipleConstructors(number1: Long, number2: Long, number3: Long) {
    def this(numberAsString1: String, numberAsString2: String, numberAsString3: String) {
      this(numberAsString1.toLong, numberAsString2.toLong, numberAsString3.toLong)
    }
  }

  // no validation -- no annotations
  case class CaseClassWithMultipleConstructorsAnnotated(
    number1: Long,
    number2: Long,
    number3: Long) {
    @JsonCreator
    def this(numberAsString1: String, numberAsString2: String, numberAsString3: String) {
      this(numberAsString1.toLong, numberAsString2.toLong, numberAsString3.toLong)
    }
  }

  // no validation -- annotations are on secondary constructor
  case class CaseClassWithMultipleConstructorsAnnotatedAndValidations(
    number1: Long,
    number2: Long,
    uuid: String) {
    @JsonCreator
    def this(
      @NotEmpty numberAsString1: String,
      @OneOf(Array("10001", "20002", "30003")) numberAsString2: String,
      @UUID thirdArgument: String
    ) {
      this(numberAsString1.toLong, numberAsString2.toLong, thirdArgument)
    }
  }

  // this should validate -- annotations are on the primary constructor
  case class CaseClassWithMultipleConstructorsPrimaryAnnotatedAndValidations(
    @Min(10000) number1: Long,
    @OneOf(Array("10001", "20002", "30003")) number2: Long,
    @UUID uuid: String) {
    def this(
      numberAsString1: String,
      numberAsString2: String,
      thirdArgument: String
    ) {
      this(numberAsString1.toLong, numberAsString2.toLong, thirdArgument)
    }
  }

  trait AncestorWithValidation {
    @NotEmpty def field1: String

    @MethodValidation(fields = Array("field1"))
    def validateField1: MethodValidationResult =
      MethodValidationResult.validIfTrue(Try(field1.toDouble).isReturn, "not a double value")
  }

  case class ImplementsAncestor(override val field1: String) extends AncestorWithValidation

  case class InvalidDoublePerson(@NotEmpty name: String)(@NotEmpty otherName: String)
  case class DoublePerson(@NotEmpty name: String)(@NotEmpty val otherName: String)
  case class ValidDoublePerson(@NotEmpty name: String)(@NotEmpty otherName: String) {
    @MethodValidation
    def checkOtherName: MethodValidationResult =
      MethodValidationResult.validIfTrue(
        otherName.length >= 3,
        "otherName must be longer than 3 chars")
  }
  case class PossiblyValidDoublePerson(@NotEmpty name: String)(@NotEmpty otherName: String) {
    def referenceSecondArgInMethod: String =
      s"The otherName value is ${otherName}"
  }

  case class CaseClassWithIntAndDateTime(
    @NotEmpty name: String,
    age: Int,
    age2: Int,
    age3: Int,
    dateTime: DateTime,
    dateTime2: DateTime,
    dateTime3: DateTime,
    dateTime4: DateTime,
    @NotEmpty dateTime5: Option[DateTime])

  case class SimpleCar(
    id: Long,
    make: CarMake,
    model: String,
    @Min(2000) year: Int,
    @NotEmpty @Size(min = 2, max = 14) licensePlate: String, //multiple constraints
    @Min(0) numDoors: Int = 4,
    manual: Boolean = false)

  case class Car(
    id: Long,
    make: CarMake,
    model: String,
    @Min(2000) year: Int,
    @Valid owners: Seq[Person],
    @NotEmpty @Size(min = 2, max = 14) licensePlate: String, //multiple constraints
    @Min(0) numDoors: Int = 4,
    manual: Boolean = false,
    ownershipStart: DateTime = DateTime.now,
    ownershipEnd: DateTime = DateTime.now.plusYears(1),
    warrantyStart: Option[DateTime] = None,
    warrantyEnd: Option[DateTime] = None,
    @Valid passengers: Seq[Person] = Seq()) {

    @MethodValidation
    def validateId: MethodValidationResult = {
      MethodValidationResult.validIfTrue(id % 2 == 1, "id may not be even")
    }

    @MethodValidation
    def validateYearBeforeNow: MethodValidationResult = {
      val thisYear = new DateTime().getYear
      val yearMoreThanOneYearInFuture: Boolean =
        if (year > thisYear) { (year - thisYear) > 1 }
        else false
      MethodValidationResult.validIfFalse(
        yearMoreThanOneYearInFuture,
        "Model year can be at most one year newer."
      )
    }

    @MethodValidation(fields = Array("ownershipEnd"))
    def ownershipTimesValid: MethodValidationResult = {
      CommonMethodValidations.validateTimeRange(
        ownershipStart,
        ownershipEnd,
        "ownershipStart",
        "ownershipEnd"
      )
    }

    @MethodValidation(fields = Array("warrantyStart", "warrantyEnd"))
    def warrantyTimeValid: MethodValidationResult = {
      CommonMethodValidations.validateTimeRange(
        warrantyStart,
        warrantyEnd,
        "warrantyStart",
        "warrantyEnd"
      )
    }
  }

  case class Driver(
    @Valid person: Person,
    @Valid car: Car)

  case class MultipleConstraints(
    @NotEmpty @Max(100) ints: Seq[Int],
    @NotEmpty @Pattern(regexp = "\\d") digits: String,
    @NotEmpty @Size(min = 3, max = 3) @OneOf(Array("how", "now", "brown",
        "cow")) strings: Seq[String])

  // maps not supported
  case class MapsAndMaps(
    @NotEmpty id: String,
    @AssertTrue bool: Boolean,
    @Valid carAndDriver: Map[Person, SimpleCar])

  // @Valid only visible on field not on type arg
  case class TestAnnotations(
    @NotEmpty cars: Seq[SimpleCar @Valid])

  case class GenericTestCaseClass[T](@NotEmpty @Valid data: T)
  case class GenericTestCaseClassMultipleTypes[T, U, V](
    @NotEmpty @Valid data: T,
    @Max(100) things: Seq[U],
    @Size(min = 3, max = 3) @Valid otherThings: Seq[V])
  case class GenericTestCaseClassWithMultipleArgs[T](
    @NotEmpty data: T,
    @Min(5) number: Int)
}
