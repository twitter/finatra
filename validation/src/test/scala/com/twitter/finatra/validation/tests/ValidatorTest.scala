package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.ValidationResult.Invalid
import com.twitter.finatra.validation.constraints.NotEmpty
import com.twitter.finatra.validation.internal.AnnotatedField
import com.twitter.finatra.validation.tests.caseclasses._
import com.twitter.finatra.validation.{
  ErrorCode,
  MessageResolver,
  Path,
  ValidationException,
  ValidationResult,
  Validator
}
import com.twitter.inject.Test
import com.twitter.inject.conversions.time._
import java.lang.annotation.Annotation
import java.util.UUID
import org.joda.time.DateTime
import scala.collection.mutable
import scala.reflect.ClassTag

private object CustomizedMessageResolver {
  private val Message = "Whatever you provided is wrong."
}

class CustomizedMessageResolver extends MessageResolver {
  import CustomizedMessageResolver._

  override def resolve(clazz: Class[_ <: Annotation], values: Any*): String = Message
  override def resolve[Ann <: Annotation: ClassTag](values: Any*): String = Message
}

class ValidatorTest extends Test with AssertValidation {

  override protected val validator: Validator = Validator.builder.build()
  private[this] val DefaultAddress =
    Address(line1 = "1234 Main St", city = "Anywhere", state = "CA", zipcode = "94102")

  /*
   * Builder withMessageResolver(...) tests
   */
  test(
    "withMessageResolver should return a new Validator with all validators built with the given resolver") {
    val customizedMessageResolver = new CustomizedMessageResolver()

    val validator = Validator.builder
      .withMessageResolver(customizedMessageResolver)
      .build()
    val testUser: User = User(id = "", name = "April", gender = "F")
    val fieldAnnotation: Option[Annotation] =
      getValidationAnnotations(classOf[User], "id").headOption

    val validationResult = validator.validate(testUser)
    validationResult.size should be(1)
    validationResult.head should equal(
      Invalid(
        message = "Whatever you provided is wrong.",
        code = ErrorCode.ValueCannotBeEmpty,
        path = Path("id"),
        annotation = fieldAnnotation
      )
    )
  }

  /*
   * Builder withCacheSize(...) tests
   */
  test("withCacheSize should override the default cache size") {
    val customizedCacheSize: Long = 512
    val validator = Validator.builder
      .withCacheSize(customizedCacheSize)
    validator.cacheSize should be(customizedCacheSize)
  }

  test("validate returns valid result") {
    val testUser: User = User(id = "9999", name = "April", gender = "F")
    validator.validate(testUser) should equal(Set.empty[ValidationResult])
  }

  test("validate returns valid result even when case class has other fields") {
    val testPerson: Person = Person(id = "9999", name = "April", address = DefaultAddress)
    validator.validate(testPerson) should equal(Set.empty[ValidationResult])
  }

  test("validate returns invalid result") {
    val testUser: User = User(id = "", name = "April", gender = "F")
    val fieldAnnotation = getValidationAnnotations(classOf[User], "id").headOption
    val validationResult = validator.validate(testUser)
    validationResult.size should be(1)
    validationResult.head should equal(
      Invalid(
        message = "cannot be empty",
        code = ErrorCode.ValueCannotBeEmpty,
        path = Path("id"),
        annotation = fieldAnnotation
      )
    )
  }

  test("validate returns invalid result of both field validations and method validations") {
    val testUser: User = User(id = "", name = "", gender = "Female")

    val idAnnotation: Option[Annotation] =
      getValidationAnnotations(classOf[User], "id").headOption
    val genderAnnotation: Option[Annotation] =
      getValidationAnnotations(classOf[User], "gender").headOption
    val methodValidation: Array[Annotation] = classOf[User]
      .getMethod("nameCheck")
      .getAnnotations

    val validationResult = validator.validate(testUser)
    validationResult.size should be(3)
    validationResult should contain(
      Invalid(
        message = "cannot be empty",
        code = ErrorCode.ValueCannotBeEmpty,
        path = Path("id"),
        annotation = idAnnotation
      )
    )
    validationResult should contain(
      Invalid(
        message = "[Female] is not one of [F,M,Other]",
        code = ErrorCode.InvalidValues(Set("Female"), Set("F", "M", "Other")),
        path = Path("gender"),
        annotation = genderAnnotation
      )
    )
    validationResult should contain(
      Invalid(
        message = "cannot be empty",
        path = Path("name"),
        annotation = methodValidation.headOption
      )
    )
  }

  test("Validator should register the user defined constraintValidator") {
    val testState: StateValidationExample = StateValidationExample(state = "NY")
    val fieldAnnotation: Option[Annotation] =
      getValidationAnnotations(classOf[StateValidationExample], "state").headOption

    val validationResult = validator.validate(testState)
    validationResult.size should be(1)
    validationResult.head should equal(
      Invalid(
        message = "Please register with state CA",
        code = ErrorCode.Unknown,
        path = Path("state"),
        annotation = fieldAnnotation
      )
    )
  }

