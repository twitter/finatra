.. _json:

Jackson Integration
===================

Finatra builds upon the excellent |jackson-module-scala|_ for `JSON <https://en.wikipedia.org/wiki/JSON>`_
support by wrapping the `Jackson <https://github.com/FasterXML/jackson>`__ `ScalaObjectMapper <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/scala/com/fasterxml/jackson/module/scala/ScalaObjectMapper.scala>`__.
One of the biggest features Finatra provides is a default improved `case class` `deserializer <#improved-case-class-deserializer>`_
which *accumulates* deserialization errors while parsing JSON into a `case class` instead of failing-fast,
such that all errors can be reported at once.

.. admonition :: 🚨 This documentation assumes some level of familiarity with `Jackson JSON Processing library <https://github.com/FasterXML/jackson>`__

    Specifically Jackson `databinding <https://github.com/FasterXML/jackson-databind#1-minute-tutorial-pojos-to-json-and-back>`__
    and the Jackson `ObjectMapper <http://fasterxml.github.io/jackson-databind/javadoc/2.10/com/fasterxml/jackson/databind/ObjectMapper.html>`__.

    There are several `tutorials <https://github.com/FasterXML/jackson-docs#tutorials>`__ (and `external documentation <https://github.com/FasterXML/jackson-docs#external-off-github-documentation>`__)
    which may be useful if you are unfamiliar with Jackson.

    Additionally, you may want to familiarize yourself with the `Jackson Annotations <https://github.com/FasterXML/jackson-docs#annotations>`_
    as they allow for finer-grain customization of Jackson databinding.

Case Classes
------------

As mentioned, `Jackson <https://github.com/FasterXML/jackson>`__ is a JSON processing library. We
generally use Jackson for `databinding <https://www.tutorialspoint.com/jackson/jackson_data_binding.htm>`__,
or more specifically:

- object serialization: converting an object **into a JSON String** and
- object deserialization: converting a JSON String **into an object**

The Finatra Jackson integration is primarily centered around serializing and deserializing Scala
`case classes <https://docs.scala-lang.org/tour/case-classes.html>`__. This is because Scala
`case classes <https://docs.scala-lang.org/tour/case-classes.html>`__ map well to the two JSON
structures [`reference <https://www.json.org/json-en.html>`__]:

- A collection of name/value pairs. Generally termed an *object*. Here, the name/value pairs are the case field name to field value but can also be an actual Scala `Map[T, U]` as well.
- An ordered list of values. Typically an *array*, *vector*, *list*, or *sequence*, which for case classes can be represented by a Scala `Iterable`.

Library Features
----------------

-  Usable outside of the Finatra framework as a limited replacement for the |jackson-module-scala|_ or `Jerkson <https://github.com/codahale/jerkson>`__.
-  A `c.t.finatra.jackson.ScalaObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/ScalaObjectMapper.scala>`__ which provides additional Scala friendly methods not found in the |jackson-module-scala|_ `ScalaObjectMapper <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/scala/com/fasterxml/jackson/module/scala/ScalaObjectMapper.scala>`_.
-  Custom `case class` `deserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/CaseClassDeserializer.scala>`__ which overcomes some of the limitations in |jackson-module-scala|_.
-  Integration with Finatra's `JSR-303 style <https://beanvalidation.org/1.0/spec/>`_ validations during JSON deserialization.
-  A Finatra `c.t.inject.TwitterModule <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala>`__ for injecting the Finatra `ScalaObjectMapper` with support for customization.
-  Integration with Finatra `HTTP routing <routing.html>`__ to support binding and validation of query, route, form params, and headers.
-  Utilities for comparing JSON strings. These utilities include `c.t.finatra.json.utils.JsonDiffUtil <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/utils/JsonDiffUtil.scala>`__, a production line utility with the primary responsibility of calculating Json diffs. In the case where a difference is found, a Some(JsonDiffResult) is returned, otherwise a None. `c.t.finatra.json.JsonDiff <https://github.com/twitter/finatra/blob/develop/jackson/src/test/scala/com/twitter/finatra/json/JsonDiff.scala>`__ is a test utility built on upon JsonDiffUtil.

c.t.finatra.jackson.ScalaObjectMapper
-------------------------------------

The Finatra |FinatraScalaObjectMapper|_ is a thin wrapper around a configured |jackson-module-scala|_
`ScalaObjectMapper <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/scala/com/fasterxml/jackson/module/scala/ScalaObjectMapper.scala>`_.
However, the Finatra |FinatraScalaObjectMapper|_ comes configured with several defaults when instantiated.

Defaults
~~~~~~~~

The following integrations are provided by default when using the |FinatraScalaObjectMapper|_:

