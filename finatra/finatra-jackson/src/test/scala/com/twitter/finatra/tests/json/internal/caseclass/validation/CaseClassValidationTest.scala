package com.twitter.finatra.tests.json.internal.caseclass.validation

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.{JsonFieldParseException, JsonObjectParseException}
import com.twitter.finatra.tests.JsonTest
import com.twitter.finatra.tests.json.internal.CarMake
import com.twitter.finatra.tests.json.internal.caseclass.validation.domain.{Address, Car, Person}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CaseClassValidationTest extends JsonTest {

  val now = DateTime.now

  val mapper = FinatraObjectMapper.create()
  val prototypeCar = Car(
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
    "top-level failed validations" in {
      val car = prototypeCar.copy(id = 2, year = 1910)

      val parseError = intercept[JsonObjectParseException] {
        mapper.parse[Car](
          mapper.writeValueAsBytes(car))
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
      val car = prototypeCar.copy(owners = owners)

      val parseError = intercept[JsonObjectParseException] {
        mapper.parse[Car](
          mapper.writeValueAsBytes(car))
      }

      parseError should equal(JsonObjectParseException(
        Seq(
          JsonFieldParseException("owners.address.street cannot be empty"),
          JsonFieldParseException("owners.address.city cannot be empty"))))
    }
  }
}
