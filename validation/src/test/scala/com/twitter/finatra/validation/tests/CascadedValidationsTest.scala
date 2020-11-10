package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.Validator
import com.twitter.finatra.validation.tests.caseclasses._
import com.twitter.inject.Test
import com.twitter.inject.conversions.time._
import java.util.UUID
import org.joda.time.DateTime

class CascadedValidationsTest extends Test with AssertValidation {

  override protected val validator: Validator = Validator.builder.build()
  private[this] val DefaultAddress =
    Address(line1 = "1234 Main St", city = "Anywhere", state = "CA", zipcode = "94102")

  private[this] val start = DateTime.now()
  private[this] val end = start.minusYears(1)

  test("cascaded method validation - defined fields") {
    // nested method validation fields
    val owner = Person(id = "9999", name = "A. Einstein", address = DefaultAddress)
    assertValidation(
      obj = Driver(
        person = owner,
        car = Car(
          id = 1234,
          make = CarMake.Volkswagen,
          model = "Beetle",
          year = 1970,
          owners = Seq(Person(id = "9999", name = "", address = DefaultAddress)),
          licensePlate = "CA123456",
          numDoors = 2,
          manual = true,
          ownershipStart = start,
          ownershipEnd = end,
          warrantyEnd = Some(end)
        )
      ),
      withErrors = Seq(
        "car.owners.person.0.name: cannot be empty",
        "car.ownershipEnd: %s [%s] must be after %s [%s]"
          .format("ownershipEnd", end.utcIso8601, "ownershipStart", start.utcIso8601),
        "car.validateId: id may not be even",
        "car.warrantyEnd: both warrantyStart and warrantyEnd are required for a valid range",
        "car.warrantyStart: both warrantyStart and warrantyEnd are required for a valid range",
        "car.year: [1970] is not greater than or equal to 2000"
      )
    )
  }

  test("cascaded method validation - no defined fields") {
    assertValidation(
      obj = Car(
        id = 1234,
        make = CarMake.Volkswagen,
        model = "Beetle",
        year = 1970,
        owners = Seq.empty[Person], // for sale!
        licensePlate = "CA123456",
        numDoors = 2,
        manual = true,
        ownershipStart = start,
        ownershipEnd = end,
        warrantyEnd = Some(end)
      ),
      withErrors = Seq(
        "ownershipEnd: %s [%s] must be after %s [%s]"
          .format("ownershipEnd", end.utcIso8601, "ownershipStart", start.utcIso8601),
        "validateId: id may not be even",
        "warrantyEnd: both warrantyStart and warrantyEnd are required for a valid range",
        "warrantyStart: both warrantyStart and warrantyEnd are required for a valid range",
        "year: [1970] is not greater than or equal to 2000"
      )
    )
  }

  test("cascaded graph validations 1") {
    assertValidation(
      obj = Division(
        name = "Joy",
        team = Seq(
          Group(
            "1001",
            Seq(
              Person(
                "1",
                "Katherine Johnson",
                Address("1234 Any Street", city = "Somewhere", state = "PA", zipcode = "00101")),
              Person(
                "2",
                "Judith Resnik",
                Address("1234 Any Street", city = "Somewhere", state = "CA", zipcode = "00101")),
              Person(
                "3",
                "Mae Jemison",
                Address("1234 Any Street", city = "Anytown", state = "", zipcode = "00101")),
              Person(
                "4",
                "Sally Ride",
                Address("1234 Any Street", city = "Whoville", state = "CA", zipcode = "00101"))
            )
          ),
          Group(
            "2002",
            Seq(
              Person(
                "1",
                "Ada Lovelace",
                Address("1234 Any Street", city = "Somewhere", state = "PA", zipcode = "00101")),
              Person(
                "2",
                "Grace Hopper",
                Address("1234 Any Street", city = "Somewhere", state = "CA", zipcode = "00101")),
              Person(
                "3",
                "Vera Rubin",
                Address("1234 Any Street", city = "Anytown", state = "", zipcode = "00101")),
              Person(
                "4",
                "Emmy Noether",
                Address("1234 Any Street", city = "Whoville", state = "CA", zipcode = "00101")
              )
            )
          )
        )
      ),
      withErrors = Seq(
        "team.group.0.person.0.address.state: Please register with state CA",
        "team.group.0.person.2.address.state: Please register with state CA",
        "team.group.0.person.2.address: state must be specified",
        "team.group.1.person.0.address.state: Please register with state CA",
        "team.group.1.person.2.address.state: Please register with state CA",
        "team.group.1.person.2.address: state must be specified"
      )
    )
  }

  test("cascaded graph validations 2") {
    assertValidation(
      obj = Car(
        id = 1234,
        make = CarMake.Volkswagen,
        model = "Beetle",
        year = 1970,
        owners = Seq.empty[Person], // for sale!
        licensePlate = "CA123456",
        numDoors = 2,
        manual = true),
      withErrors = Seq(
        "validateId: id may not be even",
        "year: [1970] is not greater than or equal to 2000"
      )
    )
  }

