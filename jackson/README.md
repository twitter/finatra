Jackson
==========================================================

# Summary
finatra-jackson is a library integrating jackson, scala, and guice.

## Features
* Usable outside of Finatra.
* [`FinatraObjectMapper`](../jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala) which provides additional Scala friendly methods not found in `ScalaObjectMapper`.
* Guice module for injecting `FinatraObjectMapper` (with support for customization e.g. snake_case vs camelCase).
* Custom `case class` deserializer which overcomes limitations in jackson-scala-module.
* Support for `case class` validations which accumulate errors (without failing fast) during json parsing.

Integration with Finatra HTTP routing to support binding and validation of query params, route params, and headers.
* Utils for comparing json in tests.
* Experimental support for iterator based json stream parsing.

## Configuration
The default configuration of Jackson is provided by the [`FinatraObjectMapper`](../jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala).

The following Jackson integrations are provided by default:

* [Joda Module](https://github.com/FasterXML/jackson-datatype-joda/blob/master/src/main/java/com/fasterxml/jackson/datatype/joda/JodaModule.java)
* [Scala Module](https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/java/com/fasterxml/jackson/module/scala/ScalaModule.java)
* [LongKeyDeserializer](../jackson/src/main/scala/com/twitter/finatra/json/internal/serde/LongKeyDeserializer.scala): Allow deserializing maps with long keys.
* [Wrapped Value Serializer](../jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/wrapped/WrappedValueSerializer.scala)
* [Duration Millis Serializer](../jackson/src/main/scala/com/twitter/finatra/json/internal/serde/DurationMillisSerializer.scala)
* [Improved DateTime Deserializer](../jackson/src/main/scala/com/twitter/finatra/json/internal/serde/FinatraDatetimeDeserializer.scala)
* [Improved `case class` Deserializer](../jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/jackson/FinatraCaseClassDeserializer.scala): See details [below](#case-class-deserializer).

### <a name="jackson-customization">Customization</a>
To override defaults or provide other config options, specify your own module (usually extending [FinatraJacksonModule](../jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala)).
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

## <a name="case-class-deserializer">Improved `case class` deserializer</a>
Finatra provides a custom `case class` deserializer which overcomes limitations in jackson-scala-module:
* Throw a JsonException when 'non Option' fields are missing in the incoming json
* Use default values when fields are missing in the incoming json
* Properly deserialize a Seq\[Long\] (see https://github.com/FasterXML/jackson-module-scala/issues/62)
* Support "wrapped values" using `WrappedValue` (this is needed since jackson-scala-module does not support `@JsonCreator`)
* Support for accumulating JSON parsing errors (instead of failing fast).
* Support for field and method level validations which also accumulate errors.

## Integration with Routing
If a custom `case class` is used as a route callback's input type, Finatra will parse the request body into the custom request. Similar to declaratively parsing a GET request (described above), Finatra will perform validations and return a 400 BadRequest with a list of the accumulated errors (in JSON format).

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

Then you'd create the following <a name="group-request-example">`case classes`</a>
```scala
case class GroupRequest(
  @NotEmpty name: String,
  description: Option[String],
  tweetIds: Set[Long],
  dates: Dates) {

  @MethodValidation
  def validateName = {
    ValidationResult.validate(
      name.startsWith("grp-"),
      "name must start with 'grp-'")
  }
}

case class Dates(
  @PastTime start: DateTime,
  @PastTime end: DateTime)
```

Validation Framework
===============================
We provide a simple validation framework inspired by [JSR-330](https://github.com/google/guice/wiki/JSR330). Our framework integrates with our custom `case class` deserializer to efficiently apply per field validations as request parsing is performed. The following validations are included, and additional validations can be provided:

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

## Method Validation

Can be used for:

* Non-generic validations -- a `MethodValidation` can be used instead of defining a reusable annotation and validator.
* Cross-field validations (e.g. `startDate` before `endDate`)

See the implementation of the `GroupRequest` [above](#group-request-example) for an example of using `MethodValidation`.

See also: [CommonMethodValidations](../jackson/src/main/scala/com/twitter/finatra/validation/CommonMethodValidations.scala)

## <a name="json-best-practices">Best Practices</a>
Use Java Enums for representing enumerations since they integrate well with Jackson's ObjectMapper and now have exhaustiveness checking as of Scala 2.10. The following Jackson annotations may be useful when working with Enums:

* @JsonCreator: Useful on a custom fromString method
* @JsonValue: Useful to place on an overridden toString method

Note:
-----------------------------------------------------------
Classes/objects in internal packages, e.g. `com.twitter.finatra.json.internal.*` are Finatra framework internal implementation details.
These are meant to be private to the framework and not intended as publicly accessible as they are details specific to the framework and
are thus more subject to breaking changes. You should not depend on their implementations remaining constant since they are not intended
for use outside of the framework itself.
