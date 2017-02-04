.. _json:

Jackson Integration
===================

Finatra improves on the already excellent `jackson-module-scala <https://github.com/FasterXML/jackson-module-scala>`__
for JSON support. Note that the `finatra-jackson <https://github.com/twitter/finatra/tree/develop/jackson>`__ library can be used outside of Finatra as a replacement for the `jackson-module-scala <https://github.com/FasterXML/jackson-module-scala>`__ or `jerkson <https://github.com/codahale/jerkson>`__.

Features
--------

-  Usable outside of the Finatra framework.
-  `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__ which provides additional Scala friendly methods not found in the `ScalaObjectMapper`.
-  `Guice module <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala>`__ for injecting the `FinatraObjectMapper` with support for customization e.g. snake\_case vs. camelCase.
-  Custom `case class deserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/jackson/FinatraCaseClassDeserializer.scala>`__ which overcomes limitations in `jackson-module-scala <https://github.com/FasterXML/jackson-module-scala>`__.
-  Support for `case class` validations which accumulate errors (without failing fast) during JSON parsing.
-  Integration with Finatra `HTTP routing <routing.html>`__ to support binding and validation of query params, route params, and headers.
-  Utilities for comparing JSON.
-  Experimental support for iterator-based JSON stream parsing.

Configuration
-------------

The default configuration of Jackson is provided by the `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__.

The following Jackson integrations are provided by default:

-  `Joda Module <https://github.com/FasterXML/jackson-datatype-joda/blob/master/src/main/java/com/fasterxml/jackson/datatype/joda/JodaModule.java>`__
-  `Scala Module <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/java/com/fasterxml/jackson/module/scala/ScalaModule.java>`__
-  `LongKeyDeserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/serde/LongKeyDeserializer.scala>`__: allows for deserializing maps with long keys.
-  `Wrapped Value Serializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/wrapped/WrappedValueSerializer.scala>`__
-  `Duration Millis Serializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/serde/DurationMillisSerializer.scala>`__
-  `Improved DateTime Deserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/serde/FinatraDatetimeDeserializer.scala>`__
-  `Improved `case class Deserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/jackson/FinatraCaseClassDeserializer.scala>`__: See details `below <#improved-case-class-deserializer>`__.

Customization
-------------

To override defaults or provide other config options, specify your own module (usually extending `FinatraJacksonModule <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala>`__).

.. code:: scala

    class Server extends HttpServer {
      override def jacksonModule = CustomJacksonModule
      ...
    }

For an example of extending the `FinatraJacksonModule` see `Adding a Custom Serializer or Deserializer`_.

See the `Framework Modules <http/server.html#framework-modules>`__ section for more information on customizing server framework modules.

Adding a Custom Serializer or Deserializer
------------------------------------------

To register a custom serializer or deserializer:

- Create a module that extends the `com.fasterxml.jackson.databind.module.SimpleModule`. You can choose to instantiate an anonymous module but this is not recommended as it is not re-usable.
- Add your serializer or deserializer using the `SimpleModule#addSerializer` or `SimpleModule#addDeserializer` methods in your module.
- In your custom FinatraJacksonModule extension, add the module to list of additional jackson modules by overriding the `additionalJacksonModules`.
- Set your custom FinatraJacksonModule extension as the framework Jackson module as detailed above in the `Customization`_ section.

For example,

.. code:: scala

    // custom deserializer
    class FooDeserializer extends com.fasterxml.jackson.databind.JsonDerializer[Foo] {
      override def deserialize(...)
    }

    // Jackson SimpleModule for custom deserializer
    class FooDeserializerModule extends com.fasterxml.jackson.databind.module.SimpleModule {
      addDeserializer(FooDeserializer)
    }

    // custom FinatraJacksonModule which replace the framework module
    object CustomJacksonModule extends FinatraJacksonModule {
      override val additionalJacksonModules = Seq(
        new SimpleModule {
          addSerializer(LocalDateParser)
        },
        new FooDeserializerModule)

      override val serializationInclusion = Include.NON_EMPTY

      override val propertyNamingStrategy = CamelCasePropertyNamingStrategy

      override def additionalMapperConfiguration(mapper: ObjectMapper) {
        mapper.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true)
      }
    }

For more information see the Jackson documentation for `Custom Serializer <http://wiki.fasterxml.com/JacksonHowToCustomSerializers>`__ or `Custom De-serializers <http://wiki.fasterxml.com/JacksonHowToCustomDeserializers>`__.


Improved `case class` deserializer
------------------------------------

Finatra provides a custom `case class deserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/jackson/FinatraCaseClassDeserializer.scala>`__ which overcomes limitations in jackson-scala-module:

-  Throws a `JsonException` when required fields are missing from the parsed JSON.
-  Use default values when fields are missing in the incoming JSON.
-  Properly deserialize a `Seq[Long]` (see https://github.com/FasterXML/jackson-module-scala/issues/62).
-  Support `"wrapped values" <http://docs.scala-lang.org/overviews/core/value-classes.html>`__ using `WrappedValue` (needed since `jackson-module-scala <https://github.com/FasterXML/jackson-module-scala>`__ does not support the ``@JsonCreator`` annotation).
-  Support for accumulating JSON parsing errors (instead of failing fast).
-  Support for field and method level validations which also accumulate errors.

Java Enums
----------

We recommend the use of `Java Enums <https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html>`__ for representing enumerations since they integrate well with Jackson's ObjectMapper and now have exhaustiveness checking as of Scala 2.10. 

The following `Jackson annotations <https://github.com/FasterXML/jackson-annotations>`__ may be useful when working with Enums:

-  ``@JsonCreator`` can be used for a custom fromString method
-  ``@JsonValue`` can be used for on an overridden toString method