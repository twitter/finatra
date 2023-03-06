.. _json:

Jackson Integration
===================

Finatra builds upon the `TwitterUtil <http://twitter.github.io/util>`__ Jackson `library <http://twitter.github.io/util/guide/util-jackson/index.html>`__ for `JSON <https://en.wikipedia.org/wiki/JSON>`_
which provides a `c.t.util.jackson.ScalaObjectMapper <https://github.com/twitter/util/blob/develop/util-jackson/src/main/scala/com/twitter/util/jackson/ScalaObjectMapper.scala>`__.
See the `User Guide <http://twitter.github.io/util/guide/util-jackson/index.html>`__ for details about the `TwitterUtil <http://twitter.github.io/util>`__ Jackson library.

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

The TwitterUtil Jackson integration is primarily centered around serializing and deserializing Scala
`case classes <https://docs.scala-lang.org/tour/case-classes.html>`__. This is because Scala
`case classes <https://docs.scala-lang.org/tour/case-classes.html>`__ map well to the two JSON
structures [`reference <https://www.json.org/json-en.html>`__]:

- A collection of name/value pairs. Generally termed an *object*. Here, the name/value pairs are the case field name to field value but can also be an actual Scala `Map[T, U]` as well.
- An ordered list of values. Typically an *array*, *vector*, *list*, or *sequence*, which for case classes can be represented by a Scala `Iterable`.

Basic Usage
~~~~~~~~~~~

Here we outline some simple usage patterns. More examples can be found in the TwitterUtil
`User Guide <http://twitter.github.io/util/guide/util-jackson/index.html>`__.

Let's assume we have these two case classes:

.. code:: scala

    case class Bar(d: String)
    case class Foo(a: String, b: Int, c: Bar)

To **serialize** a case class into a JSON string, use

.. code:: scala

    ScalaObjectMapper#writeValueAsString(any: Any): String

For example:

.. code-block:: scala
   :emphasize-lines: 16, 21

    Welcome to Scala 2.12.13 (JDK 64-Bit Server VM, Java 1.8.0_242).
    Type in expressions for evaluation. Or try :help.

    scala> import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    scala> import com.twitter.util.jackson.ScalaObjectMapper
    import com.twitter.util.jackson.ScalaObjectMapper

    scala> val mapper: ScalaObjectMapper = (new ScalaObjectMapperModule).objectMapper
    mapper: com.twitter.util.jackson.ScalaObjectMapper = com.twitter.util.jackson.ScalaObjectMapper@5690c2a8

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

.. code-block:: scala
   :emphasize-lines: 16

    Welcome to Scala 2.12.13 (JDK 64-Bit Server VM, Java 1.8.0_242).
    Type in expressions for evaluation. Or try :help.

    scala> import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    scala> import com.twitter.util.jackson.ScalaObjectMapper
    import com.twitter.util.jackson.ScalaObjectMapper

    scala> val mapper: ScalaObjectMapper = (new ScalaObjectMapperModule).objectMapper
    mapper: com.twitter.util.jackson.ScalaObjectMapper = com.twitter.util.jackson.ScalaObjectMapper@2c1523cd

    scala> val s = """{"a": "Hello, World", "b": 42, "c": {"d": "Goodbye, World"}}"""
    s: String = {"a": "Hello, World", "b": 42, "c": {"d": "Goodbye, World"}}

    scala> val foo = mapper.parse[Foo](s)
    foo: Foo = Foo(Hello, World,42,Bar(Goodbye, World))

    scala>

You can find many examples of using the `ScalaObjectMapperModule` in the various framework tests:

- Scala examples [`1 <https://github.com/twitter/finatra/blob/develop/jackson/src/test/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModuleTest.scala>`__, `2 <https://github.com/twitter/finatra/blob/develop/jackson/src/test/scala/com/twitter/finatra/jackson/ScalaObjectMapperTest.scala>`__].
- Java `example <https://github.com/twitter/finatra/blob/develop/jackson/src/test/java/com/twitter/finatra/jackson/tests/ScalaObjectMapperModuleJavaTest.java>`__.

