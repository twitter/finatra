Finatra HTTP Overview
==========================================================

Quick Start
-----------------------------------------------------------
* Depend on the `com.twitter.finatra:finatra-http` library.
* We also recommend depending on `com.twitter.finatra:finatra-slf4j` and `ch.qos.logback:logback-classic` to choose [Logback][logback] as your [slf4j](http://www.slf4j.org/manual.html) implementation.
* See the [finatra-hello-world](../examples/finatra-hello-world) example.
* See also [todo list example][quick-start].

Controllers and Routing
======================================================
Routes are defined inside a [Controller][controller] and are comprised of:

- an [HTTP method](http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html)
- a matching pattern
- and a callback function.

When Finatra receives an HTTP request, it will scan all registered controllers (in the order they are added) and dispatch the request to the first matching route starting from the top of each controller.

The following route will return "hi" when an HTTP GET of / is received.
```scala
class Example extends Controller {
  get("/") { request: Request =>
    "hi"
  }
}
```

## <a name="controller-best-practices">Best Practices</a>
* Follow REST conventions if possible. When deciding which routes to group into a particular controller, group routes related to a single resource into one controller. The per-route stats in [StatsFilter](../http/src/main/scala/com/twitter/http/filters/StatsFilter.scala) work best when this convention is followed.

```scala
class GroupsController extends Controller {
  get("/groups/:id") { ... }

  post("/groups") { ... }

  delete("/groups/:id") { ... }
}
```

yields the following stats:

```
route/groups_id/GET/...
route/groups/POST/...
route/groups_id/DELETE/...
```

Alternatively, each route can be assigned a name which will then be used to create stat names.

```scala
class GroupsController extends Controller {
  get("/groups/:id", name = "group_by_id") { ... }

  post("/groups", name = "create_group") { ... }

  delete("/groups/:id", name = "delete_group") { ... }
}
```

which will yield the stats:

```
route/group_by_id/GET/...
route/create_group/POST/...
route/delete_group/DELETE/...
```

## Route Matching Patterns

### Named Parameters

Route patterns may include named parameters:
```scala
get("/users/:id") { request: Request =>
  "You looked up " + request.params("id")
}
```

*Note: Query params and path params are both stored in the "params" field of the request. If a path parameter and a query param have the same name, the path param always wins. Therefore, ensure your path param names do not collide with a query param name that you plan to read.*

### Wildcard Parameter

Routes can also contain the wildcard pattern. The wildcard can only appear once at the end of a pattern, and it will capture all text in its place. For example
```scala
get("/files/:*") { request: Request =>
  request.params("*")
}
```
For a GET of `/files/abc/123/foo.txt` the endpoint will return `abc/123/foo.txt`

### Admin Paths

Any path starting with `/admin/` will be exposed only on the Server's admin port. All TwitterServer based services get an [HTTP admin interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) for exposing internal endpoints such as stats. The admin endpoint should never be exposed outside your DMZ).

```scala
get("/admin/users/") { request: Request =>
  userDatabase.getAllUsers(
    request.params("cursor"))
}
```

### Regular Expressions
The following will optionally match a trailing slash (e.g. /users or /users/)
```scala
get("/users/?") { request: Request =>
  ...
}
```
*Note: You cannot mix named path params with regular expression capture groups. If you require regular expression groups, make sure they are non-capturing e.g. (?:...)*

## Requests
Each route has a callback which is executed when the route matches a request. Callbacks require explicit input types and Finatra will then try to convert the incoming request into the specified input type. Finatra supports two request types:

- [Finagle HTTP Request](https://twitter.github.io/finagle/docs/index.html#com.twitter.finagle.http.Request)
- A custom `case class` Request

### Finagle `com.twitter.finagle.http.Request`
This is a [com.twitter.finagle.http.Request](https://twitter.github.io/finagle/docs/index.html#com.twitter.finagle.http.Request) which contains common HTTP attributes.

### Custom `case class` Request
Custom requests allow declarative request parsing with support for type conversions, default values, and validations.

For example suppose you wanted to parse a GET request with three query params -- `max`, `startDate`, and `verbose`:
```
http://foo.com/users?max=10&start_date=2014-05-30TZ&verbose=true
```

This can be parsed with the following `case class`:
```scala
case class UsersRequest(
  @Max(100) @QueryParam max: Int,
  @PastDate @QueryParam startDate: Option[DateTime],
  @QueryParam verbose: Boolean = false)
```

The custom `UsersRequest` is then used as the callbacks input type:
```scala
get("/users") { request: UsersRequest =>
  request
}
```

The following integration test shows how this endpoint will now respond.
```scala
server.httpGet(
  path = "/users?start_date=2013&max=10&verbose=true",
  andExpect = Ok,
  withJsonBody = """
  {
    "start_date": "2013-01-01T00:00:00.000Z",
    "max": 10,
    "verbose": true
  }
  """)

server.httpGet(
  path = "/users?max=10",
  andExpect = Ok,
  withJsonBody = """
  {
    "max": 10,
    "verbose": false
  }
  """)

server.httpGet(
  path = "/users?max=10&verbose=true",
  andExpect = Ok,
  withJsonBody = """
  {
    "max": 10,
    "verbose": true
  }
  """)

server.httpGet(
  path = "/users?verbose=5",
  andExpect = BadRequest,
  withJsonBody = """
  {
    "errors": [
      "max is a required field",
      "verbose's value '5' is not a valid boolean"]
    ]
  }
  """)
```

Notes:

* The `case class` field names must match the request parameter names. However, a [PropertyNamingStrategy](http://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/PropertyNamingStrategy.html) can be configured to handle common name substitutions (e.g. snake_case or camelCase). By default, snake_case is used (defaults are set in [`FinatraJacksonModule`][finatra-jackson-module]).

* Non optional fields without default values are required. If required fields are missing, a `CaseClassMappingException` is thrown. Normally, the default ExceptionMapper (included in `ExceptionMapperModule`) turns this exception into a HTTP 400 BadRequest with a JSON errors array (however this behavior can be customized).

* The following field annotations specify where to parse the field out of the request
  * Request Fields
     * `@RouteParam`
     * `@QueryParam`
          * *Note: Ensure that route param names do not collide with QueryParam names. Otherwise, a QueryParam could end up parsing a route param*
     * `@Header`
     * `@Cookie`
 * Other
     * `@RequestInject`: Injects the Finagle Http Request

*Note: HTTP requests with a content-type of application/json, are similarly parsed (but "Request Field" annotations are ignored). See [JSON](#json) section below.*

## Responses

Finatra will convert your route callbacks return type into a `Future[Response]` using the following rules:

* If you return a `Future[Response]`, then no conversion will be performed
* A non `Future` return value will be converted into a `Future` using a Finatra provided `FuturePool` (see [Future Conversion](#future-conversion) section for more details)
* `Some[T]` will be converted into a HTTP 200 OK
* `None` will be converted into a HTTP 404 NotFound
* Non-response classes will be converted into a HTTP 200 OK

### Response Builder
All Controllers have a protected "response" field they can use to build responses. For example:

```scala
get("/foo") { request: Request =>
  response.
    ok.
    header("a", "b").
    json("""
    {
      "name": "Bob",
      "age": 19
    }
    """)
}

get("/foo") { request: Request =>
  response.
    status(999).
    body(bytes)
}

get("/redirect") { request: Request =>
  response.
    temporaryRedirect.
    location("/foo/123")
}

post("/users") { request: FormPostRequest =>
  response.created.
    location("123").
    html(
      TestUserView(request.age, request.name))
}
```

### Response Exceptions
Responses can be embedded inside exceptions with `.toException`. You can throw the exception to terminate control flow, or wrap it inside a `Future.exception` to return a failed `Future`. However, instead of directly returning error responses in this manner, a better convention is to handle application-specific exceptions in an `ExceptionMapper`.

```scala
get("/NotFound") { request: Request =>
  response.notFound("abc not found").toFutureException
}

get("/ServerError") { request: Request =>
  response.internalServerError.toFutureException
}

get("/ServiceUnavailable") { request: Request =>
  // can throw a raw exception too
  throw response.serviceUnavailable.toException
}
```

### Setting Response Location Header
ResponseBuilder has a "location" method.
```scala
post("/users") { request: Request =>
  response.created.
    location("http").
    html(
      TestUserView(request.age, request.name))
}
```

 which can be used:

   * if the URI starts with "http" or "/" then the URI is placed in the Location header unchanged.
   * `response.location("123")` will get turned into the correct full URL in the [HttpResponseFilter](../http/src/main/scala/com/twitter/http/filters/HttpResponseFilter.scala) (e.g. `http://host.com/users/123`)

Or to obtain the request full path URL as follows:
	```
	RequestUtils.pathUrl(request)
	```

### <a name="future-conversion">Future Conversion</a>
Callbacks that do not return a Future will have their return values converted into a future by running the callback in a framework future pool. If you wish to have greater control into the future pool's threading strategy, override the following:
```scala
class Server extends HttpServer {
  override def callbackModule = CustomCallbackModule
  ...
}
```

See [CallbackConverterModule][CallbackConverterModule] for details.

*Note: For processes requiring blocking or long running CPU computations, consider creating specific future pools inside your application, and then directly returning a future from your route callbacks.*

Server and Router
======================================================

The Finatra convention is to create a Scala object with a name ending in "Main" as so
```scala
object MyServerMain extends MyServer

class MyServer extends HttpServer {...}
```

This allows your server to be instantiated multiple times in tests without worrying about static state persisting across test runs in the same JVM. `MyServerMain` is then the static object which contains the runnable *main method*.

Message Body Readers and Writers
======================================================
Documentation coming soon. See [example](src/test/scala/com/twitter/integration/tweetexample/main/TweetsEndpointServer.scala#L21).


<a name="json">JSON</a>
===============================
Finatra improves on the already excellent [jackson-module-scala](https://github.com/FasterXML/jackson-module-scala). JSON support is provided in the [finatra-jackson][finatra-jackson] library, which can be used outside of Finatra HTTP as a replacement for [jackson-scala-module](https://github.com/FasterXML/jackson-module-scala) or [jerkson](https://github.com/codahale/jerkson).

## Features
* Usable outside of Finatra.
* [`FinatraObjectMapper`](../jackson/src/main/scala/com/twitter/json/FinatraObjectMapper.scala) which provides additional Scala friendly methods not found in `ScalaObjectMapper`.
* Guice module for injecting `FinatraObjectMapper` (with support for customization e.g. snake_case vs camelCase).
* Custom `case class` deserializer which overcomes limitations in jackson-scala-module.
* Support for `case class` validations which accumulate errors (without failing fast) during json parsing.

Integration with Finatra HTTP routing to support binding and validation of query params, route params, and headers.
* Utils for comparing json in tests.
* Experimental support for iterator based json stream parsing.

## Configuration
The default configuration of Jackson is provided by the [`FinatraObjectMapper`](../jackson/src/main/scala/com/twitter/json/FinatraObjectMapper.scala).

The following Jackson integrations are provided by default:

* [Joda Module](https://github.com/FasterXML/jackson-datatype-joda/blob/master/src/main/java/com/fasterxml/jackson/datatype/joda/JodaModule.java)
* [Scala Module](https://github.com/FasterXML/jackson-module-scala/blob/master/src/main/java/com/fasterxml/jackson/module/scala/ScalaModule.java)
* [LongKeyDeserializer](../jackson/src/main/scala/com/twitter/json/internal/serde/LongKeyDeserializer.scala): Allow deserializing maps with long keys.
* [Wrapped Value Serializer](../jackson/src/main/scala/com/twitter/json/internal/caseclass/wrapped/WrappedValueSerializer.scala)
* [Duration Millis Serializer](../jackson/src/main/scala/com/twitter/json/internal/serde/DurationMillisSerializer.scala)
* [Improved DateTime Deserializer](../jackson/src/main/scala/com/twitter/json/internal/serde/FinatraDatetimeDeserializer.scala)
* [Improved `case class` Deserializer](../jackson/src/main/scala/com/twitter/json/internal/caseclass/jackson/FinatraCaseClassDeserializer.scala): See details [below](#case-class-deserializer).

### <a name="jackson-customization">Customization</a>
To override defaults or provide other config options, specify your own module (usually extending [FinatraJacksonModule](../jackson/src/main/scala/com/twitter/json/modules/FinatraJacksonModule.scala)).
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
    ValidationResult(
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

See also: [CommonMethodValidations](../jackson/src/main/scala/com/twitter/validation/CommonMethodValidations.scala)

## <a name="json-best-practices">Best Practices</a>
Use Java Enums for representing enumerations since they integrate well with Jackson's ObjectMapper and now have exhaustiveness checking as of Scala 2.10. The following Jackson annotations may be useful when working with Enums:

* @JsonCreator: Useful on a custom fromString method
* @JsonValue: Useful to place on an overridden toString method

Guice
===============================
The Finatra framework internally uses Guice extensively, and it's also availble for service writers if they choose to use it. For projects not wishing to use Guice, please see [FinatraWithoutGuice].

Finatra's Guice integration usually starts with Controllers as the root objects in the object graph. As such, controllers are added to Finatra's router by type as such:
```scala
class Server extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    val controller = new Controller(...)
    router.add[MyController]
  }
}
```

Controllers are then annotated with [JSR-330](https://github.com/google/guice/wiki/JSR330) inject annotations (Note: The somewhat strange syntax required to add an annotation to a Scala class constructor:
```scala
class MyController @Inject()(
  dao: GroupsDAO,
  service: FooService)
  extends Controller
```

## Modules
We provide a [TwitterModule](../inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala) base class which extends the capabilities of the excellent [scala-guice-module](https://github.com/codingwell/scala-guice).

### Module Definition
* Twitter Util Flags can be defined inside modules. This allows various reusable modules that require external configuration to be composed in a server.
* Prefer using a `@Provider` method over using the *bind* dsl.
* Usually modules are Scala *objects* since the modules contain no state and usage of the module is less verbose.
* Always remember to add `@Singleton` to your provides method if desired.
* Usually, modules are only required for creating classes that you don't control. Otherwise, you would simply add the JSR inject annotations directly to the class. For example, suppose you need to create an `ThirdPartyFoo` class which comes from a thirdparty jar. You could create the following Guice module to construct a singleton `ThirdPartyFoo` class which is created with a key provided through a command line flag.

```scala
object MyModule1 extends TwitterModule {
  val key = flag("key", "defaultkey", "The key to use.")

  @Singleton
  @Provides
  def providesThirdPartyFoo: ThirdPartyFoo = {
    new ThirdPartyFoo(key())
  }
}
```

### Module Configuration
A server is then started with a list of immutable Guice modules:
```scala
class Server extends HttpServer {
  override val modules = Seq(
    MyModule1,
    MyModule2)

  ...
}
```

## Request Scope
Guice supports custom scopes in addition to the most common Singleton and *Unscoped*. Request scopes are often used to allow injecting classes that change depending on the incoming request (e.g. the authenticated User). Finatra provides an implementation of a *request scope* that works across Finagle non-blocking threads (Guice's included request scope implementation uses ThreadLocal's which will not work with Finagle Futures).

Note: Fields added to the Finagle request scope will remain present in threads launched from [Finagle future pools](https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/util/FuturePool.scala)

### Adding Classes into the Finatra Request Scope

First add a jar dependency on inject-request-scope

Then define a module
```scala
import com.myapp.User
import com.twitter.finatra.requestscope.RequestScopeBinding
import com.twitter.inject.TwitterModule

object UserModule
  extends TwitterModule
  with RequestScopeBinding {

  override def configure() {
    bindRequestScope[User]
  }
}
```

Then define a Filter to seed the User into the Finatra *Request Scope*
```scala
class UserFilter @Inject()(
  requestScope: FinagleRequestScope)
  extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val userId = parseUserId(request.cookie)
    val user = User(userId)
    requestScope.seed[User](user)
    service(request)
  }
}
```

Next add the [`FinagleRequestScopeFilter`](../inject/inject-request-scope/src/main/scala/com/twitter/inject/requestscope/FinagleRequestScopeFilter.scala) filter to your server before the `UserFilter` (shown below w/ other common filters in a recommended filter order):
```scala
class Server extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    router.
      filter[FinagleRequestScopeFilter].
      filter[UserFilter].
      add[MyController1]
    }
}
```

Then inject a User or a `Provider[User]` wherever you need to access the request scope user. Note, `Provider[User]` must be used when injecting into a Singleton class.
```scala
import javax.inject.Provider


class MyController @Inject()(
  dao: GroupsDAO,
  user: Provider[User])
  extends Controller {

  get("/") { request: Request =>
    "The incoming user has id " + user.get.id
  }
}
```

## <a name="guice-best-practices">Best Practices</a>
* The server's injector is available as a protected method in `HttpServer`, but it's use should be avoided except for calling *warmup* classes, and for extending the Finatra framework.
* Avoid `@Named` annotations in favor of specific [Binding Annotations](https://github.com/google/guice/wiki/BindingAnnotations). If building with Maven, simply place your Java annotations in src/main/java for cross-compilation with your Scala code.

Finatra Without Guice
===============================

```scala
class NonGuiceServer extends HttpServer {
  val key = flag("key", "123", "The Key to use")

  override def configureHttp(router: HttpRouter) {
    val keyValue = key()
    val controller = new Controller(keyValue, ...)
    router.add(controller)
  }
}
```

<a name="filters">Filters</a>
===============================
Per controller filters are added as such:

```scala
class Server extends HttpServer {
  override configureHttp(router: HttpRouter) {
    router.
      add[MyFilter, MyController]
  }
}
```

A common filter order is as follows:

```scala
class Server extends HttpServer {
  override configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter[Request]].
      filter[StatsFilter].
      filter[ExceptionMappingFilter[Request]].
      filter[LoggingMDCFilter].
      filter[FinagleRequestScopeFilter].
      filter[UserFilter].
      filter[HttpResponseFilter[Request]].
      add[MyController1].
      add[MyController2]
  }
}
```

Finatra composes some commonly used filters into [`com.twitter.finatra.http.filters.CommonFilters`](../http/src/main/scala/com/twitter/http/filters/CommonFilters.scala). `CommonFilters` can be added in the same manner as any other filter, e.g.,

```scala
class Server extends HttpServer {
  override configureHttp(router: HttpRouter) {
    router.
      filter[CommonFilters].
      filter[LoggingMDCFilter].
      filter[FinagleRequestScopeFilter].
      filter[UserFilter].
      add[MyController1].
      add[MyController2]
  }
}
```


Warmup
===============================
Finatra server provides a *warmup* method that's called before the server's external HTTP port is bound and the /health port responds with OK. Often classes or routes will be called to warmup the JVM before traffic is routed to the server.

```scala
class DoEverythingServer extends HttpServer {
  ...
  override def warmup() {
    injector.instance[MyWarmupHandler].warmup()
  }
}
```

```scala
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.routing.HttpWarmup
import com.twitter.finatra.httpclient.RequestBuilder._
import com.twitter.finatra.utils.Handler
import com.twitter.finatra.utils.ResponseUtils.expectOkResponse
import javax.inject.Inject

class MyWarmupHandler @Inject()(
  httpWarmup: HttpWarmup) {

  override def warmup() = {
    httpWarmup.send(
      request = get("/ok"),
      responseCallback = expectOkResponse(_, "ok"))
  }
}
```

Stats
===============================
A StatsReceiver can be injected anywhere in your application
```scala
class MyService @Inject()(
  stats: StatsReceiver)
```

To override the default binding for StatsReceiver see GuiceApp#statsModule


<a name="flags">Flags</a>
===============================

## @Flag
Flag values can be injected in classes (and provider methods), by using the @Flag annotation:
```scala
class MyService @Inject()(
  @Flag("key") key: String) {
}

class MyModule extends TwitterModule {
  @Provider
  @Singleton
  def providesFoo(@Flag("key") key: String) = {
    new Foo(key)
  }
}
```

## <a name="module-best-practices">Best Practices</a>
* If a flag is defined in a module, dereference that flag directly within that module (instead of using @Flag)
```scala
object MyModule1 extends TwitterModule {
  val key = flag("key", "defaultkey", "The key to use.")

  @Singleton
  @Provides
  def providesThirdPartyFoo: ThirdPartyFoo = {
    new ThirdPartyFoo(key())
  }
}
```

## <a name="setting-flags-from-code">Setting flags from code</a>
Some deployment environments may make it difficult to set command line flags. If this is the case, Finatra HTTP's core flags can be set from code.
For example, instead of setting the "maxRequestSize" flag, you can override the following method in your server.

```scala
class TweetsEndpointServer extends HttpServer {

  override val defaultMaxRequestSize = 10.megabytes

  override def configureHttp(router: HttpRouter) {
    ...
  }
}
```

Mustache
===============================
Mustache templates must be placed in `src/main/resources/templates`.

## @View Annotation
```scala
@Mustache("foo")
case class FooView(
  name: String)

get("/foo") { request: Request =>
  FooView("abc")
}

get("/foo") { request: Request =>
  response.notFound(
    FooView("abc"))
}
```

## Explicit reference to mustache file.
```scala
get("/foo") { request: Request =>
  response.ok.view(
    "foo.mustache",
    FooClass("abc"))
}
```

File Server
===============================
Finatra's file server support is meant for internal apps only. Do not use the fileserver for production apps requiring a robust high performance file serving solution.

By default, files are served from the classpath. You can use the flag `-doc.root` to customize the classpath root.

To serve files from the local file system, use the flag `-local.doc.root`. Note that setting Java System Property `-Denv=env` is no longer required nor supported. Setting the `-local.doc.root` flag will trigger the same `localFileMode` behavior. 

Also note that it is **an error** to attempt to set both the `-doc.root` and the `-local.doc.root` flags. Either do nothing to load resources from the base of the classpath, configure a classpath "namespace" by setting the `-doc.root` **or** load files from a local filesystem location specified by the `-local.doc.root` flag.

## File Serving Examples

```scala
get("/file") { request: Request =>
  response.ok.file("/file123.txt")
}

get("/:*") { request: Request =>
  response.ok.fileOrIndex(
    request.params("*"),
    "index.html")
}
```

Filters
===============================
Filters are code that runs before any request is dispatched to a particular Controller. They can modify the incoming request as well as the outbound response. A great example is our own LoggingFilter:

```scala
class DurationLoggingFilter
  extends SimpleFilter[FinagleRequest, FinagleResponse]
  with Logging {

  def apply(
    request: FinagleRequest,
    service: Service[FinagleRequest, FinagleResponse]
  ) = {
    val start = System.currentTimeMillis()
    service(request) map { response =>
      val end = System.currentTimeMillis()
      val duration = end - start
      info(request.method + " " + request.uri + " " + response.statusCode + " " + duration + " ms")
      response
    }
  }
}
```

You can register these inside the HttpServer like so:

```scala
class MyServer extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    router.
      filter[DurationLoggingFilter].
      add[MyController]
  }
}
```

Cookies
===============================
Cookies, like Headers, are read from request and set via render:

```scala
get("/") { request =>
  val loggedIn = request.cookie("loggedIn").getOrElse("false")
  response.ok.
    plain("logged in?:" + loggedIn)
}
```

```scala
get("/") { request =>
  response.ok.
    plain("hi").
    cookie("loggedIn", "true")
}
```

Advanced cookies are supported by creating and configuring Cookie objects:

```scala
get("/") { request =>
  val c = DefaultCookie("Biz", "Baz")
  c.setSecure(true)
  response.ok.
    plain("get:path").
    cookie(c)
}
```

See the Cookie class for more details.

File Uploads
===============================
See [MultiParamsTest](../http/src/test/scala/com/twitter/http/request/MultiParamsTest.scala).


Testing
===============================
## <a name="startup-tests">Startup Tests</a>
* Startup tests should mimic production as close as possible. As such:
    - avoid using `@Bind` and "override modules" in startup tests.
    - set the Guice `stage` to `PRODUCTION` so that all singletons will be eagerly created at startup (integration/feature tests run in `State.DEVELOPMENT` by default).
    - prevent Finagle clients from making outbound connections during startup tests by setting any resolverMap entries for your clients to `nil!`.

For example:
```scala
val server = EmbeddedHttpServer(
  stage = Stage.PRODUCTION,
  twitterServer = new SampleApiServer,
  extraArgs = Seq(
    "-com.twitter.server.resolverMap=client1=nil!,client2=nil!"))
```

## Integration Tests
See [TestInjector](../inject/inject-app/src/test/scala/com/twitter/inject/app/TestInjector.scala)

## Feature Tests
See [EmbeddedTwitterServer](../inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala)

<a name="logging">Logging</a>
===============================
Finatra uses the [slf4j](http://www.slf4j.org/manual.html) for framework logging.

## Basics

#### From the [slf4j](http://www.slf4j.org/manual.html) documentation:
>"The Simple Logging Facade for Java serves as a simple facade or abstraction for various logging frameworks, such as java.util.logging, Logback and log4j. SLF4J allows the end-user to plug in the desired logging framework at deployment time."

Note that slf4j is an interface that requires an actual logging implementation. If you are familiar with [Log4j](http://en.wikipedia.org/wiki/Log4j), this concept will be familiar as it separates the logging api interface from implementation allowing you to pick an appropriate implementation.

With that, when you are using slf4j you should ensure that you do not end-up with multiple implementations on your classpath, e.g., you should not have multiple slf4j bindings and/or a java.util.logging implementation, etc on your classpath as these are all competing implementations and classpath order is non-deterministic.

While there are several scala-wrappers for slf4j, Finatra uses and exposes some additional features on top of the excellent [grizzled-slf4j](http://software.clapper.org/grizzled-slf4j/) project.

The main logging utility is the [com.twitter.inject.Logging](../inject/inject-core/src/main/scala/com/twitter/inject/Logging.scala) trait which can be mixed into any object or class:
```scala
class MyClass extends Logging {
  def foo() = {
    info("Calculating...")
    "bar"
  }
}
```

## Logback
We highly recommend using [Logback](http://logback.qos.ch/) as an slf4j binding. If you choose to use Logback, include jar dependencies on `ch.qos.logback:logback-classic` and `com.twitter.finatra:finatra-slf4j` which will give a Logback slf4j implementation (`logback-classic`) and bridges from the 3 most popular jvm logging libraries (via `finatra-slf4j`):

* [log4j](http://logging.apache.org/log4j/1.2/)
* [commons-logging](http://commons.apache.org/proper/commons-logging/)
* [`java.util.logging`](https://docs.oracle.com/javase/7/docs/api/java/util/logging/package-summary.html): There is a performance penalty for intercepting jul log messages, so make sure to also include the [Slf4jBridgeModule](../slf4j/src/main/scala/com/twitter/logging/modules/Slf4jBridgeModule.scala) in your servers list of Guice modules, as this will install the [SLF4JBridgeHandler](http://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html) which mitigates most of the performance penalty. e.g.,

```scala
class Server extends HttpServer {
  override val modules = Seq(
    Slf4jBridgeModule,
    ...)

  ...
}
```

### Configuration
See [logback.xml](../examples/finatra-hello-world/src/main/resources/logback.xml) and [logback-test.xml](../examples/finatra-hello-world/src/test/resources/logback-test.xml) in [finatra-hello-world](../examples/finatra-hello-world) project.

### [MDC](http://logback.qos.ch/manual/mdc.html) Filters
Place the [LoggingMDCFilter](../logback/src/main/scala/com/twitter/logging/filter/LoggingMDCFilter.scala) filter before any other filters which will add entries or expect MDC entries to be present.


<a name="finatra-best-practices">Finatra Best Practices</a>
===============================
* Avoid `private[this]` unless you are in a hotspot identified during profiling.
* Avoid using custom flags for server locations in Finagle clients. Instead, use the Finagle provided `resolverMap`.

Utils
===============================

## <a name="conversions">Conversions</a>
Many Finatra utilities are provided as *conversions* which add methods to common Scala and Finagle classes. They can be found in the `com.twitter.finatra.conversions` package in [`com.twitter.finatra:finatra-utils`](../utils/src/main/scala/com/twitter/conversions). Currently, the best documentation are the [unit tests](../utils/src/test/scala/com/twitter/tests/conversions) showing their usage.

* [`Futures`](../utils/src/main/scala/com/twitter/conversions/future.scala)
* [`Options`](../utils/src/main/scala/com/twitter/conversions/option.scala)
* [`Booleans`](../utils/src/main/scala/com/twitter/conversions/boolean.scala)
* [`Seq`](../utils/src/main/scala/com/twitter/conversions/seq.scala)
* [`Time`](../utils/src/main/scala/com/twitter/conversions/time.scala)
* [`Strings`](../utils/src/main/scala/com/twitter/conversions/string.scala)
* [`Tuples`](../utils/src/main/scala/com/twitter/conversions/tuple.scala)
* [`Maps`](../utils/src/main/scala/com/twitter/conversions/map.scala)

## HTTP Client
A barebones httpclient built on `finagle-http` is included in the [`finatra-httpclient`](../httpclient) project. Stay tuned for further documentation and examples.

## App Integration
Finatra's HTTP server is built on top of several reusable traits.
One of these traits is [`com.twitter.inject.App`](../inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala) which provides the integration between Guice and `com.twitter.app.App`. `com.twitter.inject.App` can be used standalone to create command line apps which may also reuse your Guice modules defined in other libraries.

See [SampleGuiceApp](../inject/inject-app/src/test/scala/com/twitter/inject/app/tests/SampleGuiceApp.scala) and [SampleAppIntegrationTest](../inject/inject-app/src/test/scala/com/twitter/inject/app/tests/SampleAppIntegrationTest.scala)

Service Samples
===============================
* [finatra-hello-world](../examples/finatra-hello-world)

<a name="migration">Version 1 Migration Guide</a>
===============================
## Controllers
You no longer need to return a `Future` from controller routes (however, always return a `Future` if you already have one).

###Add Request type to controller callbacks
```scala
//v1
get("/foo") { request =>
get("/foo") { _ =>

//v2
import com.twitter.finagle.http.Request
get("/foo") { request: Request =>
```

Change "render" to "response" and specify the HTTP status as the first method after *response* (e.g. ok, created, notFound, etc)
```scala
//v1
render.json(ret)

//v2
response.ok.json(ret)
```

Route params are now stored in request.params (which allows us to reuse `finagle.http.Request` without defining our own).
```scala
//v1
request.routeParams("q")

//v2
request.params("q")
```

## Logging
To continue using "Java Util Logging", add a jar dependency on [`slf4j-jdk14`](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.slf4j%22%20AND%20a%3A%22slf4j-jdk14%22). Otherwise, we recommend using [Logback][logback] by adding jar dependencies on `ch.qos.logback:logback-classic` and `com.twitter.finatra:finatra-slf4j`.

```scala
//v1
log.info("hello")

//v2
info("hello")
```

## Exception Mappers
ExceptionMappers map exceptions to responses. It needs to implement the following trait:
```scala
trait ExceptionMapper[T <: Throwable] {
  def toResponse(request: Request, throwable: T): Response
}
```
which says it will handle `T`-typed exceptions. The request that triggered the exception is also provided as an argument. You can make use of exception mapping by adding the [`ExceptionMappingFilter`](../http/src/main/scala/com/twitter/http/filters/ExceptionMappingFilter.scala) to your `com.twitter.finatra.routing.HttpRouter`, e.g.,
```scala
router.filter[ExceptionMappingFilter[Request]]
```

The `ExceptionMappingFilter` takes an [`ExceptionManager`](../http/src/main/scala/com/twitter/http/internal/exceptions/ExceptionManager.scala) which is provided by default in the [`ExceptionMapperModule`](../http/src/main/scala/com/twitter/http/modules/ExceptionMapperModule.scala). You can override the default module if necessary by overriding the value in the [`HttpServer`](../http/src/main/scala/com/twitter/http/HttpServer.scala).

To replicate v1 functionality around `notFound` and `error`, you could do the following:
```scala
//v1
notFound { request => ... }
error { request => ... }

//v2 notFound: Create a filter and add it before your controller:
@Singleton
class NotFoundFilter @Inject()(
 response: ResponseBuilder)
 extends SimpleFilter[Request, Response] {

 def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
   service(request) map { origResponse =>
     if (origResponse.status == Status.NotFound)
       response.notFound("bar")
     else
       origResponse
   }
 }
}

//v2 error: Write an exception mapper and register it with HttpRouter
@Singleton
class ArithmeticExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[ArithmeticException] {

  override def toResponse(request: Request, e: ArithmeticException): Response = {
    response.internalServerError("whoops, divide by zero!")
  }
}

router.
  filter[NotFoundFilter].
  filter[ExceptionMappingFilter[Request]].
  exceptionMapper[ArithmeticExceptionMapper]
```

## App
Override the configure method and add your controllers there.

Note: Flag parsing that used to be in App's constructor should be moved into the `configureHttp` method.

```scala
//v2
class Server extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    val controller1 = new Controller1(...)
    val controller2 = new Controller2(...)
    router.
      commonFilter[CommonFilters].
      commonFilter[NotFoundFilter]. // if needed (see above section on Error Handling)
      add(controller1).
      add(controller2)
  }
}
```

## Static Files
* Web resources (html/js) go in `src/main/webapp`
* Mustache templates now go in `src/main/resources/templates`

To serve static files, you now need explicit routes:
```scala
get("/:*") { request: Request =>
 response.ok.file(
   request.params("*"))
}
```

If you have an "index" page (e.g. index.html) add the following route.
```scala
get("/:*") { request: Request =>
  response.ok.fileOrIndex(
    filePath = request.params("*"),
    indexPath = "index.html")
```

## Command Line Flags
Global flags are no longer used for standard server configuration. Instead:
```
//v2
-log.output=twitter-server.log
-http.port=:8080
-admin.port=:8081
```

## Testing
- In v1, `SpecHelper` and `MockApp` are used.
- In v2, we provide a common way to run blackbox and whitebox integration tests against a locally running server:
	* [Simple Example](../examples/finatra-hello-world/src/test/scala/com/twitter/hello/HelloWorldFeatureTest.scala)
	* More Powerful Example [TODO]

## Unsupported v1 Features
* Render a route from another route.
* Controller *notFound* and *error* handler methods.




[twitter-server]: https://github.com/twitter/twitter-server
[finagle]: https://github.com/twitter/finagle
[util-app]: https://github.com/twitter/util/tree/master/util-app
[util-core]: https://github.com/twitter/util/tree/master/util-core
[guice]: https://github.com/google/guice
[jackson]: https://github.com/FasterXML/jackson
[logback]: http://logback.qos.ch/
[slf4j]: http://www.slf4j.org/manual.html
[grizzled-slf4j]: http://software.clapper.org/grizzled-slf4j/
[local]: https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/util/Local.scala
[mdc]: http://logback.qos.ch/manual/mdc.html
[controller]: ../http/src/main/scala/com/twitter/http/Controller.scala
[HttpServer]: ../http/src/main/scala/com/twitter/http/HttpServer.scala
[quick-start]: ../README.md#quick-start
[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter.finatra%22
[sinatra]: http://www.sinatrarb.com/
[finatra-jackson-module]: ../jackson/src/main/scala/com/twitter/json/modules/FinatraJacksonModule.scala
[finatra-jackson]: ../jackson
[CallbackConverterModule]: ../http/src/main/scala/com/twitter/http/modules/CallbackConverterModule.scala
