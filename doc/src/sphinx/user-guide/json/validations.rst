.. _json_validations:

JSON Validation Framework
=========================

Finatra provides a simple validation framework inspired by `JSR-303 <https://docs.oracle.com/javaee/6/tutorial/doc/gircz.html>`__.

The validations framework integrates Finatra's custom `case class` deserializer to efficiently apply per field validations as request parsing is performed. The following validation annotations are available (and additional validations can be easily created):

-  ``@CountryCode``
-  ``@FutureTime``
-  ``@PastTime``
-  ``@Pattern``
-  ``@Max``
-  ``@Min``
-  ``@NotEmpty``
-  ``@OneOf``
-  ``@Range``
-  ``@Size``
-  ``@TimeGranularity``
-  ``@UUID``
-  ``@MethodValidation``

Method Validations
------------------

A method validation is a case class method annotated with ``@MethodValidation`` which is intended to be used for validating fields of the cases class during request parsing. Reasons to use a method validation include:

-  For non-generic validations. ``@MethodValidation`` can be used instead of defining a reusable annotation and validator.
-  Cross-field validations (e.g. `startDate` before `endDate`)

For an example see the `Car <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/jackson/src/test/scala/com/twitter/finatra/json/tests/internal/caseclass/validation/domain/Car.scala#L26>`__ test case class. Additionally, see the
`CommonMethodValidations <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/validation/CommonMethodValidations.scala>`__ for pre-defined commonly useful method validations.

The ``@MethodValidation`` annotation also supports specifying an optional ``fields`` parameter to
state which fields are being evaluated in the validation. If the evaluation fails the resulting
exception will contain details about each of the fields specified in the annotation.

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

You can create a `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__
that will bypass validation like this:

.. code:: scala

    def create(injector: Injector = null): FinatraObjectMapper = {
      val jacksonModule = NullValidationFinatraJacksonModule
      new FinatraObjectMapper(
        jacksonModule.provideScalaObjectMapper(injector))
    }
