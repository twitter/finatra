package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.ValidationResult.Invalid
import com.twitter.finatra.validation.internal.AnnotatedClass
import com.twitter.finatra.validation.tests.caseclasses.{
  MinIntExample,
  Person,
  StateValidationExample,
  User
}
import com.twitter.finatra.validation.{
  ErrorCode,
  MessageResolver,
  ValidationException,
  ValidationResult,
  Validator
}
import com.twitter.inject.Test
import java.lang.annotation.Annotation

class ValidatorTest extends Test {

  val validator: Validator = Validator.builder.build()

  /*
   * Builder withMessageResolver(...) tests
   */
  test(
    "withMessageResolver should return a new Validator with all validators built with the given resolver") {
    class CustomizedMessageResolver extends MessageResolver {
      override def resolve(clazz: Class[_ <: Annotation], values: Any*): String =
        "Whatever you provided is wrong."
    }
    val customizedMessageResolver = new CustomizedMessageResolver()

    val validator = Validator.builder
      .withMessageResolver(customizedMessageResolver)
      .build()
    val testUser: User = User(id = "", name = "April", gender = "F")
    val fieldAnnotation: Option[Annotation] =
      getValidationAnnotations(classOf[User], "id").headOption
    val e = intercept[ValidationException](validator.validate(testUser))
    val validationResult = e.errors
    validationResult.size should be(1)
    validationResult.head should equal(
      Invalid(
        message = "Whatever you provided is wrong.",
        code = ErrorCode.ValueCannotBeEmpty,
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

  /*
   * validate() endpoint tests
   */
  test("validate returns valid result") {
    val testUser: User = User(id = "9999", name = "April", gender = "F")
    validator.validate(testUser) should be(())
  }

  test("validate returns valid result even when case class has other fields") {
    val testPerson: Person = Person(id = "9999", name = "April")
    validator.validate(testPerson) should be(())
  }

  test("validate returns invalid result") {
    val testUser: User = User(id = "", name = "April", gender = "F")
    val fieldAnnotation = getValidationAnnotations(classOf[User], "id").headOption
    val e = intercept[ValidationException](validator.validate(testUser))
    val validationResult = e.errors
    validationResult.size should be(1)
    validationResult.head should equal(
      Invalid(
        message = "cannot be empty",
        code = ErrorCode.ValueCannotBeEmpty,
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
    val methodValidation: Array[Annotation] = testUser.getClass
      .getMethod("nameCheck")
      .getAnnotations

    val e = intercept[ValidationException](validator.validate(testUser))
    val validationResult = e.errors
    validationResult.size should be(3)
    validationResult should contain(
      Invalid(
        message = "cannot be empty",
        code = ErrorCode.ValueCannotBeEmpty,
        annotation = idAnnotation
      )
    )
    validationResult should contain(
      Invalid(
        message = "[Female] is not one of [F,M]",
        code = ErrorCode.InvalidValues(Set("Female"), Set("F", "M")),
        annotation = genderAnnotation
      )
    )
    validationResult should contain(
      Invalid(
        message = "name cannot be empty",
        annotation = methodValidation.headOption
      )
    )
  }

  test("Validator should register the user defined constraintValidator") {
    val testState: StateValidationExample = StateValidationExample(state = "NY")
    val fieldAnnotation: Option[Annotation] =
      getValidationAnnotations(classOf[StateValidationExample], "state").headOption
    val e = intercept[ValidationException](validator.validate(testState))
    val validationResult = e.errors
    validationResult.size should be(1)
    validationResult.head should equal(
      Invalid(
        message = "Please register with state CA",
        code = ErrorCode.Unknown,
        annotation = fieldAnnotation
      )
    )
  }

  /*
   * validateField() endpoint tests
   */
  test("validateField returns valid result") {
    val annotations: Array[Annotation] =
      getValidationAnnotations(classOf[MinIntExample], "numberValue")
    val validationResult: Seq[ValidationResult] =
      validator.validateField(2, annotations.map(validator.findFieldValidator))
    // valid results are not returned
    validationResult should be(Seq.empty)
  }

  test("validateField returns invalid result") {
    val annotations: Array[Annotation] =
      getValidationAnnotations(classOf[MinIntExample], "numberValue")
    val validationResult: Seq[ValidationResult] =
      validator.validateField(0, annotations.map(validator.findFieldValidator))
    validationResult.size should be(1)
    validationResult.head should be(
      Invalid(
        message = "[0] is not greater than or equal to 1",
        code = ErrorCode.ValueTooSmall(1, 0),
        annotation = annotations.headOption
      )
    )
  }

  /*
   * validateMethods() endpoint tests
   */
  test("validateMethods returns valid result") {
    val testUser: User = User(id = "9999", name = "April", gender = "F")
    val validationResult: Seq[ValidationResult] =
      validator.validateMethods(testUser, validator.getMethodValidations(testUser.getClass))
    // valid results are not returned
    validationResult should be(Seq.empty)
  }

  test("validateMethods returns invalid result") {
    val testUser: User = User(id = "1234", name = "", gender = "M")
    val validationResult: Seq[ValidationResult] =
      validator.validateMethods(testUser, validator.getMethodValidations(testUser.getClass))
    val methodValidation: Array[Annotation] = testUser.getClass
      .getMethod("nameCheck")
      .getAnnotations

    validationResult.size should be(1)
    validationResult.head should equal(
      Invalid(
        message = "name cannot be empty",
        annotation = methodValidation.headOption
      )
    )
  }

  private[this] def getValidationAnnotations(clazz: Class[_], name: String): Array[Annotation] = {
    val AnnotatedClass(_, fields, _) = validator.getAnnotatedClass(clazz)
    fields.get(name).map(_.fieldValidators.map(_.annotation)).getOrElse(Array.empty[Annotation])
  }
}
