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

It is recommended that you use exceptions for flow control in your controller and services and rely on the [`c.t.finatra.http.exceptions.ExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/exceptions/ExceptionMapper.scala) to convert exceptions into proper HTTP responses. The Finatra framework will add a set of mappers by default which provide high-level mapping for exceptions. However, you are free to register additional mappers or to [override the default mappers](#override-defaults) altogether.

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

Then register this exception mapper in your server. **Note**: using ExceptionMappers also require you to have the [`c.t.finatra.http.filters.ExceptionMappingFilter`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/ExceptionMappingFilter.scala) defined in your filter chain. The `ExceptionMappingFilter` is part of [`c.t.finatra.http.filters.CommonFilters`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala)

```scala
import DoEverythingModule
import ExampleController
import ExampleFilter
import MalformedURLExceptionMapper
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{ExceptionMappingFilter, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[ExceptionMappingFilter[Request]]
      .add[ExampleFilter, ExampleController]
      .exceptionMapper[MalformedURLExceptionMapper]
  }
}
```
<div></div>

Again, you can see we register the exception mapper *by type* allowing the framework to instantiate an instance. Also note that the [`c.t.finatra.http.filters.ExceptionMappingFilter`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/ExceptionMappingFilter.scala) is included in the [`MergedFilter`](https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/filters/MergedFilter.scala) [`c.t.finatra.http.filter.CommonFilters`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala).

## <a class="anchor" name="override-defaults" href="#override-defaults">Override Default Behavior</a>
===============================

The [`ExceptionManager`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/exceptions/ExceptionManager.scala) is the class that handles registration of exception mappers. In the example above, the [`HttpRouter#exceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala#L42) method is simply registering the given mapper with the `ExceptionManager`. The `ExceptionManager` is configured by the inclusion of the [`ExceptionManagerModule`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/modules/ExceptionManagerModule.scala) as a framework module in every [`HttpServer`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala#L23).

If a new mapper is added over an exception type already registered in the `ExceptionManager`, the previous mapper will be overwritten. Thus, the last registered mapper for an exception type wins.

The framework adds several mappers to the `ExceptionManager` by default. If a user wants to swap out any of these defaults they simply need add their own mapper to the manager for the exception type to map. E.g., by default the framework will add the follow mappers:
  
- `Throwable`                       -> [`c.t.finatra.http.internal.exceptions.ThrowableExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/ThrowableExceptionMapper.scala)
- `JsonParseException`              -> [`c.t.finatra.http.internal.exceptions.json.JsonParseExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/json/JsonParseExceptionMapper.scala)
- `CaseClassMappingException`       -> [`c.t.finatra.http.internal.exceptions.json.CaseClassExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/json/CaseClassExceptionMapper.scala)
- `CancelledRequestException`       -> [`c.t.finatra.http.internal.exceptions.CancelledRequestExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/CancelledRequestExceptionMapper.scala)
- `c.t.finagle.Failure`             -> [`c.t.finatra.http.internal.exceptions.FailureExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/FailureExceptionMapper.scala)
- `HttpException`                   -> [`c.t.finatra.http.internal.exceptions.HttpExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/HttpExceptionMapper.scala)
- `HttpResponseException`           -> [`c.t.finatra.http.internal.exceptions.HttpResponseExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/HttpResponseExceptionMapper.scala)
- `org.apache.thrift.TException`    -> [`c.t.finatra.http.internal.exceptions.ThriftExceptionMapper`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/ThriftExceptionMapper.scala)

The `ExceptionManager` walks the exception type hierarchy starting at the given exception type moving up the inheritance chain until it finds mapper configured for the type. In this manner an `ExceptionMapper[Throwable]` will be the last mapper invoked and acts as the "default".

Therefore to change the framework "default" mapper, simply add a new mapper over the `Throwable` type, i.e., `ExceptionMapper[Throwable]` to the `ExceptionManager`. There are multiple ways to add a mapper. 

Either through the `HttpRouter`:

```
  override def configureHttp(router: HttpRouter): Unit = {
    router
      .exceptionMapper[MyThrowableExceptionMapper]
      .exceptionMapper[OtherExceptionMapper]
  }
```

Or in a module which is then added to the Server, e.g.,

```
  object MyExceptionMapperModule extends TwitterModule {
    override def singletonStartup(injector: Injector): Unit = {
      val manager = injector.instance[ExceptionManager]
      manager.add[MyThrowableExceptionMapper]
      manager.add[OtherExceptionMapper]
    }
  }

  ...

  override val modules = Seq(
    MyExceptionMapperModule)
```

Similarly, you can override the "default" mappers for `JsonParseException` and `CaseClassMappingException` by registering your own mapper over these types.
<div></div>

Next section: [Implement a Server "Warmup" Handler](/finatra/user-guide/build-new-http-server/warmup.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-http-server/filter.html"><span aria-hidden="true">&larr;</span>&nbsp;Add&nbsp;Filters</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-http-server/warmup.html">Implement&nbsp;a&nbsp;Server&nbsp;"Warmup"&nbsp;Handler&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
