---
layout: page
title: "Building a new HTTP Server"
comments: false
sharing: false
footer: true
---

Note: the most up-to-date examples are located in the [`finatra/examples`](https://github.com/twitter/finatra/tree/master/examples) project.

### Create a Server definition
===============================

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

In the code above, we are saying that our `ExampleServer` is a [`com.twitter.finatra.http.HttpServer`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala). Simplistically, a server can be thought of as a collection of [modules](/finatra/user-guide/getting-started#modules) along with the ways controllers are routed to and filtered. The Finatra convention is to create a Scala object with a name ending in "Main". This allows your server to be instantiated multiple times in tests without worrying about static state persisting across test runs in the same JVM. `ExampleServerMain` is then the static object which contains the runnable *main method*.


### Add a Controller
===============================

We now want to add the following controller to our server:

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

Our server now looks like this:

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

#### <a name="controllers-and-routing" href="#controllers-and-routing">Controllers and Routing</a>:

When Finatra receives an HTTP request, it will scan all registered controllers (in the order they are added) and dispatch the request to the first matching route starting from the top of each controller.

It is recommended to follow [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) conventions if possible. When deciding which routes to group into a particular controller, group routes related to a single resource into one controller.

#### Route Matching Patterns:

##### Named Parameters

Route patterns may include named parameters:
```scala
get("/users/:id") { request: Request =>
  "You looked up " + request.params("id")
}
```
<div></div>

Note: *Query params and path params are both stored in the `params` field of the request.* If a path parameter and a query parameter have the same name, the path parameter always wins. Therefore, you should ensure your path parameter names do not collide with a query parameter name that you plan to read.

##### Wildcard Parameter

Routes can also contain the wildcard pattern. The wildcard can only appear once at the end of a pattern and it will capture all text in its place. For example,
```scala
get("/files/:*") { request: Request =>
  request.params("*")
}
```
<div></div>
For a `GET` of `/files/abc/123/foo.txt` the endpoint will return `abc/123/foo.txt`

##### Admin Paths

Any path starting with `/admin/finatra/` will be exposed only on the server's admin port. All [TwitterServer](http://twitter.github.io/twitter-server/) based servers have an [HTTP admin interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) for exposing internal endpoints such as stats. The admin endpoint should not be exposed outside your DMZ.

```scala
get("/admin/finatra/users/") { request: Request =>
  userDatabase.getAllUsers(
    request.params("cursor"))
}
```
<div></div>

##### Regular Expressions

Regular expressions are no longer allowed in string defined paths. Note: We are planning to support regular expression based paths in a future release.

#### Requests

Each route has a callback which is executed when the route matches a request. Callbacks require explicit input types and Finatra will then try to convert the incoming request into the specified input type. Finatra supports two request types:

- A Finagle `httpx` Request
- A custom `case class` Request

##### Finagle `httpx` Request:
This is a [com.twitter.finagle.httpx.Request](https://twitter.github.io/finagle/docs/index.html#com.twitter.finagle.httpx.Request) which contains common HTTP attributes.

##### Custom `case class` Request
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

The custom `UsersRequest` is then used as the callback's input type:
```scala
get("/users") { request: UsersRequest =>
  request
}
```
<div></div>

Notes:

* The `case class` field names should match the request parameters.
  * A [PropertyNamingStrategy](http://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/PropertyNamingStrategy.html) can be configured to handle common name substitutions (e.g. snake_case or camelCase). By default, snake_case is used (defaults are set in [`FinatraJacksonModule`](https://github.com/twitter/finatra/tree/master/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala)).
  * Use backticks when special characters are involved (e.g. @Header \`user-agent\` : String)  
  * [@JsonProperty](https://github.com/FasterXML/jackson-annotations#annotations-for-renaming-properties) can also be used to specify the JSON field name  
* Non-optional fields without default values are required. If required fields are missing, a `CaseClassMappingException` is thrown.
* The following field annotations specify where to parse the field out of the request
  * Request Fields -
     * [`@RouteParam`](https://github.com/twitter/finatra/blob/master/http/src/test/scala/com/twitter/finatra/http/integration/doeverything/main/domain/IdAndNameRequest.scala)
     * [`@QueryParam`](https://github.com/twitter/finatra/blob/master/http/src/test/scala/com/twitter/finatra/http/integration/doeverything/main/domain/RequestWithQueryParamSeqString.scala) (*Ensure that @RouteParam names do not collide with @QueryParam names. Otherwise, an @QueryParam could end up parsing an @RouteParam*)
     * [`@FormParam`](https://github.com/twitter/finatra/blob/master/http/src/test/scala/com/twitter/finatra/http/integration/doeverything/main/domain/FormPostRequest.scala)
     * `@Header`
 * Other
     * `@RequestInject`: Injects the Finagle Httpx Request or any Guice managed class into your case class

*Note: HTTP requests with a content-type of application/json are similarly parsed (but "Request Field" annotations are ignored). See the [JSON](/finatra/user-guide/json) section.*

#### Responses

Finatra will convert your route callbacks return type into a `Future[Response]` using the following rules:

* If you return a `Future[Response]`, then no conversion will be performed
* A non `Future` return value will be converted into a `Future` using a Finatra provided `FuturePool` (see [Future Conversion](#future-conversion) section for more details)
* `Some[T]` will be converted into a HTTP 200 OK
* `None` will be converted into a HTTP 404 NotFound
* Non-response classes will be converted into a HTTP 200 OK

##### Response Builder:
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
<div></div>

##### Response Exceptions:
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

##### Setting Response Location Header:
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

##### <a name="future-conversion" href="#future-conversion">Future Conversion</a>:
Callbacks that do not return a Future will have their return values wrapped in a [ConstFuture](https://twitter.github.io/util/docs/index.html#com.twitter.util.ConstFuture). If your non-future result calls a blocking method, you must [avoid blocking the Finagle request](https://twitter.github.io/scala_school/finagle.html#DontBlock) by wrapping your blocking operation in a FuturePool e.g.

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

### Add Filters
===============================

If we want to apply a filter (or filters) to all added controllers we could do the following:

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

### <a name="exception-mapper" href="#exception-mapper">Add an ExceptionMapper</a>
===============================

It is recommended that in your controller and services that you use exceptions for flow control and rely on the [`com.twitter.finatra.http.exceptions.ExceptionMapper`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/exceptions/ExceptionMapper.scala) to convert exceptions into proper HTTP responses. Finatra provides a [default ExceptionMapper](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/FinatraDefaultExceptionMapper.scala) which provides high-level mapping for exceptions. However, you are free to register additional mappers (or to override the default mapper altogether).

For instance, if we wanted to map `java.net.MalformedURLException` to a `400 - BadRequest` response,

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

We could then register this exception mapper in our server:

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


### Implement a server "warmup" handler
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

Our handle above simply tries to hit the `/ping` endpoint of the server.

We can the register the handler with our server:

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


### More information
===============================

For more information, we encourage you to take a look at the [`finatra/examples`](https://github.com/twitter/finatra/tree/master/examples) in the [github](https://github.com/twitter/finatra) source.

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/getting-started"><span aria-hidden="true">&larr;</span>&nbsp;Getting&nbsp;Started</a></li>
    <li class="next"><a href="/finatra/user-guide/logging">Logging&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
