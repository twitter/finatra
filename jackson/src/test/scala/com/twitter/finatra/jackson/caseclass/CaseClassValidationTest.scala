package com.twitter.finatra.jackson.caseclass

import com.twitter.finatra.jackson.caseclass.exceptions.{
  CaseClassFieldMappingException,
  CaseClassMappingException
}
import com.twitter.finatra.jackson.{Address, Car, Person}
import com.twitter.finatra.jackson.{CarMake, ScalaObjectMapper}
import com.twitter.finatra.validation.ValidationResult.Invalid
import com.twitter.finatra.validation.constraints.{Min, NotEmpty}
import com.twitter.finatra.validation.{ErrorCode, MethodValidation}
import com.twitter.inject.Test
import org.joda.time.DateTime
import org.scalatest.OptionValues._

class CaseClassValidationTest extends Test {
  private final val now: DateTime = new DateTime("2015-04-09T05:17:15Z")
  private final val baseCar: Car = Car(
    id = 1,
    make = CarMake.Ford,
    model = "Model-T",
    year = 2000,
    owners = Seq(),
    numDoors = 2,
    manual = true,
    ownershipStart = now,
    ownershipEnd = now.plusMinutes(1),
    warrantyStart = Some(now),
    warrantyEnd = Some(now.plusHours(1)),
    passengers = Seq.empty[Person]
  )
  private final val mapper: ScalaObjectMapper = ScalaObjectMapper()

  test("class and field level validations#success") {
    parseCar(baseCar)
  }

  test("class and field level validations#top-level failed validations") {
    val parseError = intercept[CaseClassMappingException] {
      parseCar(baseCar.copy(id = 2, year = 1910))
    }

    parseError.errors.size shouldEqual 1

    val error = parseError.errors.head
    error.path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("year")
    error.reason.code shouldEqual ErrorCode.ValueTooSmall(2000, 1910)
    error.reason.message shouldEqual "[1910] is not greater than or equal to 2000"
    error.reason.annotation shouldBe defined
    error.reason.annotation.value shouldBe a[Min]
  }

  test("class and field level validations#nested failed validations") {
    val owners = Seq(
      Person(
        id = 1,
        name = "joe smith",
        dob = Some(DateTime.now),
        age = None,
        address = Some(
          Address(
            street = Some(""), // invalid
            city = "", // invalid
            state = "FL"
          )
        )
      )
    )
    val car = baseCar.copy(owners = owners)

    val parseError = intercept[CaseClassMappingException] {
      parseCar(car)
    }

    parseError.errors.size shouldEqual 2
    val errors = parseError.errors

    errors.head.path shouldEqual
      CaseClassFieldMappingException.PropertyPath
        .leaf("city").withParent("address").withParent("owners")
    errors.head.reason.code shouldEqual ErrorCode.ValueCannotBeEmpty
    errors.head.reason.message shouldEqual "cannot be empty"
    errors.head.reason.annotation shouldBe defined
    errors.head.reason.annotation.value shouldBe a[NotEmpty]

    errors(1).path shouldEqual
      CaseClassFieldMappingException.PropertyPath
        .leaf("street").withParent("address").withParent("owners")
    errors(1).reason.code shouldEqual ErrorCode.ValueCannotBeEmpty
    errors(1).reason.message shouldEqual "cannot be empty"
    errors(1).reason.annotation shouldBe defined
    errors(1).reason.annotation.value shouldBe a[NotEmpty]
  }

  test("class and field level validations#nested method validations") {
    val owners = Seq(
      Person(
        id = 2,
        name = "joe smith",
        dob = Some(DateTime.now),
        age = None,
        address = Some(Address(city = "pyongyang", state = "KP" /* invalid */ ))
      )
    )
    val car = baseCar.copy(owners = owners)

    val parseError = intercept[CaseClassMappingException] {
      parseCar(car)
    }

    parseError.errors.size shouldEqual 1
    val errors = parseError.errors

    errors.head.path shouldEqual
      CaseClassFieldMappingException.PropertyPath.leaf("address").withParent("owners")
    errors.head.reason.code shouldEqual ErrorCode.Unknown
    errors.head.reason.message shouldEqual "state must be one of [CA, MD, WI]"
    errors.head.reason.annotation shouldBe defined
    errors.head.reason.annotation.value shouldBe a[MethodValidation]

    errors.map(_.getMessage) should equal(
      Seq("owners.address: state must be one of [CA, MD, WI]")
    )
  }

