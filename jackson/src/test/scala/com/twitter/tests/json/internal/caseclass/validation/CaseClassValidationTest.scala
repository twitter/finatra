package com.twitter.finatra.tests.json.internal.caseclass.validation

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.{CaseClassValidationException, CaseClassMappingException}
import com.twitter.finatra.json.internal.caseclass.validation.validators.MinValidator
import com.twitter.finatra.tests.json.internal.CarMake
import com.twitter.finatra.tests.json.internal.caseclass.validation.domain.{Address, Car, Person}
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

  "class and field level validations" should {
    "success" in {
      parseCar(baseCar)
    }

    "top-level failed validations" in {
      val parseError = intercept[CaseClassMappingException] {
        parseCar(baseCar.copy(id = 2, year = 1910))
      }

      parseError should equal(CaseClassMappingException(
        Seq(CaseClassValidationException("year", Invalid("[1910] is not greater than or equal to 2000", MinValidator.MinValueNotObtained(2000, 1910))))))
    }

    "nested failed validations" in {
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
        Seq(
          CaseClassValidationException("owners.address.street", Invalid("cannot be empty")),
          CaseClassValidationException("owners.address.city", Invalid("cannot be empty")))))
    }

    "end before start" in {
      intercept[CaseClassMappingException] {
        parseCar(baseCar.copy(
          ownershipStart = baseCar.ownershipEnd,
          ownershipEnd = baseCar.ownershipStart))
      } should equal(
        CaseClassMappingException(
          Seq(
            CaseClassValidationException(
              "",
              Invalid("ownershipEnd [2015-04-09T05:17:15.000Z] must be after ownershipStart [2015-04-09T05:18:15.000Z]")))))
    }

    "optional end before start" in {
      intercept[CaseClassMappingException] {
        parseCar(baseCar.copy(
          warrantyStart = baseCar.warrantyEnd,
          warrantyEnd = baseCar.warrantyStart))
      } should equal(
        CaseClassMappingException(
          Seq(
            CaseClassValidationException(
              "",
              Invalid("warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]")))))
    }
  }

  private def parseCar(car: Car): Car = {
    mapper.parse[Car](
      mapper.writeValueAsBytes(car))
  }
}
