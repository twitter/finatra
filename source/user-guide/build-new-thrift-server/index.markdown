---
layout: user_guide
title: "Building a New Thrift Server"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Building a New Thrift Server</li>
</ol>

Note: the most up-to-date examples are located in the [`finatra/examples`](https://github.com/twitter/finatra/tree/master/examples) project.

## <a class="anchor" name="thrift-basics" href="#thrift-basics">Thrift Basics</a>
===============================

Finatra's thrift support is meant to be used in conjunction with [Finagle](http://twitter.github.io/finagle/) clients and is a [Finagle server](http://twitter.github.io/scrooge/Finagle.html#creating-a-server). Thus it is expected that you will be using [Scrooge](http://twitter.github.io/scrooge/) to generate [Finagle thrift bindings](http://twitter.github.io/scrooge/Finagle.html).

## <a class="anchor" name="server-definition" href="#server-definition">Create a Server definition</a>
===============================

To start, add a dependency on the `com.twitter.finatra:finatra-thrift_{scala-version}` library. We also highly recommend depending on `com.twitter.finatra:finatra-slf4j` and `ch.qos.logback:logback-classic` to choose [Logback](http://logback.qos.ch/) as your [SLF4J](http://www.slf4j.org/manual.html) implementation. For more information on logging with Finatra see: [Logging](/finatra/user-guide/logging).

Create a new class that extends [`com.twitter.finatra.thrift.ThriftServer`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/thrift/ThriftServer.scala):

```scala
import DoEverythingModule
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter

object ExampleServerMain extends ExampleServer

class ExampleServer extends ThriftServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .add[ExampleThriftController]
  }
}
```
<div></div>

This should look familiar as the structure is similar to creating an [HttpServer](/finatra/user-guide/build-new-http-server). The Finatra convention is to create a Scala [*object*](https://twitter.github.io/scala_school/basics2.html#object) with a name ending in "Main". This allows your server to be instantiated multiple times in tests without worrying about static state persisting across test runs in the same JVM. `ExampleServerMain` is then a static object which contains the runnable *main method* for the server.

### <a class="anchor" name="override-defaults" href="#override-defaults">Override Default Behavior</a>

#### <a class="anchor" name="setting-flags-from-code" href="#setting-flags-from-code">Flags</a>
Some deployment environments may make it difficult to set command line flags. If this is the case, Finatra's [ThriftServer](https://github.com/twitter/finatra/blob/master/thrift/src/main/scala/com/twitter/finatra/thrift/ThriftServer.scala)'s core flags can be set from code.
For example, instead of setting the `-thrift.port` flag, you can override the following method in your server.

```scala
class ExampleServer extends HttpServer {

  override val defaultFinatraThriftPort: String = ":9090"

  override def configureThrift(router: ThriftRouter): Unit = {
    ...
  }
}
```
<div></div>

For a list of what flags can be set programmatically, please see the [ThriftServer](https://github.com/twitter/finatra/blob/master/thrift/src/main/scala/com/twitter/finatra/thrift/ThriftServer.scala) class.

Next section: [Add a Thrift Controller](/finatra/user-guide/build-new-thrift-server/controller.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/files"><span aria-hidden="true">&larr;</span>&nbsp;Working&nbsp;with&nbsp;Files</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-thrift-server/controller.html">Add&nbsp;a&nbsp;Thrift&nbsp;Controller&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
