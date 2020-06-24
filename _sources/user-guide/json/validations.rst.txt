.. _json_validations:

JSON Validation Framework
=========================

Finatra provides a simple `Validation Framework <../validation/index.html>`__ to integrate with Finatra's custom `case class` deserializer
to efficiently apply per field and method validations as request parsing is performed.

`MixIn Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
---------------------------------------------------------------------------------------------

With Jackson `MixIn Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
support it is possible to specify field-level validation annotations for case classes that are not
under your control to be annotated.

For an example on how to use `MixIn Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
with Finatra validations, see the documentation `here <./index.html#id19>`_.

.. important::

    Note this integration will only work for field-level validation annotations since method
    validations must currently be specified *inside* of the actual class to be validated.

Validation Errors
-----------------

By default validation errors are returned **alphabetically sorted** by validation error message (for determinism when testing). See the `CaseClassMappingException <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/exceptions/CaseClassMappingException.scala>`__.

Eg.,

.. code:: json

    {
      "errors" : [
        "location.lat: [9999.0] is not between -85 and 85",
        "location.long: field is required",
        "message: size [0] is not between 1 and 140",
        "nsfw: 'abc' is not a valid boolean"
      ]
    }

Bypassing Validation
--------------------

You may desire to execute validation for specific case classes in certain scenarios, but bypass validation in others.
For example, you may want to validate a `POST` request on the write path and store the JSON results somewhere, but
bypass validating that same JSON for a `GET` request on the read path.

You can create a `ScalaObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/ScalaObjectMapper.scala>`__
that will bypass validation like this:

.. code:: scala

    ScalaObjectMapper.builder.withNoValidation.objectMapper

If you desire to bypass validation in all scenarios throughout your service, you can disable validation when defining a new `ScalaObjectMapperModule <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala>`__
and replace the default `jacksonModule` in your server definition.

.. code:: scala

    object NoValidationJacksonModule extends ScalaObjectMapperModule {
      override val validation = false
    }

    class ValidationServer extends HttpServer {
      override val name = "validation-server"
      override def jacksonModule: Module = NoValidationJacksonModule
      override protected def configureHttp(router: HttpRouter): Unit = ...
    }