  test("validateField returns valid result") {
    val annotations: Array[Annotation] =
      getValidationAnnotations(classOf[MinIntExample], "numberValue")

    val validationResult =
      validator.validateField(2, "numberValue", annotations.map(validator.findFieldValidator))
    // valid results are not returned
    validationResult should equal(mutable.ListBuffer.empty)
  }

  test("validateField returns invalid result") {
    val annotations: Array[Annotation] =
      getValidationAnnotations(classOf[MinIntExample], "numberValue")

    val validationResult =
      validator.validateField(0, "numberValue", annotations.map(validator.findFieldValidator))
    validationResult.size should be(1)
    validationResult.head should be(
      Invalid(
        message = "[0] is not greater than or equal to 1",
        code = ErrorCode.ValueTooSmall(1, 0),
        path = Path("numberValue"),
        annotation = annotations.headOption
      )
    )
  }

  test("validateMethods returns valid result") {
    val testUser: User = User(id = "9999", name = "April", gender = "F")

    val validationResult =
      validator.validateMethods(testUser, Validator.getMethodValidations(classOf[User]))
    // valid results are not returned
    validationResult should be(Set.empty)
  }

  test("validateMethods returns invalid result 1") {
    val testUser: User = User(id = "1234", name = "", gender = "M")

    val validationResult =
      validator.validateMethods(testUser, Validator.getMethodValidations(classOf[User]))
    val methodValidation: Array[Annotation] = classOf[User]
      .getMethod("nameCheck")
      .getAnnotations

    validationResult.size should be(1)
    validationResult.head should equal(
      Invalid(
        message = "cannot be empty",
        path = Path("name"), // @MethodValidation specifies field "name"
        annotation = methodValidation.headOption
      )
    )
  }

