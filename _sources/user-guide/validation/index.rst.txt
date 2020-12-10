.. _validation:

Validation Framework
====================

Finatra provides a simple validation framework inspired by the `JSR-303 <https://docs.oracle.com/javaee/6/tutorial/doc/gircz.html>`__
and `JSR-380 <https://beanvalidation.org/2.0-jsr380/spec/#introduction>`__ bean validation specifications for use in
validating the fields of a Scala `case class <https://docs.scala-lang.org/tour/case-classes.html>`__.
The validation framework can be used without any other dependencies to validate instances of Scala
case classes.

Constraints
-----------

Constraints are Java annotations that can be used to annotate a case class field with specific
validation criteria. Similar to the `JSR-380 <https://beanvalidation.org/2.0-jsr380/spec/>`__
specification, the validation framework supports the following built in constraints (for adding new
Constraints, see `Defining Additional Constraints <#define-additional-constraints>`__):

-  `@AssertTrue <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/AssertTrue.java>`__
-  `@AssertFalse <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/AssertFalse.java>`__
-  `@CountryCode <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/CountryCode.java>`__
-  `@FutureTime <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/FutureTime.java>`__
-  `@PastTime <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/PastTime.java>`__
-  `@Pattern <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Pattern.java>`__
-  `@Max <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Max.java>`__
-  `@Min <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Min.java>`__
-  `@NotEmpty <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/NotEmpty.java>`__
-  `@OneOf <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/OneOf.java>`__
-  `@Range <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Range.java>`__
-  `@Size <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Size.java>`__
-  `@TimeGranularity <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/TimeGranularity.java>`__
-  `@UUID <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/UUID.java>`__
-  `@MethodValidation <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/MethodValidation.java>`__

To specify a constraints on a case class:

.. code:: scala

    case class CoolCaseClass(@NotEmpty name: String, @Size(min = 1, max = 10) things: Seq[Int])

Method Validations
------------------

A method validation is a no-arg case class method annotated with `@MethodValidation <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/MethodValidation.java>`__.
Reasons to use a method validation include:

-  For custom validation logic. `@MethodValidation`-annotated methods can be used instead of defining a reusable annotation with a validator.
-  Cross-field validations (e.g. `startDate` before `endDate`)

For an example see the `User <https://github.com/twitter/finatra/blob/d874b1a92cd2cc257bf0d170cfb46a486df3fb5d/validation/src/test/scala/com/twitter/finatra/validation/tests/caseclasses.scala#L104>`__
test case class.

The `@MethodValidation` annotation supports specifying an optional `fields` parameter to declare
which case class fields (if any) are being evaluated in the validation. If the evaluation fails, the
resulting exception will contain details about each of the fields specified in the annotation.

To specify a `@MethodValidation` on a case class field:

.. code:: scala

    case class CoolCaseClass(@PastTime start: DateTime, @FutureTime end: DateTime) {
      @MethodValidation(fields = Array("start", "end"))
      def ensureMinimumDelta: ValidationResult = {
        ValidationResult.validate(Days.daysBetween(start, end).getDays() >= 3, "dates must be at least 3 days apart")
      }
    }

.. important::

    `@MethodValidation`-annotated methods **MUST NOT** have any arguments, otherwise invocation of
    the method will fail.

    Additionally, `@MethodValidation`-annotated methods **SHOULD NOT** throw any exceptions but
    always return a `ValidationResult` (either `ValidationResult#Valid` or `ValidationResult#Invalid`).

|c.t.finatra.validation.Validator|_
-----------------------------------

The entry point to validation is the |c.t.finatra.validation.Validator|_. You can validate a case
class' fields and methods with any built-in or additional constraints.

Instantiation
~~~~~~~~~~~~~

There are `apply` functions available for creation of a |c.t.finatra.validation.Validator|_ configured
with defaults.

.. code:: scala

    val messageResolver: com.twitter.finatra.validation.MessageResolver = ???

    val validator = Validator()
    val validator = Validator(messageResolver)

.. note::

    The Finatra `HttpServer <../http/server.html#framework-modules>`__ provides a configured default
    |c.t.finatra.validation.Validator|_ which can be overridden by providing a customized
    `Validator` via a custom `TwitterModule`, and add that module to your server definition. Please
    checkout `Using a customized Validator <#using-a-customized-validator>`__ for instructions.

    Please avoid creating a new `Validator` if one is already available via the object graph.

