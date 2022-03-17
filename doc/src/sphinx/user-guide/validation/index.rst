.. _validation:

Validation Framework Integration
================================

Finatra provides an integration with the `util-validator <https://github.com/twitter/util/tree/develop/util-validator>`__
|c.t.util.validation.ScalaValidator|_ for use in validating the fields of a Scala
`case class <https://docs.scala-lang.org/tour/case-classes.html>`__.

For more information on the `util-validator <https://github.com/twitter/util/tree/develop/util-validator>`__  library,
please see the `documentation <http://twitter.github.io/util/guide/util-validator/index.html>`__.

Using a customized |ScalaValidator|_
------------------------------------

You can customize and provide a |ScalaValidator|_ via a `TwitterModule`. The easiest way to do so is
to extend the `c.t.finatra.validation.ValidatorModule <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/ValidatorModule.scala>`_
and implement the `ValidatorModule#configureValidator` method:

.. code-block:: scala
   :emphasize-lines: 7

    import com.twitter.finatra.validation.ValidatorModule
    import com.twitter.inject.Injector
    import com.twitter.util.validation.ScalaValidator
    import com.twitter.util.validation.cfg.ConstraintMapping

    object CustomizedValidatorModule extends ValidatorModule {
      override def configureValidator(injector: Injector, builder: ScalaValidator.Builder): ScalaValidator.Builder = {
        val newConstraintMapping = ConstraintMapping(
           annotationType = classOf[FooConstraintAnnotation],
           constraintValidator = classOf[FooConstraintValidator]
         )

        builder
          .withDescriptorCacheSize(512)
          .withConstraintMapping(newConstraintMapping)
      }
    }

Then include this module in your server's list of modules. In a Finatra HTTP server, a |ScalaValidator|_
is provided as a `framework module <../http/server.html#framework-modules>`__, thus you can use your new module as the implementation of
the `validatorModule` function in your `HttpServer definition <../http/server.html#framework-modules>`__:

.. code-block:: scala
   :emphasize-lines: 7

    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.inject.TwitterModule

    ServerWithCustomizedValidator extends HttpServer {
      override val name: String = "validation-server"
      override def validatorModule: TwitterModule = CustomizedValidatorModule

      override protected def configureHttp(router: HttpRouter): Unit = {
        router
          .filter[MyFilter]
          .add[MyController]
          ...
      }
    }

.. important::

    By overriding the default `validatorModule` in an `HttpServer <../http/server.html#framework-modules>`__,
    you are also replacing the default |ScalaValidator|_ used by the framework |ScalaObjectMapper|_
    provided by the default framework `jacksonModule <https://github.com/twitter/finatra/blob/ef7197324cba8dfe6274ffb4570dea6c34b9fc33/http/src/main/scala/com/twitter/finatra/http/servers.scala#L526>`__.

    The newly defined |ScalaValidator|_ will be used to apply the validation logic in the |ScalaObjectMapper|_.

    See `Integration with Finatra Jackson Support <#integration-with-finatra-jackson-support>`__
    for more information about how case class validations work in the Finatra Jackson Framework.

Integration with Finatra Jackson Support
----------------------------------------

The Finatra framework integrates with Jackson in a couple of ways.

|ScalaObjectMapper|_
~~~~~~~~~~~~~~~~~~~~

The first and most straight-forward is through the |ScalaObjectMapper|_. The |ScalaObjectMapper|_
directly integrates with the `util-validator <http://twitter.github.io/util/guide/util-validator/index.html>`__
library via the `case class` `deserializer <../json/index.html#improved-case-class-deserializer>`__
to efficiently execute field and method validations as a part of JSON deserialization. For details
on case class validation during JSON deserialization, please refer to `JSON Validation Framework <../json/validations.html>`__.

HTTP Routing
~~~~~~~~~~~~

Secondly, Jackson support is also integrated with Finatra's `HTTP Routing <../json/routing.html>`__ to
allow for validation of JSON request bodies when modeled as a `custom request case class <../http/requests.html#custom-case-class-request-object>`__.

For example, if you have the following HTTP `custom request case class <../http/requests.html#custom-case-class-request-object>`__
defined:

.. code:: scala

    import jakarta.validation.constraints.{Max, NotEmpty}
    import com.twitter.util.validation.constraints.Pattern

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

When a `POST` request with a content-type of `application/json` is sent to the `/validate_user/`
endpoint of the server, Finatra will `automatically deserialize <../http/requests.html#custom-case-class-request-object>`__
the incoming request body into a `ValidationUserRequest` case class. During deserialization the annotated
constraints will be applied to the fields as part of instance construction.

If any validation constraint fails during JSON deserialization, a `CaseClassMappingException <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/exceptions/CaseClassMappingException.scala>`__
will be thrown which is handled by the `CaseClassExceptionMapper <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/internal/exceptions/json/CaseClassExceptionMapper.scala>`__
in a Finatra `HttpServer by default <../http/exceptions.html#default-exception-mappers>`__ to translate
the exception into a suitable HTTP response.

.. note::

    Non-recoverable validation errors are generally thrown as `jakarta.validation.ValidationException` which are
    handled by the `ThrowableExceptionMapper <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/internal/exceptions/ThrowableExceptionMapper.scala>`
    in a Finatra `HttpServer by default <../http/exceptions.html#default-exception-mappers>`__ to translate
    the exception into a suitable HTTP response.

Best Practices, Guidance & Limitations
--------------------------------------

Please refer to the `util-validator <http://twitter.github.io/util/guide/util-validator/index.html>`__
documentation `for details <http://twitter.github.io/util/guide/util-validator/index.html>`__.

- Case classes used for validation should be side-effect free under property access. That is, accessing a
  case class field should be able to be done eagerly and without side-effects.
- Case classes with generic type params should be considered **not supported** for validation. E.g.,

  .. code:: scala

      case class GenericCaseClass[T](@NotEmpty @Valid data: T)

  This may appear to work for some category of data but in practice the way the `util-validator <http://twitter.github.io/util/guide/util-validator/index.html>`__
  caches reflection data does not discriminate on the type binding and thus validation of genericized
  case classes is not guaranteed to be successful for differing type bindings of a given genericized class.
  Note that this case works in the Jackson `integration <../json/index.html#improved-case-class-deserializer>`__
  due to different caching semantics and how Jackson deserialization works where the binding type
  information is known.
- While `Iterable` collections are supported for validation when annotated with `@Valid`, this does
  not include `Map` types. Annotated `Map` types will be ignored during validation.
- More generally, types with multiple type params, e.g. `Either[T, U]`, are not supported for validation
  of contents when annotated with `@Valid`. Annotated unsupported types will be **ignored** during validation.

.. |ScalaValidator| replace:: `ScalaValidator`
.. _ScalaValidator: https://github.com/twitter/util/blob/develop/util-validator/src/main/scala/com/twitter/util/validation/ScalaValidator.scala

.. |c.t.util.validation.ScalaValidator| replace:: `c.t.util.validation.ScalaValidator`
.. _c.t.util.validation.ScalaValidator: https://github.com/twitter/util/blob/develop/util-validator/src/main/scala/com/twitter/util/validation/ScalaValidator.scala

.. |ScalaObjectMapper| replace:: `ScalaObjectMapper`
.. _ScalaObjectMapper: https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/ScalaObjectMapper.scala

