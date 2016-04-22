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

## <a class="anchor" name="server-definition" href="#server-definition">Create a Server definition</a>
===============================

To start, add a dependency on the `com.twitter.finatra:finatra-http_{scala-version}` library. We also highly recommend depending on `ch.qos.logback:logback-classic` to choose [Logback](http://logback.qos.ch/) as your [SLF4J](http://www.slf4j.org/manual.html) implementation. For more information on logging with Finatra see: [Logging](/finatra/user-guide/logging).

Create a new class that extends [`com.twitter.finatra.http.HttpServer`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala):

```scala
import DoEverythingModule
import ExampleController
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[ExampleController]
  }
}
```
<div></div>

Simplistically, a server can be thought of as a collection of [controllers](#add-controller) composed with [filters](#add-filters). Additionally, a server can define what [modules](/finatra/user-guide/getting-started#modules) to use and how to [map exceptions](#exception-mapper).

The Finatra convention is to create a Scala [*object*](https://twitter.github.io/scala_school/basics2.html#object) with a name ending in "Main" that extends your server class. The server class is used in testing as this allows your server to be instantiated multiple times in tests without worrying about static state persisting across test runs in the same JVM. The static object, e.g., `ExampleServerMain` would then be used when running the server in all other cases (i.e., in production or staging).

### <a class="anchor" name="override-defaults" href="#override-defaults">Override Default Behavior</a>

#### <a class="anchor" name="setting-flags-from-code" href="#setting-flags-from-code">Flags</a>
Some deployment environments may make it difficult to set command line flags. If this is the case, Finatra's [HttpServer](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala)'s core flags can be set from code.
For example, instead of setting the `-http.port` flag, you can override the following method in your server.

```scala
class ExampleServer extends HttpServer {

  override val defaultFinatraHttpPort: String = ":8080"

  override def configureHttp(router: HttpRouter): Unit = {
    ...
  }
}
```
<div></div>

For a list of what flags can be set programmatically, please see the [BaseHttpServer](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/internal/server/BaseHttpServer.scala) class.

#### <a class="anchor" name="default-modules" href="#default-modules">Framework Modules</a>

You can override some of the modules provided by default in [HttpServer](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala). An example use-case would be to provide a custom Jackson module implementation in place of the default [`FinatraJacksonModule`](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala). To do so you would override the `protected def jacksonModule` in your server.

```scala
class ExampleServer extends HttpServer {

  override def jacksonModule = MyCustomJacksonModule

  override def configureHttp(router: HttpRouter): Unit = {
    ...
  }
}
```
<div></div>

If your module is defined as a class, you would pass an instance of the class, e.g., `override def jacksonModule = new MyCustomJacksonModule`.

Next section: [Add an HTTP Controller](/finatra/user-guide/build-new-http-server/controller.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/getting-started"><span aria-hidden="true">&larr;</span>&nbsp;Getting&nbsp;Started</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-http-server/controller.html">Add&nbsp;an&nbsp;HTTP&nbsp;Controller&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
