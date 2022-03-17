.. _json_validations:

JSON Validation Framework
=========================

Finatra integrates with the `util-validator <https://github.com/twitter/util/tree/develop/util-validator>`__
`ScalaValidator <https://github.com/twitter/util/blob/develop/util-validator/src/main/scala/com/twitter/util/validation/ScalaValidator.scala>`__
for Scala `case class <https://docs.scala-lang.org/tour/case-classes.html>`__ field validation. For
more information on the `util-validator <https://github.com/twitter/util/tree/develop/util-validator>`__
library, please see the `documentation <http://twitter.github.io/util/guide/util-validator/index.html>`__.

`MixIn Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
---------------------------------------------------------------------------------------------

With Jackson `MixIn Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
support it is possible to specify field-level constraint annotations for case classes that are not
under your control.

For an example on how to use `MixIn Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
with validations, see the documentation `here <./index.html#id19>`_.

.. important::

    Note this integration will only work for field-level constraint annotations since method
    validations using `@MethodValidation` annotation must be specified as a method of the
    case class instance.

Validation Errors
-----------------

By default, validation errors are returned **alphabetically sorted** by validation error message (for
determinism when testing). See the `CaseClassMappingException <https://github.com/twitter/util/blob/develop/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass/exceptions/CaseClassMappingException.scala>`__.

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

You may wish to execute validation for case classes in certain scenarios, but bypass validation in
others. For example, you may want to validate a `POST` request on the write path and store the JSON
results somewhere, but bypass validating that same JSON for a `GET` request on the read path.

In your custom `ScalaObjectMapperModule <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala>`__

.. code-block:: scala
   :emphasize-lines: 4

    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    object NoValidationJacksonModule extends ScalaObjectMapperModule {
      override val validation: Boolean = false
    }

See the `Modules Configuration in Servers <../getting-started/modules.html#module-configuration-in-servers>`_
or the HTTP Server `Framework Modules <../http/server.html#framework-modules>`_ for more information
on how to make use of any custom |ScalaObjectMapperModule|_.

.. |ScalaObjectMapperModule| replace:: `ScalaObjectMapperModule`
.. _ScalaObjectMapperModule: https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala

