---
layout: user_guide
title: "Add an ExceptionMapper"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li><a href="/finatra/user-guide/build-new-http-server">Building a New HTTP Server</a></li>
  <li class="active">Add an ExceptionMapper</li>
</ol>

## Basics
===============================

It is recommended that you use exceptions for flow control in your controller and services and rely on the [`com.twitter.finatra.http.exceptions.ExceptionMapper`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/exceptions/ExceptionMapper.scala) to convert exceptions into proper HTTP responses. Finatra provides a default [FinatraDefaultExceptionMapper](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/FinatraDefaultExceptionMapper.scala) which provides high-level mapping for exceptions. However, you are free to register additional mappers or to [override the default mapper](#override-defaults) altogether.

For instance, if you wanted to map a `java.net.MalformedURLException` to a `400 - BadRequest` response you could create the following ExceptionMapper:

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

Then register this exception mapper in your server. **Note**: using ExceptionMappers also require you to have the [`ExceptionMappingFilter`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/ExceptionMappingFilter.scala) defined in your filter chain.

```scala
import DoEverythingModule
import ExampleController
import ExampleFilter
import MalformedURLExceptionMapper
import com.twitter.finatra.http.filters.{AccessLoggingFilter, ExceptionMappingFilter}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter].
      filter[ExceptionMappingFilter].
      add[ExampleFilter, ExampleController].
      exceptionMapper[MalformedURLExceptionMapper]
  }
}
```
<div></div>

Again, you can see we register the exception mapper *by type* allowing the framework to instantiate an instance.

## <a class="anchor" name="override-defaults">Override Default Behavior</a>
===============================

If you want to override the [`FinatraDefaultExceptionMapper`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/FinatraDefaultExceptionMapper.scala) you can override the `exceptionMapperModule` in your [`HttpServer`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala#L67) to provide a custom implementation, e.g.,

```scala
...
class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[AccessLoggingFilter].
      filter[ExceptionMappingFilter].
      add[ExampleFilter, ExampleController].
      exceptionMapper[MalformedURLExceptionMapper]
  }

  override def exceptionMapperModule = MyExceptionMapperModule
}
```
<div></div>

Next section: [Implement a Server "Warmup" Handler](/finatra/user-guide/build-new-http-server/warmup.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-http-server/filter.html"><span aria-hidden="true">&larr;</span>&nbsp;Add&nbsp;Filters</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-http-server/warmup.html">Implement&nbsp;a&nbsp;Server&nbsp;"Warmup"&nbsp;Handler&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