  test("validateMethods returns invalid result 2") {
    val start = DateTime.now()
    val end = start.minusYears(1)

    val testCar = Car(
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

    val validationResult: Set[ValidationResult] =
      validator.validateMethods(testCar, Validator.getMethodValidations(classOf[Car])).toSet

    validationResult.size should be(4)
    validationResult should contain(
      Invalid(
        message = "id may not be even",
        code = ErrorCode.Unknown,
        path = Path("validateId"),
        annotation = classOf[Car].getMethod("validateId").getAnnotations.headOption
      )
    )

    // the path is the specified field name (ownershipEnd) and not the method name (ownershipTimesValid)
    validationResult should contain(
      Invalid(
        message = "ownershipEnd [%s] must be after ownershipStart [%s]"
          .format(end.utcIso8601, start.utcIso8601),
        code = ErrorCode.Unknown,
        path = Path("ownershipEnd"),
        annotation = classOf[Car].getMethod("ownershipTimesValid").getAnnotations.headOption
      )
    )

    // invalid results are repeated per field specified in the method validation -- here for the two fields
    validationResult should contain(
      Invalid(
        message = "both warrantyStart and warrantyEnd are required for a valid range",
        code = ErrorCode.Unknown,
        path = Path("warrantyEnd"),
        annotation = classOf[Car].getMethod("warrantyTimeValid").getAnnotations.headOption
      )
    )
    validationResult should contain(
      Invalid(
        message = "both warrantyStart and warrantyEnd are required for a valid range",
        code = ErrorCode.Unknown,
        path = Path("warrantyStart"),
        annotation = classOf[Car].getMethod("warrantyTimeValid").getAnnotations.headOption
      )
    )
  }

  test("secondary case class constructors") {
    // the framework does not validate on construction, however, it must use
    // the case class constructor for finding the field validation annotations
    // as scala carries case class annotations on the constructor for fields specified
    // and annotated in the constructor. validations only apply to constructor parameters that are
    // also class fields, e.g., from the primary constructor.

    assertValidation(TestJsonCreator("42"))
    assertValidation(TestJsonCreator(42))
    assertValidation(TestJsonCreator2(scala.collection.immutable.Seq("1", "2", "3")))
    assertValidation(
      TestJsonCreator2(scala.collection.immutable.Seq(1, 2, 3), default = "Goodbye, world"))

    assertValidation(TestJsonCreatorWithValidation("42"))
    intercept[NumberFormatException] {
      // can't validate after the fact -- the instance is already constructed, then we validate
      assertValidation(TestJsonCreatorWithValidation(""))
    }
    assertValidation(TestJsonCreatorWithValidation(42))

    // annotations are on primary constructor
    assertValidation(
      TestJsonCreatorWithValidations("99"),
      withErrors = Seq("int: [99] is not one of [42,137]")
    )
    assertValidation(TestJsonCreatorWithValidations(42))

    assertValidation(new CaseClassWithMultipleConstructors("10001", "20002", "30003"))
    assertValidation(CaseClassWithMultipleConstructors(10001L, 20002L, 30003L))

    assertValidation(CaseClassWithMultipleConstructorsAnnotated(10001L, 20002L, 30003L))
    assertValidation(new CaseClassWithMultipleConstructorsAnnotated("10001", "20002", "30003"))

    assertValidation(
      CaseClassWithMultipleConstructorsAnnotatedAndValidations(
        10001L,
        20002L,
        UUID.randomUUID().toString))

    assertValidation(
      new CaseClassWithMultipleConstructorsAnnotatedAndValidations(
        "10001",
        "20002",
        UUID.randomUUID().toString))

    assertValidation(
      new CaseClassWithMultipleConstructorsPrimaryAnnotatedAndValidations(
        "9999",
        "10001",
        UUID.randomUUID().toString),
      withErrors = Seq("number1: [9999] is not greater than or equal to 10000")
    )

    val d = InvalidDoublePerson("Andrea")("")
    intercept[IllegalArgumentException] {
      assertValidation(d)
    }
    val d2 = DoublePerson("Andrea")("")
    assertValidation(d2, withErrors = Seq("otherName: cannot be empty"))
    val d3 = ValidDoublePerson("Andrea")("")
    val e = intercept[Exception] {
      validator.verify(d3)
    }
    e match {
      case ve: ValidationException =>
        val errors = Seq(
          "checkOtherName: otherName must be longer than 3 chars",
          "otherName: cannot be empty"
        )
        assertValidationException(ve, errors)
      case _ => fail(e.getMessage, e)
    }

    val d4 = PossiblyValidDoublePerson("Andrea")("")
    assertValidation(
      obj = d4,
      withErrors = Seq("otherName: cannot be empty")
    )
  }

  test("cycle fails") {
    intercept[IllegalArgumentException](
      validator.verify(A("5678", B("9876", C("444", null))))
    )

    intercept[IllegalArgumentException] {
      validator.verify(D("1", E("2", F("3", None))))
    }

    val e = intercept[IllegalArgumentException] {
      validator.verify(G("1", H("2", I("3", Seq.empty[G]))))
    }
    e.getMessage should equal(s"Cycle detected at class ${classOf[G].getName.stripSuffix("$")}.")
  }

  test("scala BeanProperty") {
    val value = AnnotatedBeanProperties(Seq(1, 2, 3))
    value.setField1("")
    assertValidation(
      value,
      withErrors = Seq("field1: cannot be empty")
    )
  }

  test("case class with no constructor params") {
    val value = NoConstructorParams()
    value.setId("1234")
    assertValidation(value)

    assertValidation(
      NoConstructorParams(),
      withErrors = Seq("id: cannot be empty")
    )
  }

  test("case class annotated non constructor field") {
    assertValidation(
      AnnotatedInternalFields("1234", "thisisabigstring"),
      withErrors = Seq("company: cannot be empty")
    )
  }

  test("inherited validation annotations") {
    assertValidation(
      ImplementsAncestor(""),
      withErrors = Seq(
        "field1: cannot be empty",
        "field1: not a double value"
      )
    )

    assertValidation(
      ImplementsAncestor("blimey"),
      withErrors = Seq("field1: not a double value")
    )

    assertValidation(ImplementsAncestor("3.141592653589793d"))
  }

  test("annotations") {
    val annotatedClazz = validator.findAnnotatedClass(classOf[TestAnnotations])
    annotatedClazz.members.length should equal(1)
    annotatedClazz.members.head.isInstanceOf[AnnotatedField] should be(true)
    annotatedClazz.members.head.asInstanceOf[AnnotatedField].fieldValidators.length should equal(1)
    annotatedClazz.members.head
      .asInstanceOf[AnnotatedField].fieldValidators.head.annotation.annotationType() should be(
      classOf[NotEmpty])
  }

  test("generics") {
    // generics are unsupported, thus validation is essentially a no-op but should not error
    val annotatedClazz =
      validator.findAnnotatedClass(classOf[GenericTestCaseClass[_]])
    annotatedClazz.members.length should equal(1)
    val value = GenericTestCaseClass(data = "Hello, World")
    val results = validator.validate(value)
    results.isEmpty should be(true)

    val annotatedClazz1 =
      validator.findAnnotatedClass(classOf[GenericTestCaseClassMultipleTypes[_, _, _]])
    annotatedClazz1.members.length should equal(3)

    val value1 = GenericTestCaseClassMultipleTypes(
      data = None,
      things = Seq(1, 2, 3),
      otherThings = Seq(
        UUIDExample("1234"),
        UUIDExample(UUID.randomUUID().toString),
        UUIDExample(UUID.randomUUID().toString))
    )
    val results1 = validator.validate(value1)
    results1.isEmpty should be(true)
  }

  private[this] def getValidationAnnotations(
    clazz: Class[_],
    fieldName: String
  ): Array[Annotation] =
    validator
      .findAnnotatedClass(clazz)
      .getAnnotationsForAnnotatedMember(fieldName)

}
