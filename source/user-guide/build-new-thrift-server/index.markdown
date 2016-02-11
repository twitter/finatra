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

## <a class="anchor" name="server-definition" href="#server-definition">Create a Server definition</a>
===============================

To start, add a dependency on the `com.twitter.finatra:finatra-thrift_{scala-version}` library. We also highly recommend depending on `com.twitter.finatra:finatra-slf4j` and `ch.qos.logback:logback-classic` to choose [Logback](http://logback.qos.ch/) as your [SLF4J](http://www.slf4j.org/manual.html) implementation. For more information on logging with Finatra see: [Logging](/finatra/user-guide/logging).

Create a new class that extends [`com.twitter.finatra.thrift.ThriftServer`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/thrift/ThriftServer.scala):

```scala
import DoEverythingModule
import com.twitter.finatra.thrift.routing.HttpRouter
import com.twitter.finatra.thrift.ThriftServer

object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureThrift(router: ThriftRouter) {
  }
}
```
<div></div>

This should look familiar as the structure is similar to creating an [HttpServer](/finatra/user-guide/build-new-http-server).


Next section: [Add a Thrift Controller](/finatra/user-guide/build-new-thrift-server/controller.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/getting-started"><span aria-hidden="true">&larr;</span>&nbsp;Getting&nbsp;Started</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-thrift-server/controller.html">Add&nbsp;a&nbsp;Thrift&nbsp;Controller&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