  test("cascaded graph validations 3") {
    // success
    assertValidation(
      obj = CustomerAccount(
        "checking",
        Some(User("1", "Alice", "Other"))
      )
    )

    // one failure
    assertValidation(
      obj = CustomerAccount(
        "savings",
        Some(User("1", "", "Other"))
      ),
      withErrors = Seq("customer.user.name: cannot be empty")
    )
  }

  test("cascaded graph validations 4") {
    // success
    assertValidation(
      obj = NestedUser(
        id = "1234",
        person = Person(id = "9999", name = "April", address = DefaultAddress),
        gender = "F",
        job = "Writer"))

    // one failure
    assertValidation(
      obj = NestedUser(
        id = "1234",
        person = Person(id = "9999", name = "April", address = DefaultAddress),
        gender = "X",
        job = "Writer"),
      withErrors = Seq("gender: [X] is not one of [F,M,Other]")
    )

    // two failures
    assertValidation(
      obj = NestedUser(
        id = "1234",
        person = Person(id = "9999", name = "", address = DefaultAddress),
        gender = "X",
        job = "Writer"),
      withErrors = Seq(
        "gender: [X] is not one of [F,M,Other]",
        "person.name: cannot be empty"
      )
    )

    // three failures
    assertValidation(
      obj = NestedUser(
        id = "1234",
        person = Person(id = "9999", name = "", address = DefaultAddress),
        gender = "X",
        job = ""),
      withErrors = Seq(
        "gender: [X] is not one of [F,M,Other]",
        "job: cannot be empty",
        "person.name: cannot be empty"
      )
    )
  }

  test("cascaded graph validations with collection") {
    assertValidation(
      obj = Persons(
        id = UUID.randomUUID().toString,
        people = scala.collection.immutable.Seq(
          Person(id = "1", name = "Peter", address = DefaultAddress),
          Person(id = "2", name = "Paul", address = DefaultAddress),
          Person(id = "3", name = "Mary", address = DefaultAddress)
        )
      )
    )

    // first person instance is invalid
    assertValidation(
      obj = Persons(
        id = UUID.randomUUID().toString,
        people = scala.collection.immutable.Seq(
          Person(id = "1", name = "", address = DefaultAddress),
          Person(id = "2", name = "Paul", address = DefaultAddress),
          Person(id = "3", name = "Mary", address = DefaultAddress)
        )
      ),
      withErrors = Seq("people.person.0.name: cannot be empty")
    )

    // people Seq is empty which violates constraint
    assertValidation(
      obj = Persons(
        id = UUID.randomUUID().toString,
        people = scala.collection.immutable.Seq.empty[Person]),
      withErrors = Seq("people: [0] is not greater than or equal to 1")
    )

    assertValidation(
      obj = CollectionOfCollection(
        id = "1234",
        people = scala.collection.immutable.Seq(
          Persons(
            id = UUID.randomUUID().toString,
            people = scala.collection.immutable.Seq(
              Person(id = "1", name = "Peter", address = DefaultAddress),
              Person(id = "2", name = "Paul", address = DefaultAddress),
              Person(id = "3", name = "Mary", address = DefaultAddress)
            )
          ),
          Persons(
            id = UUID.randomUUID().toString,
            people = scala.collection.immutable.Seq(
              Person(id = "4", name = "Dieter", address = DefaultAddress),
              Person(id = "5", name = "Saul", address = DefaultAddress),
              Person(id = "6", name = "Gary", address = DefaultAddress)
            )
          )
        )
      )
    )

    assertValidation(
      obj = MapsAndMaps(
        id = "1234",
        bool = true,
        carAndDriver = Map(
          Person(id = "1", name = "Sara", address = DefaultAddress) -> SimpleCar(
            id = 1001L,
            CarMake.Audi,
            "A5",
            1985, // not validated
            "CA101231",
            manual = true
          )
        )
      )
    )

    assertValidation(
      obj = MapsAndMaps(
        id = "1234",
        bool = false, // fails validation
        carAndDriver = Map(
          Person(id = "1", name = "Sara", address = DefaultAddress) -> SimpleCar(
            id = 1001L,
            CarMake.Audi,
            "A5",
            1985, // not validated
            "CA101231",
            manual = true
          )
        )
      ),
      withErrors = Seq(
        "bool: [false] is not true"
      )
    )

    // validations not cascaded as the Array[User] is not annotated with @Valid
    assertValidation(
      obj = CollectionWithArray(
        names = Array("Egret"),
        users = Array(
          User(id = "", name = "Ian", gender = "Other")
        )
      )
    )

    // validations not cascaded as the Array[User] is not annotated with @Valid
    assertValidation(
      obj = CollectionWithArray(
        names = Array("Ibis"),
        users = Array(
          User(id = "33333", name = "Ian", gender = "Other"),
          User(id = "", name = "Pat", gender = "Other"),
          User(id = "55555", name = "Chris", gender = "Other"),
          User(id = "", name = "Alex", gender = "Other"),
          User(id = "77777", name = "Yan", gender = "Other")
        )
      ),
      withErrors = Seq(
        "users: [5] is not less than or equal to 2"
      )
    )
  }
}