As mentioned above, there is also a plethora of Jackson `tutorials <https://github.com/FasterXML/jackson-docs#tutorials>`__ and `HOW-TOs <https://github.com/FasterXML/jackson-docs#external-off-github-documentation>`__
available online which provide more in-depth examples of how to use a Jackson `ObjectMapper <http://fasterxml.github.io/jackson-databind/javadoc/2.10/com/fasterxml/jackson/databind/ObjectMapper.html>`__.

TwitterUtil `ScalaObjectMapper`
-------------------------------

The TwitterUtil |ScalaObjectMapper|_ is a thin wrapper around a configured |jackson-module-scala|_
`com.fasterxml.jackson.module.scala.ScalaObjectMapper <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/scala-2.%2B/tools/jackson/module/scala/ScalaObjectMapper.scala>`_.
In Finatra, the recommended way to construct a |ScalaObjectMapper|_ is via the Finatra |ScalaObjectMapperModule|_.

c.t.f.jackson.modules.ScalaObjectMapperModule
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Finatra provides the |ScalaObjectMapperModule|_, a `c.t.inject.TwitterModule <../getting-started/modules.html>`_,
which is the recommended way to configure and bind a |ScalaObjectMapper|_ to the object graph. This
is similar to the `jackson-module-guice <https://github.com/FasterXML/jackson-modules-base/tree/master/guice>`_
`ObjectMapperModule <https://github.com/FasterXML/jackson-modules-base/blob/master/guice/src/main/java/com/fasterxml/jackson/module/guice/ObjectMapperModule.java>`_
but uses Finatra's `TwitterModule <../getting-started/modules.html>`_.

The |ScalaObjectMapperModule|_ provides bound instances of:

- a `JsonFactory <https://fasterxml.github.io/jackson-core/javadoc/2.11/com/fasterxml/jackson/core/JsonFactory.html>`__ configured |ScalaObjectMapper|_ as a `Singleton`.
- a `JsonFactory <https://fasterxml.github.io/jackson-core/javadoc/2.11/com/fasterxml/jackson/core/JsonFactory.html>`__ |ScalaObjectMapper|_ with a `PropertyNamingStrategy` of `camelCase` as a `Singleton`.
- a `JsonFactory <https://fasterxml.github.io/jackson-core/javadoc/2.11/com/fasterxml/jackson/core/JsonFactory.html>`__ |ScalaObjectMapper|_ with a `PropertyNamingStrategy` of `snake\_case` as a `Singleton`.

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

c.t.f.jackson.modules.YamlScalaObjectMapperModule
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Finatra also provides the |YamlScalaObjectMapperModule|_ which is an extension of the
|ScalaObjectMapperModule|_ that can be used to configure and bind a |ScalaObjectMapper|_ using a
Jackson `YAMLFactory <https://fasterxml.github.io/jackson-dataformats-text/javadoc/yaml/2.11/com/fasterxml/jackson/dataformat/yaml/YAMLFactory.html>`__
instead of a `JsonFactory <https://fasterxml.github.io/jackson-core/javadoc/2.11/com/fasterxml/jackson/core/JsonFactory.html>`__.

The |YamlScalaObjectMapperModule|_ provides bound instances of:

- a `YAMLFactory <https://fasterxml.github.io/jackson-dataformats-text/javadoc/yaml/2.11/com/fasterxml/jackson/dataformat/yaml/YAMLFactory.html>`__ configured |ScalaObjectMapper|_ as a `Singleton`.
- a `YAMLFactory <https://fasterxml.github.io/jackson-dataformats-text/javadoc/yaml/2.11/com/fasterxml/jackson/dataformat/yaml/YAMLFactory.html>`__ configured |ScalaObjectMapper|_ with a `PropertyNamingStrategy` of `camelCase` as a `Singleton`.
- a `YAMLFactory <https://fasterxml.github.io/jackson-dataformats-text/javadoc/yaml/2.11/com/fasterxml/jackson/dataformat/yaml/YAMLFactory.html>`__ configured |ScalaObjectMapper|_ with a `PropertyNamingStrategy` of `snake\_Case` as a `Singleton`.

