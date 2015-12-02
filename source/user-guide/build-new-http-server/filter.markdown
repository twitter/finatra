---
layout: user_guide
title: "Add Filters"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li><a href="/finatra/user-guide/build-new-http-server">Building a New HTTP Server</a></li>
  <li class="active">Add Filters</li>
</ol>

## Basics
===============================

If you want to apply a filter (or filters) to all added controllers you can do the following:

```scala
import DoEverythingModule
import ExampleController
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter[Request]].
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
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter[Request]].
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
<div></div>

## <a class="anchor" name="request-scope" href="#request-scope">Request Scope</a>
===============================

Guice supports [custom scopes](https://github.com/google/guice/wiki/CustomScopes) in addition to the most common [Singleton and *Unscoped*](https://github.com/google/guice/wiki/Scopes). Request scopes are often used to allow injecting classes that change depending on the incoming request (e.g. the authenticated User). Finatra provides an implementation of *RequestScoped* that works across Finagle non-blocking threads (Guice's included `@RequestScoped` implementation uses ThreadLocals which will not work with [twitter/util](https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala) `com.twitter.util.Future`).

Note: Fields added to the Finagle request scope will remain present in threads launched from a Finagle [FuturePool](https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/util/FuturePool.scala).

### Adding Classes into the Finatra Request Scope

First add a dependency on `com.twitter:inject-request-scope`

Then define a module,

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
<div></div>

Next define a Filter to seed the `User` into the Finatra Request Scope:
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
<div></div>

Next add the [`FinagleRequestScopeFilter`](https://github.com/twitter/finatra/tree/master/inject/inject-request-scope/src/main/scala/com/twitter/inject/requestscope/FinagleRequestScopeFilter.scala) filter to your server before the `UserFilter` (shown below w/ other common filters in a recommended filter order):

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

Then inject a `User` or a `Provider[User]` wherever you need to access the request scope user. Note, `Provider[User]` must be used when injecting into a Singleton class.

```scala
import javax.inject.Provider

@Singleton
class MyController @Inject()(
  dao: GroupsDAO,
  user: Provider[User])
  extends Controller {

  get("/") { request: Request =>
    "The incoming user has id " + user.get.id
  }
}
```
<div></div>

### <a class="anchor" name="request-ctx" href="#request-ctx">Using `com.twitter.finagle.http.Request#ctx`</a>

Above we saw how to seed classes to the Finatra Request scope using a `javax.inject.Provider`. However, we recommend *not* seeding with a request-scope `Provider[T]` but instead using Finagle's [`com.twitter.finagle.http.Request#ctx`](https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Request.scala#L33). Internally, we generally use the `Request#ctx` over `Provider[T]` even though we use Guice extensively.

To use the `Request#ctx` technique,

```scala
// domain object
case class User(id: Long)

// create a filter
class UserFilter extends SimpleFilter[Request, Response] {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    UserContext.setUser(request)
    service(request)
  }
}

// create a context
object UserContext {
  private val UserField = Request.Schema.newField[User]()

  implicit class UserContextSyntax(val request: Request) extends AnyVal {
    def user: User = request.ctx(UserField)
  }

  private[twitter] def setUser(request: Request): Unit = {
    val user = User(1) //Parse user from request headers/cookies/etc.
    request.ctx.update(UserField, user)
  }
}
```
<div></div>

Then to use:

```scala
// import the UserContext into scope, the method Request#user
// will now be available on the Request object.
import UserContext._

class MyController() extends Controller {
  get("/") { request: Request =>
    "Hi " + request.user.id
  }
}
```
<div></div>

Next section: [Add an ExceptionMapper](/finatra/user-guide/build-new-http-server/exceptions.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-http-server/controller.html"><span aria-hidden="true">&larr;</span>&nbsp;Add&nbsp;a&nbsp;Controller</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-http-server/exceptions.html">Add&nbsp;an&nbsp;ExceptionMapper&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
