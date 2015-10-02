---
layout: user_guide
title: "Working With JSON"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Working With JSON</li>
</ol>

## Basics
===============================

Finatra improves on the already excellent [jackson-module-scala](https://github.com/FasterXML/jackson-module-scala). JSON support is provided in the [finatra-jackson](https://github.com/FasterXML/jackson) library which can be used outside of Finatra as a replacement for the [jackson-scala-module](https://github.com/FasterXML/jackson-module-scala) or [jerkson](https://github.com/codahale/jerkson).

### Features

* Usable outside of Finatra.
* [`FinatraObjectMapper`](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala) which provides additional Scala friendly methods not found in the `ScalaObjectMapper`.
* [Guice module](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala) for injecting `FinatraObjectMapper` (with support for customization e.g. snake_case vs camelCase).
* Custom [`case class` deserializer](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/jackson/FinatraCaseClassDeserializer.scala) which overcomes limitations in jackson-scala-module.
* Support for `case class` validations which accumulate errors (without failing fast) during JSON parsing.
* Integration with Finatra [HTTP routing](/finatra/user-guide/routing-json) to support binding and validation of query params, route params, and headers.
* Utils for comparing JSON in tests.
* Experimental support for iterator based JSON stream parsing.

## Configuration
===============================

The default configuration of Jackson is provided by the [`FinatraObjectMapper`](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala).

The following Jackson integrations are provided by default:

* [Joda Module](https://github.com/FasterXML/jackson-datatype-joda/blob/master/src/main/java/com/fasterxml/jackson/datatype/joda/JodaModule.java)
* [Scala Module](https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/java/com/fasterxml/jackson/module/scala/ScalaModule.java)
* [LongKeyDeserializer](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/internal/serde/LongKeyDeserializer.scala): Allow deserializing maps with long keys.
* [Wrapped Value Serializer](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/wrapped/WrappedValueSerializer.scala)
* [Duration Millis Serializer](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/internal/serde/DurationMillisSerializer.scala)
* [Improved DateTime Deserializer](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/internal/serde/FinatraDatetimeDeserializer.scala)
* [Improved `case class` Deserializer](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/jackson/FinatraCaseClassDeserializer.scala): See details [below](#case-class-deserializer).

## <a name="jackson-customization">Customization</a>
===============================

To override defaults or provide other config options, specify your own module (usually extending [FinatraJacksonModule](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala)).
```scala
class Server extends HttpServer {
  override def jacksonModule = CustomJacksonModule
  ...
}

object CustomJacksonModule extends FinatraJacksonModule {
  override val additionalJacksonModules = Seq(
    new SimpleModule {
      addSerializer(LocalDateParser)
    })

  override val serializationInclusion = Include.NON_EMPTY

  override val propertyNamingStrategy = CamelCasePropertyNamingStrategy

  override def additionalMapperConfiguration(mapper: ObjectMapper) {
    mapper.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true)
  }
}
```

## Improved `case class` deserializer
===============================

Finatra provides a custom [`case class` deserializer](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/jackson/FinatraCaseClassDeserializer.scala) which overcomes limitations in jackson-scala-module:

* Throws a `JsonException` when non-optional fields are missing from the parsed JSON.
* Use default values when fields are missing in the incoming JSON.
* Properly deserialize a `Seq[Long]` (see https://github.com/FasterXML/jackson-module-scala/issues/62).
* Support "wrapped values" using `WrappedValue` (this is needed since jackson-scala-module does not support `@JsonCreator`).
* Support for accumulating JSON parsing errors (instead of failing fast).
* Support for field and method level validations which also accumulate errors.

## <a name="routing-json" href="#routing-json">Integration with Routing</a>
===============================

If a custom `case class` is used as a route callback's input type, Finatra will parse the request body into the custom request class. Similar to declaratively parsing a `GET` request (described above), Finatra will perform validations and return a `400 BadRequest` with a list of any accumulated errors in JSON format.

Suppose you wanted to handle POST's of the following JSON (representing a group of tweet ids):
```json
{
  "name": "EarlyTweets",
  "description": "Some of the earliest tweets on Twitter.",
  "tweetIds": [20, 22, 24],
  "dates": {
    "start": "1",
    "end": "2"
  }
}
```

You would create and use the following <a name="group-request-example">`case classes`</a>
```scala
case class GroupRequest(
  @NotEmpty name: String,
  description: Option[String],
  tweetIds: Set[Long],
  dates: Dates) {

  @MethodValidation
  def validateName = {
    ValidationResult(
      name.startsWith("grp-"),
      "name must start with 'grp-'")
  }
}

case class Dates(
  @PastTime start: DateTime,
  @PastTime end: DateTime)
```

## Validation Framework
===============================

We provide a simple validation framework inspired by [JSR-330](https://github.com/google/guice/wiki/JSR330). The validations framework integrates with the custom `case class` deserializer to efficiently apply per field validations as request parsing is performed. The following validations are included (and additional validations can be easily provided):

* CountryCode
* FutureTime
* PastTime
* Max
* Min
* NotEmpty
* OneOf
* Range
* Size
* TimeGranularity
* UUID
* MethodValidation

#### Method Validation

Can be used for:

* Non-generic validations. `MethodValidation` can be used instead of defining a reusable annotation and validator.
* Cross-field validations (e.g. `startDate` before `endDate`)

See the implementation of the `GroupRequest` [above](#group-request-example) for an example of using `MethodValidation`.

See also: [CommonMethodValidations](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/validation/CommonMethodValidations.scala)

### <a name="json-best-practices">Best Practices</a>
===============================

Use Java Enums for representing enumerations since they integrate well with Jackson's ObjectMapper and now have exhaustiveness checking as of Scala 2.10. The following Jackson annotations may be useful when working with Enums:

* `@JsonCreator`: Useful on a custom fromString method
* `@JsonValue`: Useful to place on an overridden toString method

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-http-server"><span aria-hidden="true">&larr;</span>&nbsp;Building&nbsp;a&nbsp;new&nbsp;HTTP&nbsp;Server</a></li>
    <li class="next"><a href="/finatra/user-guide/working-with-files">Working&nbsp;with&nbsp;Files&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