Since the |YamlScalaObjectMapperModule|_ is an extension of the |ScalaObjectMapperModule|_ is can be
used in place of the |ScalaObjectMapperModule|_ where a `YAML <https://yaml.org/>`__ object mapper
is desired.

Adding a Custom Serializer or Deserializer
------------------------------------------

To register a custom serializer or deserializer, configure any custom serializer or deserializer
via the methods provided by the |ScalaObjectMapperModule|_.

- Create a new Jackson `com.fasterxml.jackson.databind.JacksonModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/JacksonModule.java>`_ implementation.

  .. tip::

    To implement a new Jackson `Module <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/JacksonModule.java>`_ for adding a basic custom serializer or deserializer, you can
    use the `com.fasterxml.jackson.databind.module.SimpleModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/module/SimpleModule.java>`_.

    Note, that if you want to register a `JsonSerializer` or `JsonDeserializer` over a parameterized
    type, such as a `Collection[T]` or `Map[T, U]`, that you should instead implement
    `com.fasterxml.jackson.databind.deser.Deserializers <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/deser/Deserializers.java>`_
    or `com.fasterxml.jackson.databind.ser.Serializers <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/ser/Serializers.java>`_
    which provide callbacks to match the full signatures of the class to deserialize into via a
    Jackson `JavaType`.

    Also note that with this usage it is generally recommended to add your `Serializers` or
    `Deserializers` implementation via a |jackson-module-scala|_ `JacksonModule <https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/scala/tools/jackson/module/scala/JacksonModule.scala>`_.
    (which is an extension of `com.fasterxml.jackson.databind.JacksonModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/JacksonModule.java>`_
    and can thus be used in place). See example below.

- Add your serializer or deserializer using the `SimpleModule#addSerializer` or `SimpleModule#addDeserializer` methods in your module.
- In your custom |ScalaObjectMapperModule|_ extension, add the `JacksonModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/JacksonModule.java>`_ implementation to list of additional Jackson modules by overriding and implementing the `ScalaObjectMapperModule#additionalJacksonModules`.

For example: first create the serializer or deserializer (a deserializer example is shown below)

.. code:: scala

    import com.fasterxml.jackson.databind.JsonDeserializer
    import com.fasterxml.jackson.databind.deser.Deserializers

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

Then add via a Jackson `SimpleModule` or a |jackson-module-scala|_ `JacksonModule`:

.. code:: scala

    import com.fasterxml.jackson.databind.module.SimpleModule
    import com.fasterxml.jackson.module.scala.JacksonModule

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

.. note::

    It is also important to note that `Jackson <https://github.com/FasterXML/jackson-databind>`_
    Modules are **not** Google `Guice <https://github.com/google/guice>`_ Modules but are instead
    interfaces for extensions that can be registered with a Jackson `ObjectMapper` in order to
    provide a well-defined set of extensions to default functionality. In this way, they are similar
    in concept to Google `Guice <https://github.com/google/guice>`__ Modules, but for configuring an
    `ObjectMapper` instead of an `Injector`.

Lastly, add the custom serializer or deserializer to your customized Finatra |ScalaObjectMapperModule|_:

.. code-block:: scala
   :emphasize-lines: 4, 5, 6, 7, 8, 9, 10, 11, 12, 13

    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    object MyCustomObjectMapperModule extends ScalaObjectMapperModule {
      override val additionalJacksonModules = Seq(
        // added via a new anonymous SimpleModule
        new SimpleModule {
          addSerializer(LocalDateParser)
        },
        // added via a re-usable SimpleModule
        new FooDeserializerModule,
        // added via a re-usable JacksonModule
        new MapIntIntDeserializerModule
      )
    }

For more information see the Jackson documentation for
`Custom Serializers <https://github.com/FasterXML/jackson-docs/wiki/JacksonHowToCustomSerializers>`__.

