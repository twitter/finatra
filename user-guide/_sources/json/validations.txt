.. _json_validations:

JSON Validation Framework
=========================

Finatra provides a simple validation framework inspired by `JSR-303 <http://docs.oracle.com/javaee/6/tutorial/doc/gircz.html>`__.

The validations framework integrates Finatra's custom `case class` deserializer to efficiently apply per field validations as request parsing is performed. The following validation annotations are available (and additional validations can be easily created):

-  ``@CountryCode``
-  ``@FutureTime``
-  ``@PastTime``
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

For an example see the `Car <https://github.com/twitter/finatra/blob/develop/jackson/src/test/scala/com/twitter/finatra/json/tests/internal/caseclass/validation/domain/Car.scala#L20>`__ test case class. Additionally, see the
`CommonMethodValidations <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/validation/CommonMethodValidations.scala>`__ for pre-defined commonly useful method validations.

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