-  The Jackson `DefaultScalaModule <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/scala/com/fasterxml/jackson/module/scala/DefaultScalaModule.scala>`__.
-  The Jackson `JodaModule <https://github.com/FasterXML/jackson-datatype-joda/blob/master/src/main/java/com/fasterxml/jackson/datatype/joda/JodaModule.java>`__.
-  A `LongKeyDeserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/internal/serde/LongKeyDeserializer.scala>`__: allows for deserializing maps with long keys.
-  A `WrappedValueSerializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/internal/caseclass/wrapped/WrappedValueSerializer.scala>`__: more information on "WrappedValues" `here <https://docs.scala-lang.org/overviews/core/value-classes.html>`__.
-  A `JodaDurationMillisSerializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/internal/serde/JodaDurationMillisSerializer.scala>`__.
-  An improved `JodaDateTimeDeserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/internal/serde/JodaDatetimeDeserializer.scala>`__.
-  Twitter `c.t.util.Time <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Time.scala>`_ and `c.t.util.Duration <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Duration.scala>`_ serializers [`1 <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/serde/TimeStringSerializer.scala>`_, `2 <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/serde/DurationStringSerializer.scala>`_] and deserializers [`1 <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/serde/TimeStringDeserializer.scala>`_, `2 <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/serde/DurationStringDeserializer.scala>`_].
-  An improved `CaseClassDeserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/internal/caseclass/jackson/CaseClassDeserializer.scala>`__: see details `below <#improved-case-class-deserializer>`__.

Instantiation
~~~~~~~~~~~~~

There are `apply` functions available for creation of a |FinatraScalaObjectMapper|_ configured
with the defaults listed above:

.. code:: scala

    val injector: com.google.inject.Injector = ???
    val underlying: com.fasterxml.jackson.databind.ObjectMapper
        with com.fasterxml.jackson.module.scala.ScalaObjectMapper = ???

    val objectMapper = ScalaObjectMapper()
    val objectMapper = ScalaObjectMapper(injector)
    val objectMapper = ScalaObjectMapper(underlying)
    val objectMapper = ScalaObjectMapper(injector, underlying)

.. important::

    All of the above `apply` methods which take an underlying Jackson `ObjectMapper` will always
    **mutate the configuration** of the underlying Jackson `ObjectMapper` to apply the current
    configuration of the `ScalaObjectMapper#Builder` to the provided Jackson `ObjectMapper`. That is,
    they should be considered builder functions to help produce a configured |FinatraScalaObjectMapper|_.

    However, there may be times where you would like to *only wrap* an already configured Jackson `ObjectMapper`.

    To do, use `ScalaObjectMapper.objectMapper(underlying)` to create a |FinatraScalaObjectMapper|_
    which **wraps but does not mutate** the configuration of the given underlying Jackson `ObjectMapper`.

Basic Usage
~~~~~~~~~~~

Let's assume we have these two case classes:

.. code:: scala

    case class Bar(d: String)
    case class Foo(a: String, b: Int, c: Bar)

To **serialize** a case class into a JSON string, use

.. code:: scala

    ScalaObjectMapper#writeValueAsString(any: Any): String

For example:

.. code:: scala

    Welcome to Scala 2.12.12 (JDK 64-Bit Server VM, Java 1.8.0_242).
    Type in expressions for evaluation. Or try :help.

    scala> val mapper = ScalaObjectMapper()
    mapper: com.twitter.finatra.jackson.ScalaObjectMapper = com.twitter.finatra.jackson.ScalaObjectMapper@490d9c41

    scala> val foo = Foo("Hello, World", 42, Bar("Goodbye, World"))
    foo: Foo = Foo(Hello, World,42,Bar(Goodbye, World))

    scala> mapper.writeValueAsString(foo)
    res0: String = {"a":"Hello, World","b":42,"c":{"d":"Goodbye, World"}}

    scala> // or use the configured "pretty print mapper"

    scala> mapper.writePrettyString(foo)
    res1: String =
    {
      "a" : "Hello, World",
      "b" : 42,
      "c" : {
        "d" : "Goodbye, World"
      }
    }

    scala>

To **deserialize** a JSON string into a case class, use

.. code:: scala

    ScalaObjectMapper#parse[T](s: String): T

For example, assuming the same `Bar` and `Foo` case classes defined above:

.. code:: scala

    Welcome to Scala 2.12.12 (JDK 64-Bit Server VM, Java 1.8.0_242).
    Type in expressions for evaluation. Or try :help.

    scala> val mapper = ScalaObjectMapper()
    mapper: com.twitter.finatra.jackson.ScalaObjectMapper = com.twitter.finatra.jackson.ScalaObjectMapper@3b64f131

    scala> val s = """{"a": "Hello, World", "b": 42, "c": {"d": "Goodbye, World"}}"""
    s: String = {"a": "Hello, World", "b": 42, "c": {"d": "Goodbye, World"}}

    scala> val foo = mapper.parse[Foo](s)
    foo: Foo = Foo(Hello, World,42,Bar(Goodbye, World))

    scala>

