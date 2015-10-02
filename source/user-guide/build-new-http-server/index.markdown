---
layout: user_guide
title: "Building a New HTTP Server"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Building a New HTTP Server</li>
</ol>

Note: the most up-to-date examples are located in the [`finatra/examples`](https://github.com/twitter/finatra/tree/master/examples) project.

## <a name="server-definition" href="#server-definition">Create a Server definition</a>
===============================

To start, add a dependency on the `com.twitter.finatra:finatra-http_{scala-version}` library. We also highly recommend depending on `com.twitter.finatra:finatra-slf4j` and `ch.qos.logback:logback-classic` to choose [Logback](http://logback.qos.ch/) as your [SLF4J](http://www.slf4j.org/manual.html) implementation. For more information on logging with Finatra see: [Logging](/finatra/user-guide/logging).

Create a new class that extends [`com.twitter.finatra.http.HttpServer`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala):

```scala
import DoEverythingModule
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.HttpServer

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
  }
}
```
<div></div>

Simplistically, a server can be thought of as a collection of [modules](/finatra/user-guide/getting-started#modules) along with the ways controllers are routed to and filtered. The Finatra convention is to create a Scala [*object*](https://twitter.github.io/scala_school/basics2.html#object) with a name ending in "Main". This allows your server to be instantiated multiple times in tests without worrying about static state persisting across test runs in the same JVM. `ExampleServerMain` is then a static object which contains the runnable *main method* for the server.

### TwitterServer HTTP Admin Interface

All [TwitterServer](https://github.com/twitter/twitter-server) based services start an [HTTP Admin Interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) bound to a port configurable via the `-admin.port` flag. If you want to serve an external interface this will be bound to a separate port configurable via the `-http.port` flag.

Some deployment environments such as [Heroku](https://www.heroku.com/), [AppFog](https://www.appfog.com/), and [OpenShift](https://www.openshift.com) only allow a single port to be used when deploying an application. In these cases, you can disable the [HTTP Admin Interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) started by [TwitterServer](https://github.com/twitter/twitter-server) as such:
```scala
class ExampleServer extends HttpServer {
  override val disableAdminHttpServer = true
  ...
}
```
Since the admin port is currently required by [TwitterServer](https://github.com/twitter/twitter-server) you will need to set the `-admin.port` and `-http.port` flags to the same value in addition to specifying ```override val disableAdminHttpServer = true``` above.

For more information, see the [Heroku](https://www.heroku.com/) [hello-world example](https://github.com/twitter/finatra/tree/master/examples/hello-world-heroku).

## <a name="add-controller" href="#add-controller">Add a Controller</a>
===============================

Suppose you want to add the following controller to your server:

```scala
import ExampleService
import com.twitter.finagle.httpx.Request
import com.twitter.finatra.http.Controller
import javax.inject.Inject

class ExampleController @Inject()(
  exampleService: ExampleService
) extends Controller {

  get("/ping") { request: Request =>
    "pong"
  }

  get("/name") { request: Request =>
    response.ok.body("Bob")
  }

  post("/foo") { request: Request =>
    exampleService.do(request)
    "bar"
  }
}
```
<div></div>

The server now looks like this:

```scala
import DoEverythingModule
import ExampleController
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      add[ExampleController]
  }
}
```
<div></div>

Here we are adding *by type* allowing the framework to handle class instantiation.

### <a name="controllers-and-routing" href="#controllers-and-routing">Controllers and Routing</a>:

Routes are defined in a [Sinatra](http://www.sinatrarb.com/)-style syntax which consists of an HTTP method, a URL matching pattern and an associated callback function that takes a generic `RequestType` and returns a generic `ResponseType`. See the [`com.twitter.finatra.http.RouteDSL`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/RouteDSL.scala).

When Finatra receives an HTTP request, it will scan all registered controllers (in the order they are added) and dispatch the request to the first matching route starting from the top of each controller invoking the route's associated callback function.

It is recommended to follow [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) conventions if possible, e.g., when deciding which routes to group into a particular controller, group routes related to a single resource into one controller. The per-route stating in the [`com.twitter.finatra.http.filters.StatsFilter`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/StatsFilter.scala) works best when this convention is followed.

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
<div></div>

### Route Matching Patterns:

#### Named Parameters

Route patterns may include named parameters:
```scala
get("/users/:id") { request: Request =>
  "You looked up " + request.params("id")
}
```
<div></div>

Note: *Query params and route params are both stored in the "params" field of the request.* If a route parameter and a query parameter have the same name, the route parameter always wins. Therefore, you should ensure your route parameter names do not collide with any query parameter name that you plan to read.

#### Wildcard Parameter

Routes can also contain the wildcard pattern. The wildcard can only appear once at the end of a pattern and it will capture all text in its place. For example,
```scala
get("/files/:*") { request: Request =>
  request.params("*")
}
```
<div></div>
For a `GET` of `/files/abc/123/foo.txt` the endpoint will return `abc/123/foo.txt`

#### Admin Paths

Any path starting with `/admin/finatra/` will be exposed only on the server's admin port. All [TwitterServer](http://twitter.github.io/twitter-server/) based servers have an [HTTP admin interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) for exposing internal endpoints such as stats. The admin endpoint should not be exposed outside your DMZ.

```scala
get("/admin/finatra/users/") { request: Request =>
  userDatabase.getAllUsers(
    request.params("cursor"))
}
```
<div></div>

#### Regular Expressions

Regular expressions are no longer allowed in string defined paths. Note: We are planning to support regular expression based paths in a future release.

### Requests

Each route has a callback which is executed when the route matches a request. Callbacks require explicit input types and Finatra will then try to convert the incoming request into the specified input type. Finatra supports two request types: a Finagle `httpx` Request or a custom `case class` Request.

#### Finagle `httpx` Request:
This is a [com.twitter.finagle.httpx.Request](https://twitter.github.io/finagle/docs/index.html#com.twitter.finagle.httpx.Request) which contains common HTTP attributes.

#### Custom `case class` Request
Custom requests allow declarative request parsing with support for type conversions, default values, and validations.

For example suppose you wanted to parse a `GET` request with three query params: `max`, `startDate`, and `verbose`, e.g.,

```text
http://foo.com/users?max=10&start_date=2014-05-30TZ&verbose=true
```
<div></div>

This can be parsed with the following `case class`:
```scala
case class UsersRequest(
  @Max(100) @QueryParam max: Int,
  @PastDate @QueryParam startDate: Option[DateTime],
  @QueryParam verbose: Boolean = false)
```
<div></div>

The custom `UsersRequest` can then be used as the callback's input type:
```scala
get("/users") { request: UsersRequest =>
  request
}
```
<div></div>

The `case class` field names should match the request parameters or use the [@JsonProperty](https://github.com/FasterXML/jackson-annotations#annotations-for-renaming-properties) annotation to specify the JSON field name in the case class (see: [example](https://github.com/twitter/finatra/blob/master/jackson/src/test/scala/com/twitter/finatra/tests/json/internal/ExampleCaseClasses.scala#L141)). A [PropertyNamingStrategy](http://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/PropertyNamingStrategy.html) can be configured to handle common name substitutions (e.g. snake_case or camelCase). By default, snake_case is used (defaults are set in [`FinatraJacksonModule`](https://github.com/twitter/finatra/tree/master/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala)).

Use a Scala ["back-quote" literal](http://www.scala-lang.org/files/archive/spec/2.11/01-lexical-syntax.html) for the field name when special characters are involved (e.g. @Header \`user-agent\` : String).

Non-optional fields without default values are considered required. If a required field is missing, a `CaseClassMappingException` is thrown.

The following field annotations specify where to parse a field out of the request

  * Request Fields:
     * [`@RouteParam`](https://github.com/twitter/finatra/blob/master/http/src/test/scala/com/twitter/finatra/http/integration/doeverything/main/domain/IdAndNameRequest.scala)
     * [`@QueryParam`](https://github.com/twitter/finatra/blob/master/http/src/test/scala/com/twitter/finatra/http/integration/doeverything/main/domain/RequestWithQueryParamSeqString.scala) (*ensure that @RouteParam names do not collide with @QueryParam names. Otherwise, an @QueryParam could end up parsing an @RouteParam*)
     * [`@FormParam`](https://github.com/twitter/finatra/blob/master/http/src/test/scala/com/twitter/finatra/http/integration/doeverything/main/domain/FormPostRequest.scala)
     * `@Header`
 * Other:
     * `@RequestInject`: Injects the Finagle `httpx` Request or any Guice managed class into your case class

*Note: HTTP requests with a content-type of `application/json` will always have their body. This behavior can be disabled by annotating the `case class` with `@JsonIgnoreBody` leaving the raw request body accessible through `@RequestInject`.*

For more specifics on how JSON parsing integrates with routing see: [Integration with Routing](/finatra/user-guide/json#routing-json) in the [JSON](/finatra/user-guide/json) documentation.

### Responses

#### <a name="future-conversion" href="#future-conversion">Future Conversion</a>:

For the basics of Futures in Finatra, see: [Futures](/finatra/user-guide/getting-started#futures) in the [Getting Started](/finatra/user-guide/getting-started) documentation.

Finatra will convert your route callbacks return type into a `com.twitter.util.Future[Response]` using the following rules:

* If you return a `com.twitter.util.Future[Response]`, then no conversion will be performed.
* A non `com.twitter.util.Future` return value will be converted into a `com.twitter.util.Future` using a Finatra provided `FuturePool`.
* `Some[T]` will be converted into a HTTP 200 OK.
* `None` will be converted into a HTTP 404 NotFound.
* Non-response classes will be converted into a HTTP 200 OK.

Callbacks that do not return a [`com.twitter.util.Future`](https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala) will have their return values wrapped in a [`com.twitter.util.ConstFuture`](https://twitter.github.io/util/docs/index.html#com.twitter.util.ConstFuture). If your non-future result calls a blocking method, you must [avoid blocking the Finagle request](https://twitter.github.io/scala_school/finagle.html#DontBlock) by wrapping your blocking operation in a FuturePool e.g.

```scala
import com.twitter.finatra.utils.FuturePools

class MyController extends Controller {

  private val futurePool = FuturePools.unboundedPool("CallbackConverter")

  get("/") { request: Request =>
    futurePool {
      blockingCall()
    }
  }
}
```
<div></div>

#### Response Builder:
All Controllers have a protected `response` field they can use to build responses. For example:

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
<div></div>

#### Response Exceptions:
Responses can be embedded inside exceptions with `.toException`. You can throw the exception to terminate control flow, or wrap it inside a `Future.exception` to return a failed `Future`. However, instead of directly returning error responses in this manner, a better convention is to handle application-specific exceptions in an [`ExceptionMapper`](#exception-mapper).

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
<div></div>

#### Setting the Response Location Header:
ResponseBuilder has a "location" method.
```scala
post("/users") { request: Request =>
  response.created.
    location("http").
    html(
      TestUserView(request.age, request.name))
}
```
<div></div>

which can be used:

   * if the URI starts with "http" or "/" then the URI is placed in the Location header unchanged.
   * `response.location("123")` will get turned into the correct full URL in the [HttpResponseFilter](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/HttpResponseFilter.scala) (e.g. `http://host.com/users/123`)

Or to obtain the request full path URL as follows:

```scala
RequestUtils.pathUrl(request)
```
<div></div>

## <a name="add-filters" href="#add-filters">Add Filters</a>
===============================

If you want to apply a filter (or filters) to all added controllers you can do the following:

```scala
import DoEverythingModule
import ExampleController
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter].
      add[ExampleController]
  }
}
```
<div></div>

It is also possible to add a filter per controller,

```scala
import DoEverythingModule
import ExampleController
import ExampleFilter
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter].
      add[ExampleFilter, ExampleController]
  }
}
```
<div></div>

In both cases, we are again applying the filter *by type* allowing the framework to instantiate instances of the filters.

Finatra composes some commonly used filters into [`com.twitter.finatra.http.filters.CommonFilters`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala). `CommonFilters` can be added in the same manner as any other filter, e.g.,

```scala
override configureHttp(router: HttpRouter) {
  router.
    filter[CommonFilters].
    filter[ExampleFilter].
    add[MyController1].
    add[MyController2]
}
```

## <a name="exception-mapper" href="#exception-mapper">Add an ExceptionMapper</a>
===============================

It is recommended that in your controller and services that you use exceptions for flow control and rely on the [`com.twitter.finatra.http.exceptions.ExceptionMapper`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/exceptions/ExceptionMapper.scala) to convert exceptions into proper HTTP responses. Finatra provides a [default ExceptionMapper](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/FinatraDefaultExceptionMapper.scala) which provides high-level mapping for exceptions. However, you are free to register additional mappers (or to override the default mapper altogether).

For instance, if you wanted to map `java.net.MalformedURLException` to a `400 - BadRequest` response,

```scala
@Singleton
class MalformedURLExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[MalformedURLException] {

  override def toResponse(request: Request, exception: MalformedURLException): Response = {
    response.badRequest(s"Malformed URL - ${exception.getMessage}")
  }
}
```
<div></div>

You could then register this exception mapper in your server:

```scala
import DoEverythingModule
import ExampleController
import ExampleFilter
import MalformedURLExceptionMapper
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter].
      add[ExampleFilter, ExampleController].
      exceptionMapper[MalformedURLExceptionMapper]
  }
}
```
<div></div>

Again, you can see we register the exception mapper *by type* allowing the framework to instantiate the instance.


## <a name="server-warmup" href="#server-warmup">Implement a Server "Warmup" Handler</a>
===============================

There may be occasions where we want to exercise a code path before accepting traffic to the server. In this case you can implement a [`com.twitter.finatra.utils.Handler`](https://github.com/twitter/finatra/blob/master/utils/src/main/scala/com/twitter/finatra/utils/Handler.scala). Your handler should be constructed with an [`com.twitter.finatra.http.routing.HttpWarmup`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/routing/HttpWarmup.scala) instance.

```scala
import com.twitter.finatra.http.routing.HttpWarmup
import com.twitter.finatra.httpclient.RequestBuilder._
import com.twitter.finatra.utils.Handler
import javax.inject.Inject

class ExampleWarmupHandler @Inject()(
  httpWarmup: HttpWarmup)
  extends Handler {

  override def handle() = {
    httpWarmup.send(
      get("/ping"))
  }
}
```
<div></div>

The handle above simply tries to hit the `/ping` endpoint of the server.

You can the register the handler with your server:

```scala
import DoEverythingModule
import ExampleController
import ExampleFilter
import ExampleWarmupHandler
import MalformedURLExceptionMapper
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter].
      add[ExampleFilter, ExampleController].
      exceptionMapper[MalformedURLExceptionMapper]
  }

  override def warmup() {
    run[ExampleWarmupHandler]()
  }
}
```
<div></div>

Once again, the warmup handler is added *by type* allowing the framework to construct the instance.

The [`com.twitter.inject.app.App#warmup`](https://github.com/twitter/finatra/blob/master/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L119) method is called before the server's external HTTP port is bound and thus before the TwitterServer [Lifecycle Management](http://twitter.github.io/twitter-server/Features.html#lifecycle-management) `/health` endpoint responds with `OK`.


## <a name="more-information" href="#more-information">More information</a>
===============================

For more information, we encourage you to take a look at the full [`finatra/examples`](https://github.com/twitter/finatra/tree/master/examples) in the [github](https://github.com/twitter/finatra) source.

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/getting-started"><span aria-hidden="true">&larr;</span>&nbsp;Getting&nbsp;Started</a></li>
    <li class="next"><a href="/finatra/user-guide/json">Working&nbsp;with&nbsp;JSON&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
