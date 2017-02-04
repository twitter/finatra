.. _http_warmup:

HTTP Server Warmup
==================

There may be occasions where we want to exercise specific code paths before accepting traffic to the server (e.g., for triggering JIT in the JVM).
In this case you can implement a `c.t.inject.utils.Handler <https://github.com/twitter/finatra/blob/develop/inject/inject-utils/src/main/scala/com/twitter/inject/utils/Handler.scala>`__ and your handler should be constructed with an `c.t.finatra.http.routing.HttpWarmup <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpWarmup.scala>`__ instance.

For example, if we wanted to run an initial call through our HTTP service we could create the following handler:

.. code:: scala

    import com.twitter.finatra.http.routing.HttpWarmup
    import com.twitter.finatra.httpclient.RequestBuilder._
    import com.twitter.inject.utils.Handler
    import javax.inject.Inject

    class ExampleWarmupHandler @Inject()(
      httpWarmup: HttpWarmup)
      extends Handler {

      override def handle() = {
        httpWarmup.send(
          get("/ping"))
      }
    }


The handler above simply tries to hit the `/ping` endpoint of the server.

You can then run this handler in the `warmup` lifecycle method:

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import ExampleFilter
    import ExampleWarmupHandler
    import MalformedURLExceptionMapper
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
          .add[ExampleFilter, ExampleController]
          .exceptionMapper[MalformedURLExceptionMapper]
      }

      override def warmup() {
        handle[ExampleWarmupHandler]()
      }
    }

The `c.t.inject.app.App#warmup <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L122>`__ lifecycle method is called before the server's external HTTP port(s) are bound and thus before the TwitterServer `Lifecycle Management <https://twitter.github.io/twitter-server/Features.html#lifecycle-management>`__ `/health` endpoint responds with `OK`.

See `here <../getting-started/lifecycle.html>`__ for more information on the lifecycle of a Finatra server.

More information
----------------

For more information, we encourage you to take a look at the full `finatra/examples <https://github.com/twitter/finatra/tree/master/examples>`__ in the `github <https://github.com/twitter/finatra>`__ source.