You can find many examples of using the `ScalaObjectMapper` in the various framework tests:

- Scala examples [`1 <https://github.com/twitter/finatra/blob/develop/jackson/src/test/scala/com/twitter/finatra/jackson/AbstractScalaObjectMapperTest.scala>`__, `2 <https://github.com/twitter/finatra/blob/develop/jackson/src/test/scala/com/twitter/finatra/jackson/ScalaObjectMapperTest.scala>`__].
- Java `example <https://github.com/twitter/finatra/blob/develop/jackson/src/test/java/com/twitter/finatra/jackson/tests/ScalaObjectMapperJavaTest.java>`__.

As mentioned above, there is also a plethora of Jackson `tutorials <https://github.com/FasterXML/jackson-docs#tutorials>`__ and `HOW-TOs <https://github.com/FasterXML/jackson-docs#external-off-github-documentation>`__
available online which provide more in-depth examples of how to use a Jackson `ObjectMapper <http://fasterxml.github.io/jackson-databind/javadoc/2.10/com/fasterxml/jackson/databind/ObjectMapper.html>`__.

Advanced Configuration
~~~~~~~~~~~~~~~~~~~~~~

To apply more custom configuration to create a |FinatraScalaObjectMapper|_, there is a builder for
constructing a customized mapper.

E.g., to set a `PropertyNamingStrategy` different than the default:

.. code:: scala

    val objectMapper: ScalaObjectMapper =
      ScalaObjectMapper.builder
        .withPropertyNamingStrategy(PropertyNamingStrategy.KebabCaseStrategy)
        .objectMapper

Or to set additional modules or configuration:

.. code:: scala

    val objectMapper: ScalaObjectMapper =
      ScalaObjectMapper.builder
        .withPropertyNamingStrategy(PropertyNamingStrategy.KebabCaseStrategy)
        .withAdditionalJacksonModules(Seq(MySimpleJacksonModule))
        .withAdditionalMapperConfigurationFn(
          _.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        )
        .objectMapper

You can also get a `camelCase` or `snake_case` specifically configured mapper.

.. code:: scala

    val camelCaseObjectMapper: ScalaObjectMapper =
      ScalaObjectMapper.builder
        .withAdditionalJacksonModules(Seq(MySimpleJacksonModule))
        .withAdditionalMapperConfigurationFn(
          _.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        )
        .camelCaseObjectMapper

    val snakeCaseObjectMapper: ScalaObjectMapper =
      ScalaObjectMapper.builder
        .withAdditionalJacksonModules(Seq(MySimpleJacksonModule))
        .withAdditionalMapperConfigurationFn(
          _.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        )
        .snakeCaseObjectMapper

Or, if you already have an instance of an object mapper and want a copy that is configured to
either a `camelCase` or `snake_case` property naming strategy, you can pass it to the appropriate
`ScalaObjectMapper` utility method:

.. code:: scala

    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.experimental.{ScalaObjectMapper => JacksonScalaObjectMapper}
    import com.twitter.finatra.jackson.ScalaObjectMapper

    // our default Jackson object mapper
    val jacksonObjectMapper: ObjectMapper with JacksonScalaObjectMapper = ???

    // a 'camelCase' copy
    val camelCaseObjectMapper: ScalaObjectMapper =
      ScalaObjectMapper.camelCaseObjectMapper(jacksonObjectMapper)

    // a 'snake_case' copy
    val snakeCaseObjectMapper: ScalaObjectMapper =
      ScalaObjectMapper.snakeCaseObjectMapper(jacksonObjectMapper)

Note that these methods will *copy* the underlying Jackson mapper (not mutate it) to produce a new
|FinatraScalaObjectMapper|_ configured with the desired property naming strategy. That is, a new
underlying mapper will be created which copies the original configuration and only the property
naming strategy changed.

As mentioned above, you also wrap an already configured object mapper with the |FinatraScalaObjectMapper|_:

.. code:: scala

    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.experimental.{ScalaObjectMapper => JacksonScalaObjectMapper}
    import com.twitter.finatra.jackson.ScalaObjectMapper

    // our default Jackson object mapper
    val jacksonObjectMapper: ObjectMapper with JacksonScalaObjectMapper = ???

    // a Finatra 'ScalaObjectMapper' copy
    val objectMapper: ScalaObjectMapper = ScalaObjectMapper.objectMapper(jacksonObjectMapper)

This will *copy* the underlying Jackson mapper (not mutate it) to produce a new
|FinatraScalaObjectMapper|_ configured the same as the given Jackson object mapper.