Basic Usage
~~~~~~~~~~~

The |c.t.finatra.validation.Validator|_ provides two methods:

.. code:: scala

    com.twitter.finatra.validation.Validator#validate(obj: Any): Set[ValidationResult]
    com.twitter.finatra.validation.Validator#verify(obj: Any): Unit

`Validator#validate(obj: Any)` will return the `Set[ValidationResult]` of failed constraints for
processing. The `Validator#verify(obj: Any)` method will throw `ValidationException` if any
constraints fail. The exception will contain all failed constraint results.

Let's assume we have the following case class:

.. code:: scala

    case class Things(@Size(min = 1, max = 2) names: Seq[String])

To validate and return the `Set[ValidationResult]` of failed constraints, use

.. code:: scala

    Validator#validate(obj: Any): Set[ValidationResult]

For example:

.. code:: scala

    Welcome to Scala 2.12.12 (JDK 64-Bit Server VM, Java 1.8.0_242).
    Type in expressions for evaluation. Or try :help.

    scala> case class Things(@Size(min = 1, max = 2) names: Seq[String])
    defined class Things

    scala> val validator = Validator()
    validator: com.twitter.finatra.validation.Validator = com.twitter.finatra.validation.Validator@4aeaff64

    scala> val things = Things(names = Seq.empty[String])
    things: Things = Things(List())

    scala> validator.validate(things).mkString("\n")
    res0: String = Invalid(size [0] is not between 1 and 2,SizeOutOfRange(0,1,2),names,Some(@com.twitter.finatra.validation.constraints.Size(min=1, max=2)))

    scala> // and with a valid instance

    scala> val things2 = Things(Seq("hello", "world"))
    things2: Things = Things(List(hello, world))

    scala> validator.validate(things2)
    res1: Set[com.twitter.finatra.validation.ValidationResult] = Set()

To instead throw an exception when validating, use

.. code:: scala

    Validator#verify(obj: Any): Unit

For example, assuming the same `Things` case class defined above:

.. code:: scala

    Welcome to Scala 2.12.12 (TwitterJDK 64-Bit Server VM, Java 1.8.0_242).
    Type in expressions for evaluation. Or try :help.

    scala> val validator = Validator()
    validator: com.twitter.finatra.validation.Validator = com.twitter.finatra.validation.Validator@7b3c2ae0

    scala> val things = Things(names = Seq.empty[String])
    things: Things = Things(List())

    scala> validator.verify(things)
    com.twitter.finatra.validation.ValidationException:
    Validation Errors:              names: size [0] is not between 1 and 2

    scala> // and with a valid instance

    scala> val things2 = Things(Seq("hello", "world"))
    things2: Things = Things(List(hello, world))

    scala> validator.verify(things2)

    scala>

.. note::

    The list of `ValidationException#errors <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/ValidationException.scala>`__
    will be sorted in alphabetical order by their message -- which starts with the field name, meaning
    that failures for the same field will be collated together when the `ValidationException#getMessage`
    is called.

Advanced Configuration
~~~~~~~~~~~~~~~~~~~~~~

To apply more custom configuration to create a |c.t.finatra.validation.Validator|_, there is a
builder for constructing a customized validator.

E.g., to set a `MessageResolver` different than the default:

.. code:: scala

    val validator = Validator.builder
      .withMessageResolver(new MyCustomMessageResolver)

You can also leverage `Validator.Builder` to customize more attributes of the `Validator` including
`cacheSize`. Please see the `example <#using-a-customized-validator>`__ on how to use `Builder`.

Requirements on classes to be validated
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Objects hosting constraints and expecting to be validated by the Validation framework must fulfill
the following requirements:

- Constraints must be applied to declared fields and method validations to declared methods.
- Static fields and static methods are excluded from validation.
- Constraints can be applied to interfaces and superclasses.

Object validation
~~~~~~~~~~~~~~~~~

This is not supported. Constraints must be defined on a field or property of a case class and not on
the case class itself. E.g., the following not supported will not have any effect:

.. code:: scala

    @Min(1) // THIS DOES NOT APPLY
    case class WrappedValue(int: Int)