You would then use this module in your server. See the `Modules Configuration in Servers <../getting-started/modules.html#module-configuration-in-servers>`_
or the HTTP Server `Framework Modules <../http/server.html#framework-modules>`_ for more information
on how to make use of any custom |ScalaObjectMapperModule|_.

Improved `case class` deserializer
----------------------------------

The TwitterUtil |ScalaObjectMapper|_ provides a custom `case class deserializer <https://github.com/twitter/util/blob/develop/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass/CaseClassDeserializer.scala>`__
which overcomes some limitations in |jackson-module-scala|_ and it is worth understanding some its
features:

-  Throws a `JsonMappingException` when required fields are missing from the parsed JSON.
-  Uses specified `case class` default values when fields are missing in the incoming JSON.
-  Properly deserializes a `Seq[Long]` (see: https://github.com/FasterXML/jackson-module-scala/issues/62).
-  Supports `"wrapped values" <https://docs.scala-lang.org/overviews/core/value-classes.html>`__ using `c.t.util.jackson.WrappedValue <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/WrappedValue.scala>`_.
-  Support for field and method level validations via integration with the `util-validator <https://twitter.github.io/util/guide/util-validator/index.html>`__ Bean Validation 2.0 style validations during JSON deserialization.
-  Accumulates all JSON deserialization errors (instead of failing fast) in a returned sub-class of `JsonMappingException` (see: `CaseClassMappingException <https://github.com/twitter/util/blob/develop/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass/exceptions/CaseClassMappingException.scala>`_).

The `case class` deserializer is added by default when constructing a new |ScalaObjectMapper|_.

.. tip::

  Note: with the TwitterUtil `case class` deserializer, non-option fields without default values are
  **considered required**.

  If a required field is missing, a `CaseClassMappingException` is thrown.

Jackson InjectableValues Support
--------------------------------

By default, the Finatra provides a |ScalaObjectMapper|_ via the |ScalaObjectMapperModule|_ configured
to resolve Jackson `InjectableValues <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/InjectableValues.java>`_
via a given Google `Guice <https://github.com/google/guice>`_ `Injector <https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Injector.html>`_.

The default is very similar to the `jackson-module-guice <https://github.com/FasterXML/jackson-modules-base/tree/master/guice>`_ --
`GuiceInjectableValues <https://github.com/FasterXML/jackson-modules-base/blob/master/guice/src/main/java/tools/jackson/module/guice/GuiceInjectableValues.java>`_.

.. note::

    Jackson “InjectableValues” is not related to `Dependency Injection <../getting-started/dependency_injection.html#dependency-injection>`_
    or Google `Guice <https://github.com/google/guice>`_. It is meant to convey the filling in of a
    value in a deserialized object from somewhere other than the incoming JSON. In Jackson parlance,
    this is “injection” of a value.

The Finatra `c.t.finatra.jackson.caseclass.GuiceInjectableValues <https://github.com/twitter/finatra/blob/release/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/GuiceInjectableValues.scala>`_
allows users to denote fields in the case class to fill with values that come from a configured Google `Guice <https://github.com/google/guice>`_
`Injector <https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Injector.html>`_
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

As mentioned, this is essentially the same functionality found in the `jackson-module-guice <https://github.com/FasterXML/jackson-modules-base/tree/master/guice>`__
`GuiceInjectableValues <https://github.com/FasterXML/jackson-modules-base/blob/master/guice/src/main/java/tools/jackson/module/guice/GuiceInjectableValues.java>`__
with a key difference being that the Finatra version ensures we do not attempt lookup of a field from
the Guice Injector **unless there is a non-null Guice Injector configured and the field is specifically annotated**
with one of the supporting annotations.

Using the `Foo` case class above, you could then parse incoming JSON with the |ScalaObjectMapper|_:

.. code:: scala

    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
    import com.twitter.util.jackson.ScalaObjectMapper
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
    val mapper: ScalaObjectMapper = (new ScalaObjectMapper).objectMapper(injector.underlying)
    val foo = mapper.parse[Foo](json)

When deserializing the JSON string into an instance of `Foo`, the mapper will attempt to locate an
instance of type `Bar` from the given injector and use it in place of the `bar` field in the `Foo`
case class.

.. caution::

    It is an error to specify multiple field injection annotations on a field, and it is also an
    error to use a field injection annotation in conjunction with **any** `JacksonAnnotation <https://github.com/FasterXML/jackson-annotations/blob/a991c43a74e4230eb643e380870b503997674c2d/src/main/java/com/fasterxml/jackson/annotation/JacksonAnnotation.java#L9>`_.

    Both of these cases will result in error during deserialization of JSON into the case class when
    using the |ScalaObjectMapper|_.

As mentioned, the Finatra HTTP integration provides extended Jackson `InjectableValues` support
specifically for injecting values into a `case class` which can be obtained from different parts of
an HTTP message. This uses the TwitterUtil `@InjectableValues <http://twitter.github.io/util/guide/util-jackson/index.html#injectablevalue>`__
annotation on specific Finatra HTTP request related `annotations <../http/requests.html#field-annotations>`_.

See the `HTTP Requests - Field Annotations <../http/requests.html#field-annotations>`_ documentation
for more details on HTTP Message "injectable values".

.. important::

    It is important to note that Jackson currently only allows registration of a single `InjectableValues`
    implementation for an ObjectMapper.

    Thus, if you configure your bound |ScalaObjectMapper|_ with a different implementation, the
    default behavior provided by the Finatra `GuiceInjectableValues` will be overridden.

`Mix-in Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
----------------------------------------------------------------------------------------------

The Jackson `Mix-in Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
provides a way to associate annotations to classes without needing to modify the target classes
themselves. It is intended to help support 3rd party datatypes where the user cannot modify the
sources to add annotations.

The TwitterUtil |ScalaObjectMapper|_ supports Jackson `Mix-in Annotations <https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations>`_
for specifying field annotations during deserialization with the `case class deserializer <https://github.com/twitter/util/blob/develop/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass/CaseClassDeserializer.scala>`_.

For example, to deserialize JSON into the following classes that are not yours to annotate:

.. code:: scala

    case class Point(x: Int, y: Int) {
      def area: Int = x * y
    }

    case class Points(points: Seq[Point])

However, you want to enforce field constraints with `validations <./validations.html>`_
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

Implement via a Custom |ScalaObjectMapperModule|_
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- First, create a new Jackson `com.fasterxml.jackson.databind.JacksonModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/JacksonModule.java>`__ implementation. You can use the `com.fasterxml.jackson.databind.module.SimpleModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/module/SimpleModule.java>`_.
- Add your `MixIn` using the `SimpleModule#setMixInAnnotation` method in your module.
- In your custom |ScalaObjectMapperModule|_ extension, add the `JacksonModule <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/tools/jackson/databind/JacksonModule.java>`__.

For example, create a new Jackson `SimpleModule`:

.. code:: scala

    import com.fasterxml.jackson.databind.module.SimpleModule

    object PointMixInModule extends SimpleModule {
        setMixInAnnotation(classOf[Point], classOf[PointMixIn]);
    }

Then add the `SimpleModule` to the list of additional Jackson modules in your custom |ScalaObjectMapperModule|_:

.. code:: scala

    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    object MyCustomObjectMapperModule extends ScalaObjectMapperModule {
      override val additionalJacksonModules = Seq(PointMixInModule)
    }

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

.. |ScalaObjectMapper| replace:: `ScalaObjectMapper`
.. _ScalaObjectMapper: https://github.com/twitter/util/blob/develop/util-jackson/src/main/scala/com/twitter/util/jackson/ScalaObjectMapper.scala

.. |ScalaObjectMapperModule| replace:: `ScalaObjectMapperModule`
.. _ScalaObjectMapperModule: https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala

.. |YamlScalaObjectMapperModule| replace:: `YamlScalaObjectMapperModule`
.. _YamlScalaObjectMapperModule: https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/YamlScalaObjectMapperModule.scala


.. |jackson-module-scala| replace:: `jackson-module-scala`
.. _jackson-module-scala: https://github.com/FasterXML/jackson-module-scala