Access to the underlying Jackson Object Mapper
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As previously stated, the |FinatraScalaObjectMapper|_ is a thin wrapper around a configured Jackson |jackson-module-scala|_
`ScalaObjectMapper <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/scala/com/fasterxml/jackson/module/scala/ScalaObjectMapper.scala>`_.

You can always access the underlying Jackson object mapper by calling `underlying`:

.. code:: scala

    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.experimental.{ScalaObjectMapper => JacksonScalaObjectMapper}
    import com.twitter.finatra.jackson.ScalaObjectMapper

    val objectMapper: ScalaObjectMapper = ???

    val jacksonObjectMapper: ObjectMapper with JacksonScalaObjectMapper = objectMapper.underlying

c.t.finatra.jackson.modules.ScalaObjectMapperModule
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The framework also provides a `c.t.inject.TwitterModule <../getting-started/modules.html>`_ which
can be used to bind a configured |FinatraScalaObjectMapper|_ to the object graph. This is similar
to the `jackson-module-guice <https://github.com/FasterXML/jackson-modules-base/tree/master/guice>`_
`ObjectMapperModule <https://github.com/FasterXML/jackson-modules-base/blob/master/guice/src/main/java/com/fasterxml/jackson/module/guice/ObjectMapperModule.java>`_
but uses Finatra's `TwitterModule <../getting-started/modules.html>`_.

The |ScalaObjectMapperModule|_ provides bound instances of:

- a configured |FinatraScalaObjectMapper|_ as a `Singleton`.
- a `camelCase` configured |FinatraScalaObjectMapper|_ as a `Singleton`.
- a `snake\_case` configured |FinatraScalaObjectMapper|_ as a `Singleton`.

.. tip::

    Generally, you are encouraged to obtain a reference to the `Singleton` instance provided by the
    object graph over instantiating a new mapper. This is to ensure usage of a consistently configured
    mapper across your application.

The |ScalaObjectMapperModule|_ provides overridable methods which mirror the
`ScalaObjectMapper#Builder` for configuring the bound mappers.

For example, to create a `c.t.inject.TwitterModule <../getting-started/modules.html>`_  which sets
the `PropertyNamingStrategy` different than the default:

.. code:: scala

    import com.fasterxml.jackson.databind.PropertyNamingStrategy
    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    object MyCustomObjectMapperModule extends ScalaObjectMapperModule {

        override val propertyNamingStrategy: PropertyNamingStrategy =
          new PropertyNamingStrategy.KebabCaseStrategy
    }

Or to set additional modules or configuration:

.. code:: scala

    import com.fasterxml.jackson.databind.{
      DeserializationFeature,
      Module,
      ObjectMapper,
      PropertyNamingStrategy
    }
    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    object MyCustomObjectMapperModule extends ScalaObjectMapperModule {

        override val propertyNamingStrategy: PropertyNamingStrategy =
          new PropertyNamingStrategy.KebabCaseStrategy

        override val additionalJacksonModules: Seq[Module] =
          Seq(MySimpleJacksonModule)

        override def additionalMapperConfiguration(mapper: ObjectMapper): Unit = {
          mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        }
    }

See the `Modules Configuration in Servers <../getting-started/modules.html#module-configuration-in-servers>`_
or the HTTP Server `Framework Modules <../http/server.html#framework-modules>`_ for more information
on how to make use of any custom |ScalaObjectMapperModule|_.

Adding a Custom Serializer or Deserializer
------------------------------------------

To register a custom serializer or deserializer, you have a couple of options depending on if you
are using injection to bind an instance of a |FinatraScalaObjectMapper|_ to the object graph. When using
injection, you should prefer to configure any custom serializer or deserializer via the methods
provided by the |ScalaObjectMapperModule|_, otherwise you can directly configure the `underlying`
Jackson mapper of a |FinatraScalaObjectMapper|_ instance.

Via a Custom |ScalaObjectMapperModule|_ (recommended)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Create a new Jackson `com.fasterxml.jackson.databind.Module <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/Module.java>`_ implementation.

  .. tip::

    To implement a new Jackson `Module <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/Module.java>`_ for adding a basic custom serializer or deserializer, you can
    use the `com.fasterxml.jackson.databind.module.SimpleModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/module/SimpleModule.java>`_.

    Note, that if you want to register a `JsonSerializer` or `JsonDeserializer` over a parameterized
    type, such as a `Collection[T]` or `Map[T, U]`, that you should instead implement
    `com.fasterxml.jackson.databind.deser.Deserializers <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/deser/Deserializers.java>`_
    or `com.fasterxml.jackson.databind.ser.Serializers <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/ser/Serializers.java>`_
    which provide callbacks to match the full signatures of the class to deserialize into via a
    Jackson `JavaType`.

    Also note that with this usage it is generally recommended to add your `Serializers` or
    `Deserializers` implementation via a |jackson-module-scala|_ `JacksonModule <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/scala/com/fasterxml/jackson/module/scala/JacksonModule.scala>`_.
    (which is an extension of `com.fasterxml.jackson.databind.Module <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/Module.java>`_
    and can thus be used in place). See example below.

