---
layout: user_guide
title: "Implement a Server \"Warmup\" Handler"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li><a href="/finatra/user-guide/build-new-thrift-server">Building a New Thrift Server</a></li>
  <li class="active">Implement a Server "Warmup" Handler</li>
</ol>

## Basics
===============================

There may be occasions where we want to exercise specific code paths before accepting traffic to the server (say for triggering JIT in the JVM). In this case you can implement a [`com.twitter.inject.utils.Handler`](https://github.com/twitter/finatra/blob/develop/inject/inject-utils/src/main/scala/com/twitter/inject/utils/Handler.scala). Your handler should be constructed with a `com.twitter.finatra.thrift.routing.ThriftWarmup` instance.

For example if we wanted to run an initial call through our Thrift service, we could create the following handler:

```scala
import ExampleThriftController
import com.twitter.example.thriftscala.ExampleService.Add1
import com.twitter.inject.Logging
import com.twitter.inject.utils.Handler
import com.twitter.util.Await
import javax.inject.Inject

class ExampleThriftWarmupHandler @Inject()(
 warmup: ThriftWarmup)
 extends Handler
 with Logging {

  /* Should be a ClientId that is white-listed to your service. */
  private val clientId = ClientId("client123")

  override def handle() = {
    try {
        clientId.asCurrent {
          warmup.send(
            method = Add1,
            args = Add1.Args(5)) { result: Add1.Result =>
              assert(result.success.isDefined, "Warmup request failed.")
              assert(result.success.get == 6, "Warmup request failed.")
            }
        }
    } catch {
      case e: Throwable =>
        // Here we don't want a warmup failure to prevent server start-up --
        // this is important if your service will call downstream services
        // during warmup that could be temporarily down or unavailable.
        // We don't want that unavailability to cause our server to fail
        // warm-up and prevent the server from starting. So we simply log
        // the error message here.
        error(e.getMessage, e)
    } finally {
      warmup.close()
    }
    info("Warm-up done.")
  }
}
```
<div></div>

You can then run this handler in the `warmup` lifecycle method:

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

  override def warmup() {
    handle[ExampleThriftWarmupHandler]()
  }
}
```
<div></div>


The [`com.twitter.inject.app.App#warmup`](https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L122) lifecycle method is called before the server's external Thrift port is bound and thus before the TwitterServer [Lifecycle Management](http://twitter.github.io/twitter-server/Features.html#lifecycle-management) `/health` endpoint responds with `OK`. See [here](/finatra/user-guide/getting-started#lifecycle) for more information on the lifecycle of a Finatra server.

## <a class="anchor" name="more-information" href="#more-information">More information</a>
===============================

For more information, we encourage you to take a look at the full [`finatra/examples`](https://github.com/twitter/finatra/tree/master/examples) in the [github](https://github.com/twitter/finatra) source.

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-thrift-server/filter.html"><span aria-hidden="true">&larr;</span>&nbsp;Add&nbsp;Filters</a></li>
    <li class="next"><a href="/finatra/user-guide/admin-http-interface">HTTP&nbsp;Admin&nbsp;Interface<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