Field and property validation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Constraints can be defined on any case class field which is reachable via `class.getDeclaredFields`.
Note that constraints can be inherited by members which are overriding or implementing a member defined
in a superclass (see next section).

Inheritance (interface and superclass)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A constraint declaration can be placed on a trait. For a given class, constraint declarations
held on superclasses as well as interfaces are evaluated by the `Validator`.

The effect of constraint declarations is cumulative. Constraints declared on a superclass
will be validated along with any constraints defined on an overridden version of the field
according to the Java Language Specification visibility rules.

Graph validation
~~~~~~~~~~~~~~~~

In addition to supporting instance validation, validation of graphs of objects is also supported.
The result of a graph validation is returned as a unified set of constraint violations. `@Valid <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/Valid.java>`__
is used to express validation traversal of an association.

Consider the situation where case class `X` contains a field of type `Y` which is another case class.
By annotating the case class `Y` with constraint annotations, the Validator will validate `Y`
(and its properties) when `X` is validated. The exact type `Z` of the value contained in the
field declared of type `Y` (subclass, implementation) is determined at runtime. The constraint
definitions of `Z` are used. This ensures proper polymorphic behavior for associations marked
with `@Valid`. E.g.,

.. code:: scala

    case class Bar(@Max(1000) id: Int, @NonEmpty name: String)

    case Foo(@Min(1) id: Int, @Valid bar: Bar)

When validating, `Foo`, the annotated fields of `Bar` will also be validated.

Collections
^^^^^^^^^^^

Collection-valued, array-valued and generally `Iterable[+A]` fields may also be decorated with the
`@Valid` annotation. This causes **all contents** of the collection to be validated. Any
collection type implementing `scala.Iterable[+A]` is supported. E.g.,

.. code:: scala

    case class Bar(@Max(1000) id: Int, @NonEmpty name: String)
    case Foo(@Min(1) id: Int, @Valid bars: Seq[Bar])

The `Validator` will properly validate each `Bar` instance in the `Seq[Bar]`. Note that if you have
instances with large collections or numerous collections, you will pay at a minimum the cost of
linear-time validation for each collection.

It is also worth noting this does not take into account any laziness defined for the property which
can lead to undesirable behavior in some cases. For instance if accessing the field state triggered
a load from the database. As such, it is recommended that classes used for validation
**be side-effect free under property access**.

If validation fails, the `ValidationResult` will contain the index in the collection in addition to
the field name. For instance, when validating `Foo`, if the *first* `Bar` instance has an empty `name`
field, the `Path` would be: `bars.0.name`.

.. important::

    Only `Iterable[+A]` types are supported and property access should be **side-effect free**. Annotating
    the collection with `@Valid` will cascade validation to **every instance in the collection**.

`Option[_]`
^^^^^^^^^^^

The `Validator` will always "unwrap" a field of type `Option[_]` by default for validation,
meaning that when a case class field is of type `Option[_]` any defined constraints will apply to
the contained type. E.g. for the case class, `Bar` defined:

.. code:: scala

    case class Bar(@Min(10) b: Option[Int])

When validated, the `b` field will only be validated when the valueis defined, otherwise the
validation will be skipped. In this manner the annotation can be thought of as being applied to the
contained type for the `Option[_]` but note that the default constraints will only for known types
and not general case classes, thus this would do nothing:

.. code:: scala

    case class Foo(i: Int)
    case class Bar(@Min(10) b: Option[Foo])

And result in an error from the `MinConstraintValidator <https://github.com/twitter/finatra/blob/bfb6e22c9260eb3150b6768d6628ee6b3498183c/validation/src/main/scala/com/twitter/finatra/validation/constraints/MinConstraintValidator.scala#L42>`__
since it does not support the `Foo` type. However, you can still trigger graph validation of the
contained case class within an option using the `@Valid` annotation, e.g.,

.. code:: scala

    case class Foo(@Min(10) i: Int)
    case class Bar(@Valid b: Option[Foo])

This would properly cascade the validation to the contained case class type of the `Option` field,
`Foo` which is annotated with a field constraint. If the `Foo` instance failed validation, the
`Path` would be reported as: `b.foo.i` where the contained class is used to denote the coordinate of
the failed field.

Maps
^^^^

Validation of case class field of type `Map[T, U]` or other multi-typed collections is
**not supported** for validation. For example, case classes defined:

.. code:: scala

    case class Bar(@Max(1000) id: Int, @NonEmpty name: String)
    case class Baz(@NotEmpty id: String)

    case Foo(@Min(1) id: Int, @Valid barBaz: Map[Bar, Baz])

The `Validator` *will not* validate the key nor value elements of `Bar` or `Baz` for the `barbaz` field.

However, constraints *on* the `barBaz` field will be applied, e.g.,

.. code:: scala

    case class Bar(@Max(1000) id: Int, @NonEmpty name: String)
    case class Baz(@NotEmpty id: String)

    case Foo(@Min(1) id: Int, @NotEmpty @Valid barBaz: Map[Bar, Baz])

The `Validator` *will* evaluate the `@NotEmpty` constraint on field `barBaz`. Annotated unsupported
types will be ignored during validation.

.. important::

    Any unsupported cascade associations denoted with an `@Valid` annotated field will be ignored
    for validation.

Generic Case Classes
^^^^^^^^^^^^^^^^^^^^

Additionally, using `@Valid` on an generically typed field **is unsupported**. E.g.,

.. code:: scala

    case class Baz(@NotEmpty id: String)
    case class GenericCaseClass[T](@Valid data: T)

    val obj = GenericCaseClass(data = Baz(id = "1234"))

    validator.verify(obj)

In this case, the runtime type of `T` is not guaranteed to be discernible when inspecting
`obj.getClass` type due to type erasure and thus validation on the `Baz` type for the `data` field
will be skipped.

Validation routine
~~~~~~~~~~~~~~~~~~

The validation routine applied on a given case class instance will execute the following validations
in no particular order:

- for all *reachable* fields (declared field within the case class), execute all field level
  validations (including the ones expressed on superclasses).
- for all *reachable* methods (declared method within the case class), execute all method level
  validations (including the ones expressed on superclasses).
- for all *reachable* and *cascadable* (annotated with `@Valid`) associations, execute all cascading
  validations including the ones expressed on interfaces and superclasses (this includes both field
  and method validations).

Defining Additional Constraints
-------------------------------

To define new constraints, you can extend the `Constraint <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/Constraint.java>`__
interface, and define a matching `ConstraintValidator <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/ConstraintValidator.scala>`__
that is referred to in then`validatedBy` field of the new constraint definition.

Define an additional `Constraint`:

.. code:: java

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    import com.twitter.finatra.validation.Constraint;

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = StateConstraintValidator.class)
    public @interface StateConstraint {}

Define an additional `ConstraintValidator`:

.. code:: scala

    import com.twitter.finatra.validation.{ConstraintValidator, MessageResolver, ValidationResult}

    class StateConstraintValidator(messageResolver: MessageResolver)
        extends ConstraintValidator[StateConstraint, String](messageResolver) {

      override def isValid(annotation: StateConstraint, value: String): ValidationResult =
        ValidationResult.validate(
          value.equalsIgnoreCase("CA"),
          "Please register with state CA"
        )
    }

The validation framework will locate the new `StateConstraint` and perform the validation logic
defined in its matching `StateConstraintValidator` automatically at run time.

Using a customized `Validator`
------------------------------

You can switch to use a different `MessageResolver` or change the `cacheSize` of the default
`Validator`.

Provide a customized Validator in a `TwitterModule`. The easiest way to do so is to extend the
`c.t.finatra.validation.ValidatorModule <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/ValidatorModule.scala>`_
and implement the `ValidatorModule#configureValidator` method:

.. code:: scala

    import com.twitter.finatra.validation.{Validator, ValidatorModule}
    import com.twitter.inject.Injector

    object CustomizedValidatorModule extends ValidatorModule {
      override def configureValidator(injector: Injector, builder: Validator.Builder): Validator.Builder =
        builder
          .withCacheSize(512)
          .withMessageResolver(new CustomizedMessageResolver())
    }

Use your new module as the implementation of `validatorModule` in your `server definition <../http/server.html#framework-modules>`__:

.. code:: scala

    ValidationServer extends HttpServer {
      override val name: String = "validation-server"
      override def validatorModule: TwitterModule = CustomizedValidatorModule

      override protected def configureHttp(router: HttpRouter): Unit = {
        router
          .filter[CommonFilters]
          .add[ValidationController]
          ...
      }
    }