- Add your serializer or deserializer using the `SimpleModule#addSerializer` or `SimpleModule#addDeserializer` methods in your module.
- In your custom |ScalaObjectMapperModule|_ extension, add the Jackson `Module <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/Module.java>`_ implementation to list of additional Jackson modules by overriding and implementing the `ScalaObjectMapperModule#additionalJacksonModules`.

For example:

.. code:: scala

    import com.fasterxml.jackson.databind.JsonDeserializer
    import com.fasterxml.jackson.databind.deser.Deserializers
    import com.fasterxml.jackson.databind.module.SimpleModule
    import com.fasterxml.jackson.module.scala.JacksonModule
    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    // custom deserializer
    class FooDeserializer extends JsonDeserializer[Foo] {
      override def deserialize(...)
    }

    // custom parameterized deserializer
    class MapIntIntDeserializer extends JsonDeserializer[Map[Int, Int]] {
      override def deserialize(...)
    }

    // custom parameterized deserializer resolver
    class MapIntIntDeserializerResolver extends Deserializers.Base {
      override def findBeanDeserializer(
        javaType: JavaType,
        config: DeserializationConfig,
        beanDesc: BeanDescription
      ): MapIntIntDeserializer = {
        if (javaType.isMapLikeType && javaType.hasGenericTypes && hasIntTypes(javaType)) {
          new MapIntIntDeserializer
        } else null
      }

      private[this] def hasIntTypes(javaType: JavaType): Boolean = {
        val k = javaType.containedType(0)
        val v = javaType.containedType(1)
        k.isPrimitive && k.getRawClass == classOf[Integer] &&
          v.isPrimitive && v.getRawClass == classOf[Integer]
      }
    }

    // Jackson SimpleModule for custom deserializer
    class FooDeserializerModule extends SimpleModule {
      addDeserializer(FooDeserializer)
    }

    // Jackson Module Scala JacksonModule for custom deserializer
    class MapIntIntDeserializerModule extends JacksonModule {
      override def getModuleName: String = this.getClass.getName

      this += {
        _.addDeserializers(new MapIntIntDeserializerResolver)
      }
    }

    object MyCustomObjectMapperModule extends ScalaObjectMapperModule {
      override val additionalJacksonModules = Seq(
        // added via a new anonymous SimpleModule
        new SimpleModule {
          addSerializer(LocalDateParser)
        },
        // added via a re-usable SimpleModule
        new FooDeserializerModule,
        // added via a re-usable JacksonModule
        new MapIntIntDeserializerModule)
    }

For more information see the Jackson documentation for
`Custom Serializers <https://github.com/FasterXML/jackson-docs/wiki/JacksonHowToCustomSerializers>`__.

.. note::

    It is also important to note that `Jackson <https://github.com/FasterXML/jackson-databind>`_
    Modules are **not** Google `Guice <https://github.com/google/guice>`_ Modules but are instead
    interfaces for extensions that can be registered with a Jackson `ObjectMapper` in order to
    provide a well-defined set of extensions to default functionality. In this way, they are similar
    to Google `Guice <https://github.com/google/guice>`__ Modules, but for configuring an
    `ObjectMapper` instead of an `Injector`.

Via Adding a Module to a |FinatraScalaObjectMapper|_ instance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Follow the steps to create a Jackson Module for the custom serializer or deserializer then register
the module to the underlying Jackson mapper from the |FinatraScalaObjectMapper|_ instance:

.. code:: scala

    import com.fasterxml.jackson.databind.JsonDeserializer
    import com.fasterxml.jackson.databind.module.SimpleModule
    import com.twitter.finatra.jackson.ScalaObjectMapper

    // custom deserializer
    class FooDeserializer extends JsonDeserializer[Foo] {
      override def deserialize(...)
    }

    // Jackson SimpleModule for custom deserializer
    class FooDeserializerModule extends SimpleModule {
      addDeserializer(FooDeserializer)
    }

    ...

    val scalaObjectMapper: ScalaObjectMapper = ???
    scalaObjectMapper.registerModule(new FooDeserializerModule)

.. warning::

    Please note that this will mutate the underlying Jackson `ObjectMapper` and thus care should be
    taken with this approach. It is highly recommended to prefer setting configuration via a
    custom |ScalaObjectMapperModule|_ implementation.

Improved `case class` deserializer
----------------------------------

