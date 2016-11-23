---
layout: user_guide
title: "Add Filters"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li><a href="/finatra/user-guide/build-new-thrift-server">Building a New Thrift Server</a></li>
  <li class="active">Add Filters</li>
</ol>

## Basics
===============================

The ThriftRouter allows your to build a global filter chain that will trigger on the request path when executing an RPC call to the ThriftController. Filters must be a subclass of [`c.t.finatra.thrift.ThriftFilter`](https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/ThriftFilter.scala) which is a `c.t.finagle.Filter[ThriftRequest[T], Rep, ThriftRequest[T], Rep]` that is polymorphic in `T`.

If you want to apply a filter (or filters) in your server you can do the following:

```scala
import DoEverythingModule
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.filters._

object ExampleServerMain extends ExampleServer

class ExampleServer extends ThriftServer {

  override val modules = Seq(
    DoEverythingModule)

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .add[ExampleThriftController]
  }
}
```
<div></div>

Next section: [Implement a Server "Warmup" Handler](/finatra/user-guide/build-new-thrift-server/warmup.html).

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-thrift-server/controller.html"><span aria-hidden="true">&larr;</span>&nbsp;Add&nbsp;a&nbsp;Thrift&nbsp;Controller</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-thrift-server/warmup.html">Implement&nbsp;a&nbsp;Server&nbsp;"Warmup"&nbsp;Handler&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