.. import::

    By overriding the default `validatorModule` in an `HttpServer <../http/server.html#framework-modules>`__,
    you are also replacing the default `Validator` used by the `ScalaObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/ScalaObjectMapper.scala>`__
    provided by the default framework `jacksonModule <https://github.com/twitter/finatra/blob/ef7197324cba8dfe6274ffb4570dea6c34b9fc33/http/src/main/scala/com/twitter/finatra/http/servers.scala#L526>`__.

    The newly defined `Validator` will be used to apply the validation logic in the `ScalaObjectMapper`__.

    See `Integration with Finatra Jackson Support <#integration-with-finatra-jackson-support>`__
    for more information about how validations works in Finatra Jackson Framework.

Integration with Finatra Jackson Support
----------------------------------------

The validation framework integrates with Finatra's improved `case class` `deserializer <../json/index.html#improved-case-class-deserializer>`__
to efficiently apply field and method validations. For more information, please refer to
`JSON Validation Framework <../json/validations.html>`__.

The Jackson support is also integrated with Finatra's `HTTP Routing <../json/routing.html>`__ to
allow for validation of JSON request bodies.

For example, if you have the following HTTP `custom request case class <../http/requests.html#custom-case-class-request-object>`__
defined:

.. code:: scala

    case class ValidateUserRequest(
      @NotEmpty @Pattern(regexp = "[a-z]+") userName: String,
      @Max(value = 9999) id: Long,
      title: String
    )

And in your HTTP controller, you define a `POST` endpoint:

.. code:: scala

    post("/validate_user") { _: ValidateUserRequest =>
      ...
    }

When you perform a `POST` request with a content-type of `application/json` to the `/validate_user/`
endpoint, Finatra will deserialize the request body passed into a `ValidationUserRequest` case class,
which will perform validations of the annotated fields as part of instance construction.

If any validation constraint fails, a `CaseClassMappingException <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/exceptions/CaseClassMappingException.scala>`__
will be thrown which is handled by the `CaseClassExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/json/CaseClassExceptionMapper.scala>`__
in a Finatra HTTP server by default to translate the exception into a suitable HTTP response.

Best Practices, Guidance & Limitations
--------------------------------------

- When defining constraints, case classes MUST be defined with all constructors args publicly visible.
  E.g., a constructor defined like so:

  .. code:: scala

      case class DoublePerson(@NotEmpty name: String)(@NotEmpty otherName: String)

  will throw an `IllegalArgumentException` if passed to the `Validator`. The reason being that the
  second parameter list fields are by default not publicly visible. If you find you need to separate
  a constructor like so, you will need to explicitly make the fields in the second parameter list
  visible for validation:

  .. code:: scala

      case class DoublePerson(@NotEmpty name: String)(@NotEmpty val otherName: String)

  or reference the second argument somewhere within the body of the case class which has the effect
  of making the second argument list visible via reflection as a declared field, e.g.,

  .. code:: scala

      case class DoublePerson(@NotEmpty name: String)(@NotEmpty otherName: String) {
        def someMethod: String =
          s"This references the second parameter argument: $otherName"
      }
- Case classes used for validation be side-effect free under property access. That is, accessing a
  case class field should be able to be done eagerly and without side-effects.
- Case classes with generic type params are **not supported** for validation. E.g.,

  .. code:: scala

      case class GenericCaseClass[T](@NotEmpty @Valid data: T)

  This may appear to work for some category of data but caching of reflection data does not discriminate
  on the type binding and thus validation of genericized case classes is not guaranteed to be
  successful for differing type bindings of a given genericized class. Note that this case works in
  the Jackson `integration <../json/index.html#improved-case-class-deserializer>`__ due to different
  caching semantics and how Jackson deserialization works where the binding type information is known.
- While `Iterable` collections are supported for validation when annotated with `@Valid`, this does
  not include `Map` types. Annotated `Map` types will be ignored during validation.
- More generally, types with multiple type params, e.g. `Either[T, U]`, are not supported for validation
  of contents when annotated with `@Valid`. Annotated unsupported types will be **ignored** during validation.

.. |c.t.finatra.validation.Validator| replace:: `c.t.finatra.validation.Validator`
.. _c.t.finatra.validation.Validator: https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/Validator.scala