Finatra provides a custom `case class deserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/CaseClassDeserializer.scala>`__
which overcomes some limitations in |jackson-module-scala|_:

-  Throws a `JsonMappingException` when required fields are missing from the parsed JSON.
-  Uses specified default values when fields are missing in the incoming JSON.
-  Properly deserializes a `Seq[Long]` (see: https://github.com/FasterXML/jackson-module-scala/issues/62).
-  Supports `"wrapped values" <https://docs.scala-lang.org/overviews/core/value-classes.html>`__ using `c.t.inject.domain.WrappedValue <https://github.com/twitter/finatra/blob/develop/inject/inject-utils/src/main/scala/com/twitter/inject/domain/WrappedValue.scala>`_.
-  Support for field and method level validations via integration with Finatra's `JSR-303 style <https://beanvalidation.org/1.0/spec/>`_ validations.
-  Accumulates all JSON deserialization errors (instead of failing fast) in a returned sub-class of `JsonMappingException` (see: `CaseClassMappingException <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/exceptions/CaseClassMappingException.scala>`_).

The `case class` deserializer is added by default when constructing a new |FinatraScalaObjectMapper|_.

.. tip::

  Note: with the |FinatraCaseClassDeserializer|_, non-option fields without default values are
  **considered required**.

  If a required field is missing, a `CaseClassMappingException` is thrown.

`@JsonCreator` Support
----------------------

The |FinatraCaseClassDeserializer|_ supports specification of a constructor or static factory
method annotated with the Jackson Annotation, `@JsonCreator <https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations#deserialization-details>`_
(an annotation for indicating a specific constructor or static factory method to use for
instantiation of the case class during deserialization).

For example, you can annotate a method on the companion object for the case class as a static
factory for instantiation. Any static factory method to use for instantiation **MUST** be specified
on the companion object for case class:

.. code:: scala

    case class MySimpleCaseClass(int: Int)

    object MySimpleCaseClass {
      @JsonCreator
      def apply(s: String): MySimpleCaseClass = MySimpleCaseClass(s.toInt)
    }

Or to specify a secondary constructor to use for case class instantiation:

.. code:: scala

    case class MyCaseClassWithMultipleConstructors(number1: Long, number2: Long, number3: Long) {
      @JsonCreator
      def this(numberAsString1: String, numberAsString2: String, numberAsString3: String) {
        this(numberAsString1.toLong, numberAsString2.toLong, numberAsString3.toLong)
      }
    }

.. note::

    If you define multiple constructors on a case class, it is **required** to annotate one of the
    constructors with `@JsonCreator`.

    To annotate the primary constructor (as the syntax can seem non-intuitive because the `()` is
    required):

    .. code:: scala

        case class MyCaseClassWithMultipleConstructors @JsonCreator()(number1: Long, number2: Long, number3: Long) {
          def this(numberAsString1: String, numberAsString2: String, numberAsString3: String) {
            this(numberAsString1.toLong, numberAsString2.toLong, numberAsString3.toLong)
          }
        }

    The parens are needed because the Scala class constructor syntax requires constructor
    annotations to have exactly one parameter list, possibly empty.

    If you define multiple case class constructors with no visible `@JsonCreator` constructor or
    static factory method via a companion, deserialization will error.

`@JsonFormat` Support
---------------------

The |FinatraCaseClassDeserializer|_ supports `@JsonFormat`-annotated case class fields to properly
contextualize deserialization based on the values in the annotation.

A common use case is to be able to support deserializing a JSON string into a "time" representation
class based on a specific pattern independent of the time format configured on the `ObjectMapper` or
even the default format for a given deserializer for the type.

For instance, Finatra provides a `deserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/serde/TimeStringDeserializer.scala>`_
for the `com.twitter.util.Time <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Time.scala>`_
class. This deserializer is a Jackson `ContextualDeserializer <https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/deser/ContextualDeserializer.html>`_
and will properly take into account a `@JsonFormat`-annotated field. However, the
|FinatraCaseClassDeserializer|_ is invoked first and acts as a proxy for deserializing the time
value. The case class deserializer properly contextualizes the field for correct deserialization by
the `TimeStringDeserializer`.

Thus if you had a case class defined:

.. code:: scala

    import com.fasterxml.jackson.annotation.JsonFormat
    import com.twitter.util.Time

    case class Event(
      id: Long,
      description: String,
      @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX") when: Time
    )

The following JSON:

.. code:: json

    {
      "id": 42,
      "description": "Something happened.",
      "when": "2018-09-14T23:20:08.000-07:00"
    }

Will always deserialize properly into the case class regardless of the pattern configured on the
`ObjectMapper` or as the default of a contextualized deserializer:

.. code:: scala

    val scalaObjectMapper: ScalaObjectMapper = ???
    val event: Event = scalaObjectMapper.parse[Event](json)

Jackson InjectableValues Support
--------------------------------

By default, the framework provides a |FinatraScalaObjectMapper|_ configured to resolve Jackson
`InjectableValues <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/InjectableValues.java>`_
via a given Google `Guice <https://github.com/google/guice>`_ `Injector <https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Injector.html>`_.

The default is very similar to the `jackson-module-guice <https://github.com/FasterXML/jackson-modules-base/tree/master/guice>`_:
`GuiceInjectableValues <https://github.com/FasterXML/jackson-modules-base/blob/master/guice/src/main/java/com/fasterxml/jackson/module/guice/GuiceInjectableValues.java>`_.

.. note::

    Jackson “InjectableValues” is not related to `Dependency Injection <../getting-started/basics.html#dependency-injection>`_
    or Google `Guice <https://github.com/google/guice>`_. It is meant to convey the filling in of a
    value in a deserialized object from somewhere other than the incoming JSON. In Jackson parlance,
    this is “injection” of a value.

The Finatra `c.t.finatra.jackson.caseclass.DefaultInjectableValues <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/DefaultInjectableValues.scala>`_
allows users to denote fields in the case class to fill with values that come from a configured
Google `Guice <https://github.com/google/guice>`_ `Injector <https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Injector.html>`_
such that you can do this:

.. code:: scala

    import javax.inject.Inject

    case class Foo(name: String, description: String, @Inject bar: Bar)

That is, annotate the field to inject with either:

- `javax.inject.Inject <https://docs.oracle.com/javaee/7/api/index.html?javax/inject/Inject.html>`_,
- `com.google.inject.Inject <https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Inject.html>`_, or
- `com.fasterxml.jackson.annotation.JacksonInject <https://fasterxml.github.io/jackson-annotations/javadoc/2.9/index.html?com/fasterxml/jackson/annotation/JacksonInject.html>`_

and the framework will attempt to get an instance of the field type from the Injector with which
the mapper was configured. In this case, the framework would attempt to obtain an instance of `Bar`
from the object graph.

.. note::

    The framework also provides an `@InjectableValue` annotation which is used to mark other
    `java.lang.annotation.Annotation` interfaces as annotations that support case class field
    injection via Jackson `InjectableValues`.

    Finatra's HTTP integration defines such annotations to support injecting case class fields
    obtained from parts of an HTTP message.

    See the `HTTP Requests - Field Annotations <../http/requests.html#field-annotations>`_
    documentation for more details.

Using the case class above, you could then parse incoming JSON with the |FinatraScalaObjectMapper|_:

.. code:: scala

    import com.twitter.finatra.jackson.ScalaObjectMapper
    import com.twitter.inject.Injector
    import com.twitter.inject.app.TestInjector
    import javax.inject.Inject

    case class Foo(name: String, description: String, @Inject bar: Bar)

    val json: String =
      """
        |{
        |  “name”: “FooItem”,
        |  “description”: “This is the description for FooItem”
        |}
      """.stripMargin

    val injector: Injector = TestInjector(???).create
    val mapper = ScalaObjectMapper.objectMapper(injector.underlying)
    val foo = mapper.parse[Foo](json)

When deserializing the JSON string into an instance of Foo, the mapper will attempt to locate an
instance of type `Bar` from the given injector and use it in place of the `bar` field in the `Foo`
case class.

.. caution::

    It is an error to specify multiple field injection annotations on a field, and it is also an
    error to use a field injection annotation in conjunction with **any** `JacksonAnnotation <https://github.com/FasterXML/jackson-annotations/blob/a991c43a74e4230eb643e380870b503997674c2d/src/main/java/com/fasterxml/jackson/annotation/JacksonAnnotation.java#L9>`_.

    Both of these cases will result in error during deserialization of JSON into the case class when
    using the |FinatraCaseClassDeserializer|_.

As mentioned, the Finatra HTTP integration provides further Jackson `InjectableValues` support specifically for
injecting values into a case class which are obtained from different parts of an HTTP message.

See the `HTTP Requests - Field Annotations <../http/requests.html#field-annotations>`_ documentation
for more details on HTTP Message "injectable values".

`Mix-in Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
----------------------------------------------------------------------------------------------

The Jackson `Mix-in Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
provides a way to associate annotations to classes without needing to modify the target classes
themselves. It is intended to help support 3rd party datatypes where the user cannot modify the
sources to add annotations.

The |FinatraCaseClassDeserializer|_ supports Jackson `Mix-in Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
for specifying field annotations during deserialization with the `case class deserializer <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/CaseClassDeserializer.scala>`_.

For example, to deserialize JSON into the following classes that are not yours to annotate:

.. code:: scala

    case class Point(x: Int, y: Int) {
      def area: Int = x * y
    }

    case class Points(points: Seq[Point])

However, you want to enforce field constraints with Finatra `validations <./validations.html>`_
during deserialization. You can define a `Mix-in`,

.. code:: scala

    trait PointMixIn {
      @Min(0) @Max(100) def x: Int
      @Min(0) @Max(100) def y: Int
      @JsonIgnore def area: Int
    }

Then register this `Mix-in` for the `Point` class type. There are several ways to do this. Generally,
it is recommended to always prefer applying configuration in a custom `ScalaObjectMapperModule` to
ensure usage of a consistently configured mapper across your application.

Implement via a Custom |ScalaObjectMapperModule|_ (recommended)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- First, create a new Jackson `com.fasterxml.jackson.databind.Module <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/Module.java>`_ implementation. You can use the `com.fasterxml.jackson.databind.module.SimpleModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/module/SimpleModule.java>`_.
- Add your `Mix-in` using the `SimpleModule#setMixInAnnotation` method in your module.
- In your custom |ScalaObjectMapperModule|_ extension, add the Jackson `Module <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/Module.java>`_.

For example, create a new Jackson `Module <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/Module.java>`_:

.. code:: scala

    import com.fasterxml.jackson.databind.module.SimpleModule

    object PointMixInModule extends SimpleModule {
        setMixInAnnotation(classOf[Point], classOf[PointMixIn]);
    }

Then add the module to the list of additional Jackson modules in your custom |ScalaObjectMapperModule|_:

.. code:: scala

    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    object MyCustomObjectMapperModule extends ScalaObjectMapperModule {
      override val additionalJacksonModules = Seq(PointMixInModule)
    }

Implement via Adding a Module to a |FinatraScalaObjectMapper|_ instance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Follow the steps to create a Jackson Module for the `Mix-in` then register the module to the
underlying Jackson mapper from the |FinatraScalaObjectMapper|_ instance:

.. code:: scala

    import com.fasterxml.jackson.databind.module.SimpleModule
    import com.twitter.finatra.jackson.ScalaObjectMapper

    object PointMixInModule extends SimpleModule {
        setMixInAnnotation(classOf[Point], classOf[PointMixIn]);
    }

    ...

    val scalaObjectMapper: ScalaObjectMapper = ???
    scalaObjectMapper.registerModule(PointMixInModule)

Or register the `Mix-in` for the class type directly on the mapper (without a Jackson Module):

.. code:: scala

    val objectMapper: ScalaObjectMapper = ???
    objectMapper.underlying.addMixin[Point, PointMixIn]

.. warning::

    Please note that this will mutate the underlying Jackson `ObjectMapper` and thus care should be
    taken with this approach. It is highly recommended to prefer setting configuration via a
    custom |ScalaObjectMapperModule|_ implementation to ensure consistency of the mapper
    configuration across your application.

Deserializing this JSON would then error with failed validations:

.. code:: json

    {
      "points": [
        {"x": -1, "y": 120},
        {"x": 4, "y": 99}
      ]
    }

As the first `Point` instance has an x-value less than the minimum of 0 and a y-value greater than
the maximum of 100.

Known `CaseClassDeserializer` Limitations
-----------------------------------------

The |FinatraCaseClassDeserializer|_ provides a fair amount of utility but can not and does not
support all Jackson Annotations. The behavior of supporting a Jackson Annotation can at times be
ambiguous (or even nonsensical), especially when it comes to combining Jackson Annotations and
injectable field annotations.

Java Enums
----------

We recommend the use of `Java Enums <https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html>`__
for representing enumerations since they integrate well with Jackson's ObjectMapper and have
exhaustiveness checking as of Scala 2.10.

The following `Jackson annotations <https://github.com/FasterXML/jackson-annotations>`__ may be
useful when working with Enums:

- `@JsonValue`: can be used for an overridden `toString` method.
- `@JsonEnumDefaultValue`: can be used for defining a default value when deserializing unknown Enum values. Note that this requires `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE <https://github.com/FasterXML/jackson-databind/wiki/Deserialization-Features#value-conversions-coercion>`_ feature to be enabled.

.. |FinatraScalaObjectMapper| replace:: `ScalaObjectMapper`
.. _FinatraScalaObjectMapper: https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/ScalaObjectMapper.scala

.. |FinatraCaseClassDeserializer| replace:: `Finatra case class deserializer`
.. _FinatraCaseClassDeserializer: https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/CaseClassDeserializer.scala

.. |ScalaObjectMapperModule| replace:: `ScalaObjectMapperModule`
.. _ScalaObjectMapperModule: https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala

.. |jackson-module-scala| replace:: `jackson-module-scala`
.. _jackson-module-scala: https://github.com/FasterXML/jackson-module-scala

