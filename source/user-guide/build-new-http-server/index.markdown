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

## <a class="anchor" name="server-definition">Create a Server definition</a>
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

Simplistically, a server can be thought of as a collection of [controllers](#add-controller) composed with [filters](#add-filters). Additionally, a server can define what  [modules](/finatra/user-guide/getting-started#modules) to use and how to [map exceptions](#exception-mapper). The Finatra convention is to create a Scala [*object*](https://twitter.github.io/scala_school/basics2.html#object) with a name ending in "Main". This allows your server to be instantiated multiple times in tests without worrying about static state persisting across test runs in the same JVM. `ExampleServerMain` is then a static object which contains the runnable *main method* for the server.

### TwitterServer HTTP Admin Interface

All [TwitterServer](https://github.com/twitter/twitter-server) based services start an [HTTP Admin Interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) bound to a port configurable via the `-admin.port` flag. If you want to serve an external interface this will be bound to a separate port configurable via the `-http.port` flag.

Some deployment environments such as [Heroku](https://www.heroku.com/), [AppFog](https://www.appfog.com/), and [OpenShift](https://www.openshift.com) only allow a single port to be used when deploying an application. In these cases, you can disable the [HTTP Admin Interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) started by [TwitterServer](https://github.com/twitter/twitter-server) as such:

```scala
class ExampleServer extends HttpServer {
  override val disableAdminHttpServer = true
  ...
}
```
<div></div>

Since the admin port is currently required by [TwitterServer](https://github.com/twitter/twitter-server) you will need to set the `-admin.port` and `-http.port` flags to the same value in addition to specifying `override val disableAdminHttpServer = true` above.

For more information, see the [Heroku](https://www.heroku.com/) [hello-world example](https://github.com/twitter/finatra/tree/master/examples/hello-world-heroku).

### <a class="anchor" name="override-defaults">Override Default Behavior</a>

#### <a class="anchor" name="setting-flags-from-code">Flags</a>
Some deployment environments may make it difficult to set command line flags. If this is the case, Finatra's [HttpServer](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala)'s core flags can be set from code.
For example, instead of setting the `-maxRequestSize` flag, you can override the following method in your server.

```scala
class ExampleServer extends HttpServer {

  override val defaultMaxRequestSize = 10.megabytes

  override def configureHttp(router: HttpRouter) {
    ...
  }
}
```
<div></div>

#### <a class="anchor" name="default-modules">Framework Modules</a>

You can override some of the modules provided by default in [HttpServer](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala). An example use-case would be to provide a custom Jackson module implementation in place of the default [`FinatraJacksonModule`](https://github.com/twitter/finatra/blob/master/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala). To do so you would override the `protected def jacksonModule` in your server.

```scala
class ExampleServer extends HttpServer {

  override def jacksonModule = MyCustomJacksonModule

  override def configureHttp(router: HttpRouter) {
    ...
  }
}
```
<div></div>

If your module is defined as a class, you would pass an instance of the class, e.g., `override def jacksonModule = new MyCustomJacksonModule`.

Next section: [Add a Controller](/finatra/user-guide/build-new-http-server/controller.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/getting-started"><span aria-hidden="true">&larr;</span>&nbsp;Getting&nbsp;Started</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-http-server/controller.html">Add&nbsp;a&nbsp;Controller&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
