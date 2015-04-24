package com.twitter.finatra.tests.json.internal.caseclass.validation

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.{JsonMethodValidationException, JsonFieldParseException, JsonObjectParseException}
import com.twitter.finatra.tests.json.internal.CarMake
import com.twitter.finatra.tests.json.internal.caseclass.validation.domain.{Address, Car, Person}
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
      val parseError = intercept[JsonObjectParseException] {
        parseCar(baseCar.copy(id = 2, year = 1910))
      }

      parseError should equal(JsonObjectParseException(
        Seq(JsonFieldParseException("year [1910] is not greater than or equal to 2000"))))
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

      val parseError = intercept[JsonObjectParseException] {
        parseCar(car)
      }

      parseError should equal(JsonObjectParseException(
        Seq(
          JsonFieldParseException("owners.address.street cannot be empty"),
          JsonFieldParseException("owners.address.city cannot be empty"))))
    }

    "end before start" in {
      intercept[JsonObjectParseException] {
        parseCar(baseCar.copy(
          ownershipStart = baseCar.ownershipEnd,
          ownershipEnd = baseCar.ownershipStart))
      } should equal(
        JsonObjectParseException(methodValidationErrors = Seq(
          JsonMethodValidationException("ownershipEnd [2015-04-09T05:17:15.000Z] must be after ownershipStart [2015-04-09T05:18:15.000Z]"))))
    }

    "optional end before start" in {
      intercept[JsonObjectParseException] {
        parseCar(baseCar.copy(
          warrantyStart = baseCar.warrantyEnd,
          warrantyEnd = baseCar.warrantyStart))
      } should equal(
        JsonObjectParseException(methodValidationErrors = Seq(
          JsonMethodValidationException("warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]"))))
    }
  }

  private def parseCar(car: Car): Car = {
    mapper.parse[Car](
      mapper.writeValueAsBytes(car))
  }
}
