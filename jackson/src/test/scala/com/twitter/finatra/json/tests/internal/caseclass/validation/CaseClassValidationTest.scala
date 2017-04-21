package com.twitter.finatra.json.tests.internal.caseclass.validation

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassValidationException.PropertyPath
import com.twitter.finatra.json.internal.caseclass.exceptions.{CaseClassMappingException, CaseClassValidationException}
import com.twitter.finatra.json.tests.internal.CarMake
import com.twitter.finatra.json.tests.internal.caseclass.validation.domain.{Address, Car, Person}
import com.twitter.finatra.validation.ErrorCode
import com.twitter.finatra.validation.ValidationResult.Invalid
import com.twitter.inject.Test
import org.joda.time.DateTime

class CaseClassValidationTest extends Test {

  val now = new DateTime("2015-04-09T05:17:15Z")

  val mapper = FinatraObjectMapper.create()
  val baseCar = Car(
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
    warrantyEnd = Some(now.plusHours(1)))

  test("class and field level validations#success") {
    parseCar(baseCar)
  }

  test("class and field level validations#top-level failed validations") {
    val parseError = intercept[CaseClassMappingException] {
      parseCar(baseCar.copy(id = 2, year = 1910))
    }

    parseError should equal(CaseClassMappingException(
      Set(CaseClassValidationException(PropertyPath.leaf("year"), Invalid("[1910] is not greater than or equal to 2000", ErrorCode.ValueTooSmall(2000, 1910))))))
  }

  test("class and field level validations#nested failed validations") {
    val owners = Seq(
      Person(
        name = "joe smith",
        dob = Some(DateTime.now),
        address = Some(Address(
          street = Some(""), // invalid
          city = "", // invalid
          state = "FL")))
    )
    val car = baseCar.copy(owners = owners)

    val parseError = intercept[CaseClassMappingException] {
      parseCar(car)
    }

    parseError should equal(CaseClassMappingException(
      Set(
        CaseClassValidationException(PropertyPath.leaf("city").withParent("address").withParent("owners"), Invalid("cannot be empty", ErrorCode.ValueCannotBeEmpty)),
        CaseClassValidationException(PropertyPath.leaf("street").withParent("address").withParent("owners"), Invalid("cannot be empty", ErrorCode.ValueCannotBeEmpty))
      )))
  }

  test("class and field level validations#nested method validations") {
    val owners = Seq(
      Person(
        name = "joe smith",
        dob = Some(DateTime.now),
        address = Some(Address(
          city = "pyongyang",
          state = "KP" /* invalid */)))
    )
    val car = baseCar.copy(owners = owners)

    val parseError = intercept[CaseClassMappingException] {
      parseCar(car)
    }

    parseError should equal(CaseClassMappingException(
      Set(
        CaseClassValidationException(PropertyPath.leaf("address").withParent("owners"), Invalid("state must be one of [CA, MD, WI]")))))

    parseError.errors.map(_.getMessage) should equal(Seq("owners.address: state must be one of [CA, MD, WI]"))
  }

  test("class and field level validations#end before start") {
    intercept[CaseClassMappingException] {
      parseCar(baseCar.copy(
        ownershipStart = baseCar.ownershipEnd,
        ownershipEnd = baseCar.ownershipStart))
    } should equal(
      CaseClassMappingException(
        Set(
          CaseClassValidationException(
            PropertyPath.empty,
            Invalid("ownershipEnd [2015-04-09T05:17:15.000Z] must be after ownershipStart [2015-04-09T05:18:15.000Z]")))))
  }

  test("class and field level validations#optional end before start") {
    intercept[CaseClassMappingException] {
      parseCar(baseCar.copy(
        warrantyStart = baseCar.warrantyEnd,
        warrantyEnd = baseCar.warrantyStart))
    } should equal(
      CaseClassMappingException(
        Set(
          CaseClassValidationException(
            PropertyPath.empty,
            Invalid("warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]")))))
  }

  test("class and field level validations#no start with end") {
    intercept[CaseClassMappingException] {
      parseCar(baseCar.copy(
        warrantyStart = None,
        warrantyEnd = baseCar.warrantyEnd))
    } should equal(
      CaseClassMappingException(
        Set(
          CaseClassValidationException(
            PropertyPath.empty,
            Invalid("both warrantyStart and warrantyEnd are required for a valid range")))))
  }

  test("class and field level validations#start with end") {
    parseCar(baseCar)
  }

  test("class and field level validations#errors sorted by message") {
    val first = CaseClassValidationException(PropertyPath.empty, Invalid("123"))
    val second = CaseClassValidationException(PropertyPath.empty, Invalid("aaa"))
    val third = CaseClassValidationException(PropertyPath.leaf("bla"), Invalid("zzz"))
    val fourth = CaseClassValidationException(PropertyPath.empty, Invalid("xxx"))

    val unsorted = Set(third, second, fourth, first)
    val expectedSorted = Seq(first, second, third, fourth)

    CaseClassMappingException(unsorted).errors should equal(expectedSorted)
  }

  test("class and field level validations#option[string] validation") {
    val address = Address(
      street = Some(""), // invalid
      city = "New Orleans",
      state = "LA")

    val parseError = intercept[CaseClassMappingException] {
      mapper.parse[Address](mapper.writeValueAsBytes(address))
    }
    println(parseError)
  }

  private def parseCar(car: Car): Car = {
    mapper.parse[Car](
      mapper.writeValueAsBytes(car))
  }
}