  test("class and field level validations#end before start") {
    val parseError = intercept[CaseClassMappingException] {
      parseCar(
        baseCar.copy(ownershipStart = baseCar.ownershipEnd, ownershipEnd = baseCar.ownershipStart)
      )
    }

    parseError.errors.size shouldEqual 1
    val errors = parseError.errors

    errors.head.path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("ownershipEnd")
    errors.head.reason.code shouldEqual ErrorCode.Unknown
    errors.head.reason.message shouldEqual "ownershipEnd [2015-04-09T05:17:15.000Z] must be after ownershipStart [2015-04-09T05:18:15.000Z]"
    errors.head.reason.annotation shouldBe defined
    errors.head.reason.annotation.value shouldBe a[MethodValidation]
  }

  test("class and field level validations#optional end before start") {
    val parseError = intercept[CaseClassMappingException] {
      parseCar(
        baseCar.copy(warrantyStart = baseCar.warrantyEnd, warrantyEnd = baseCar.warrantyStart)
      )
    }

    parseError.errors.size shouldEqual 2
    val errors = parseError.errors

    errors.head.path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("warrantyEnd")
    errors.head.reason.code shouldEqual ErrorCode.Unknown
    errors.head.reason.message shouldEqual
      "warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]"
    errors.head.reason.annotation shouldBe defined
    errors.head.reason.annotation.value shouldBe a[MethodValidation]

    errors(1).path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("warrantyStart")
    errors(1).reason.code shouldEqual ErrorCode.Unknown
    errors(1).reason.message shouldEqual
      "warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]"
    errors(1).reason.annotation shouldBe defined
    errors(1).reason.annotation.value shouldBe a[MethodValidation]
  }

  test("class and field level validations#no start with end") {
    val parseError = intercept[CaseClassMappingException] {
      parseCar(baseCar.copy(warrantyStart = None, warrantyEnd = baseCar.warrantyEnd))
    }

    parseError.errors.size shouldEqual 2
    val errors = parseError.errors

    errors.head.path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("warrantyEnd")
    errors.head.reason.code shouldEqual ErrorCode.Unknown
    errors.head.reason.message shouldEqual
      "both warrantyStart and warrantyEnd are required for a valid range"
    errors.head.reason.annotation shouldBe defined
    errors.head.reason.annotation.value shouldBe a[MethodValidation]

    errors(1).path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("warrantyStart")
    errors(1).reason.code shouldEqual ErrorCode.Unknown
    errors(1).reason.message shouldEqual
      "both warrantyStart and warrantyEnd are required for a valid range"
    errors(1).reason.annotation shouldBe defined
    errors(1).reason.annotation.value shouldBe a[MethodValidation]
  }

  test("class and field level validations#start with end") {
    parseCar(baseCar)
  }

  test("class and field level validations#errors sorted by message") {
    val first =
      CaseClassFieldMappingException(
        CaseClassFieldMappingException.PropertyPath.Empty,
        Invalid("123"))
    val second =
      CaseClassFieldMappingException(
        CaseClassFieldMappingException.PropertyPath.Empty,
        Invalid("aaa"))
    val third = CaseClassFieldMappingException(
      CaseClassFieldMappingException.PropertyPath.leaf("bla"),
      Invalid("zzz"))
    val fourth =
      CaseClassFieldMappingException(
        CaseClassFieldMappingException.PropertyPath.Empty,
        Invalid("xxx"))

    val unsorted = Set(third, second, fourth, first)
    val expectedSorted = Seq(first, second, third, fourth)

    CaseClassMappingException(unsorted).errors should equal(expectedSorted)
  }

  test("class and field level validations#option[string] validation") {
    val address = Address(
      street = Some(""), // invalid
      city = "New Orleans",
      state = "LA"
    )

    intercept[CaseClassMappingException] {
      mapper.parse[Address](mapper.writeValueAsBytes(address))
    }
  }

  private def parseCar(car: Car): Car = {
    mapper.parse[Car](mapper.writeValueAsBytes(car))
  }
}
