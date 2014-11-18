package com.twitter.finatra.tests.json.internal.caseclass.validation

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.{JsonFieldParseException, JsonObjectParseException}
import com.twitter.finatra.tests.json.internal.CarMake
import com.twitter.finatra.tests.json.internal.caseclass.validation.domain.{Address, Car, Person}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FeatureSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class CaseClassValidationTest extends FeatureSpec with ShouldMatchers {

  val mapper = FinatraObjectMapper.create()
  val prototypeCar = Car(
    id = 1,
    make = CarMake.Ford,
    model = "Model-T",
    year = 2000,
    owners = Seq(),
    numDoors = 2,
    manual = true)

  feature("class and field level validations") {

    scenario("top-level failed validations") {
      val car = prototypeCar.copy(id = 2, year = 1910)

      val parseError = intercept[JsonObjectParseException] {
        mapper.parse[Car](
          mapper.writeValueAsBytes(car))
      }

      parseError should equal(JsonObjectParseException(
        Seq(JsonFieldParseException("year [1910] is not greater than or equal to 2000"))))
    }

    scenario("nested failed validations") {
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
