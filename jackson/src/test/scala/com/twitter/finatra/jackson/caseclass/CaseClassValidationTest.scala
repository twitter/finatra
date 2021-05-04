package com.twitter.finatra.jackson.caseclass

import com.twitter.finatra.jackson.caseclass.exceptions.CaseClassFieldMappingException.ValidationError
import com.twitter.finatra.jackson.caseclass.exceptions.{
  CaseClassFieldMappingException,
  CaseClassMappingException
}
import com.twitter.finatra.jackson.{Address, Car, CarMake, Person, ScalaObjectMapper}
import com.twitter.finatra.validation.ErrorCode
import com.twitter.inject.Test
import org.joda.time.DateTime

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

  /*
   Notes:

   ValidationError.Field types will not have a root bean instance and the violation property path
   will include the class name, e.g., for a violation on the `id` parameter of the `RentalStation` case
   class, the property path would be: `RentalStation.id`.

   ValidationError.Method types will have a root bean instance (since they require an instance in order for
   the method to be invoked) and the violation property path will not include the class name since this
   is understood to be known since the method validations typically have to be manually invoked, e.g.,
   for the method `validateState` that is annotated with field `state`, the property path of a violation would be:
   `validateState.state`.
   */

  test("class and field level validations#success") {
    parseCar(baseCar)
  }

  test("class and field level validations#top-level failed validations") {
    val value = baseCar.copy(id = 2, year = 1910)
    val parseError = intercept[CaseClassMappingException] {
      parseCar(value)
    }

    parseError.errors.size shouldEqual 1

    val error = parseError.errors.head
    error.path should equal(CaseClassFieldMappingException.PropertyPath.leaf("year"))
    error.reason.message should equal("[1910] is not greater than or equal to 2000")
    error.reason.detail match {
      case ValidationError(violation, ValidationError.Field, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.ValueTooSmall(2000, 1910)
        violation.getPropertyPath.toString should equal("Car.year")
        violation.getMessage should equal("[1910] is not greater than or equal to 2000")
        violation.getInvalidValue should equal(1910)
        violation.getRootBeanClass should equal(classOf[Car])
        violation.getRootBean == null should be(
          true
        ) // ValidationError.Field types won't have a root bean instance
      case _ => fail()
    }
  }

  test("class and field level validations#nested failed validations") {
    val invalidAddress =
      Address(
        street = Some(""), // invalid
        city = "", // invalid
        state = "FL" // invalid
      )

    val owners = Seq(
      Person(
        id = 1,
        name = "joe smith",
        dob = Some(DateTime.now),
        age = None,
        address = Some(invalidAddress)
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
    errors.head.reason.message shouldEqual "cannot be empty"
    errors.head.reason.detail match {
      case ValidationError(violation, ValidationError.Field, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.ValueCannotBeEmpty
        violation.getPropertyPath.toString shouldEqual "Address.city"
        violation.getMessage shouldEqual "cannot be empty"
        violation.getInvalidValue shouldEqual ""
        violation.getRootBeanClass shouldEqual classOf[Address]
        violation.getRootBean == null shouldBe true // ValidationError.Field types won't have a root bean instance
      case _ =>
        fail()
    }

    errors(1).path shouldEqual
      CaseClassFieldMappingException.PropertyPath
        .leaf("street").withParent("address").withParent("owners")
    errors(1).reason.message shouldEqual "cannot be empty"
    errors(1).reason.detail match {
      case ValidationError(violation, ValidationError.Field, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.ValueCannotBeEmpty
        violation.getPropertyPath.toString shouldEqual "Address.street"
        violation.getMessage shouldEqual "cannot be empty"
        violation.getInvalidValue shouldEqual ""
        violation.getRootBeanClass shouldEqual classOf[Address]
        violation.getRootBean == null shouldBe true // ValidationError.Field types won't have a root bean instance
      case _ =>
        fail()
    }
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
    val car: Car = baseCar.copy(owners = owners)

    val parseError = intercept[CaseClassMappingException] {
      parseCar(car)
    }

    parseError.errors.size shouldEqual 1
    val errors = parseError.errors

    errors.head.path shouldEqual
      CaseClassFieldMappingException.PropertyPath
        .leaf("address").withParent("owners")
    errors.head.reason.message shouldEqual "state must be one of [CA, MD, WI]"
    errors.head.reason.detail.getClass shouldEqual classOf[ValidationError]
    errors.head.reason.detail match {
      case ValidationError(violation, ValidationError.Method, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.IllegalArgument
        violation.getPropertyPath.toString shouldEqual "validateState" // the @MethodValidation annotation does not specify a field.
        violation.getMessage shouldEqual "state must be one of [CA, MD, WI]"
        violation.getInvalidValue shouldEqual Address(city = "pyongyang", state = "KP")
        violation.getRootBeanClass shouldEqual classOf[Address]
        violation.getRootBean shouldEqual Address(
          city = "pyongyang",
          state = "KP"
        ) // ValidationError.Method types have a root bean instance
      case _ =>
        fail()
    }

    errors.map(_.getMessage) shouldEqual (
      Seq("owners.address: state must be one of [CA, MD, WI]")
    )
  }

  test("class and field level validations#end before start") {
    val value: Car =
      baseCar.copy(ownershipStart = baseCar.ownershipEnd, ownershipEnd = baseCar.ownershipStart)
    val parseError = intercept[CaseClassMappingException] {
      parseCar(value)
    }

    parseError.errors.size shouldEqual 1
    val errors = parseError.errors

    errors.head.path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("ownership_end")
    errors.head.reason.message shouldEqual "ownershipEnd [2015-04-09T05:17:15.000Z] must be after ownershipStart [2015-04-09T05:18:15.000Z]"
    errors.head.reason.detail.getClass shouldEqual classOf[ValidationError]
    errors.head.reason.detail match {
      case ValidationError(violation, ValidationError.Method, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.Unknown
        violation.getPropertyPath.toString shouldEqual "ownershipTimesValid.ownershipEnd" // the @MethodValidation annotation specifies a single field.
        violation.getMessage shouldEqual "ownershipEnd [2015-04-09T05:17:15.000Z] must be after ownershipStart [2015-04-09T05:18:15.000Z]"
        violation.getInvalidValue shouldEqual value
        violation.getRootBeanClass shouldEqual classOf[Car]
        violation.getRootBean shouldEqual value // ValidationError.Method types have a root bean instance
      case _ =>
        fail()
    }
  }

  test("class and field level validations#optional end before start") {
    val value: Car =
      baseCar.copy(warrantyStart = baseCar.warrantyEnd, warrantyEnd = baseCar.warrantyStart)
    val parseError = intercept[CaseClassMappingException] {
      parseCar(value)
    }

    parseError.errors.size shouldEqual 2
    val errors = parseError.errors

    errors.head.path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("warranty_end")
    errors.head.reason.message shouldEqual
      "warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]"
    errors.head.reason.detail.getClass shouldEqual classOf[ValidationError]
    errors.head.reason.detail match {
      case ValidationError(violation, ValidationError.Method, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.Unknown
        violation.getPropertyPath.toString shouldEqual "warrantyTimeValid.warrantyEnd" // the @MethodValidation annotation specifies two fields.
        violation.getMessage shouldEqual "warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]"
        violation.getInvalidValue shouldEqual value
        violation.getRootBeanClass shouldEqual classOf[Car]
        violation.getRootBean shouldEqual value // ValidationError.Method types have a root bean instance
      case _ =>
        fail()
    }

    errors(1).path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("warranty_start")
    errors(1).reason.message shouldEqual
      "warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]"
    errors(1).reason.detail.getClass shouldEqual classOf[ValidationError]
    errors(1).reason.detail match {
      case ValidationError(violation, ValidationError.Method, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.Unknown
        violation.getPropertyPath.toString shouldEqual "warrantyTimeValid.warrantyStart" // the @MethodValidation annotation specifies two fields.
        violation.getMessage shouldEqual "warrantyEnd [2015-04-09T05:17:15.000Z] must be after warrantyStart [2015-04-09T06:17:15.000Z]"
        violation.getInvalidValue shouldEqual value
        violation.getRootBeanClass shouldEqual classOf[Car]
        violation.getRootBean shouldEqual value // ValidationError.Method types have a root bean instance
      case _ =>
        fail()
    }
  }

  test("class and field level validations#no start with end") {
    val value: Car = baseCar.copy(warrantyStart = None, warrantyEnd = baseCar.warrantyEnd)
    val parseError = intercept[CaseClassMappingException] {
      parseCar(value)
    }

    parseError.errors.size shouldEqual 2
    val errors = parseError.errors

    errors.head.path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("warranty_end")
    errors.head.reason.message shouldEqual
      "both warrantyStart and warrantyEnd are required for a valid range"
    errors.head.reason.detail.getClass shouldEqual classOf[ValidationError]
    errors.head.reason.detail match {
      case ValidationError(violation, ValidationError.Method, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.Unknown
        violation.getPropertyPath.toString shouldEqual "warrantyTimeValid.warrantyEnd" // the @MethodValidation annotation specifies two fields.
        violation.getMessage shouldEqual "both warrantyStart and warrantyEnd are required for a valid range"
        violation.getInvalidValue shouldEqual value
        violation.getRootBeanClass shouldEqual classOf[Car]
        violation.getRootBean shouldEqual value // ValidationError.Method types have a root bean instance
      case _ =>
        fail()
    }

    errors(1).path shouldEqual CaseClassFieldMappingException.PropertyPath.leaf("warranty_start")
    errors(1).reason.message shouldEqual
      "both warrantyStart and warrantyEnd are required for a valid range"
    errors(1).reason.detail.getClass shouldEqual classOf[ValidationError]
    errors(1).reason.detail match {
      case ValidationError(violation, ValidationError.Method, Some(errorCode)) =>
        errorCode shouldEqual ErrorCode.Unknown
        violation.getPropertyPath.toString shouldEqual "warrantyTimeValid.warrantyStart" // the @MethodValidation annotation specifies two fields.
        violation.getMessage shouldEqual "both warrantyStart and warrantyEnd are required for a valid range"
        violation.getInvalidValue shouldEqual value
        violation.getRootBeanClass shouldEqual classOf[Car]
        violation.getRootBean shouldEqual value // ValidationError.Method types have a root bean instance
      case _ =>
        fail()
    }
  }

  test("class and field level validations#errors sorted by message") {
    val first =
      CaseClassFieldMappingException(
        CaseClassFieldMappingException.PropertyPath.Empty,
        CaseClassFieldMappingException.Reason("123"))
    val second =
      CaseClassFieldMappingException(
        CaseClassFieldMappingException.PropertyPath.Empty,
        CaseClassFieldMappingException.Reason("aaa"))
    val third = CaseClassFieldMappingException(
      CaseClassFieldMappingException.PropertyPath.leaf("bla"),
      CaseClassFieldMappingException.Reason("zzz"))
    val fourth =
      CaseClassFieldMappingException(
        CaseClassFieldMappingException.PropertyPath.Empty,
        CaseClassFieldMappingException.Reason("xxx"))

    val unsorted = Set(third, second, fourth, first)
    val expectedSorted = Seq(first, second, third, fourth)

    CaseClassMappingException(unsorted).errors shouldEqual (expectedSorted)
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
