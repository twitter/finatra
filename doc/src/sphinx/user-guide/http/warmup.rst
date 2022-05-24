.. _http_warmup:

HTTP Server Warmup
==================

There may be occasions where we want to exercise specific code paths before accepting traffic to the
server (e.g., for triggering JIT in the JVM). We can do this via server warmup.

Warmup
------

Finatra provides a `warmup` lifecycle callback function which will be executed in the correct phase
of server startup, i.e, before the server's external HTTP port(s) are bound and thus before the
TwitterServer `Lifecycle Management <https://twitter.github.io/twitter-server/Features.html#lifecycle-management>`__
`/health` endpoint responds with `OK`.

See `here <../getting-started/lifecycle.html>`__ for more information on the lifecycle of a Finatra
server.

To perform warmup, simply override this callback method and implement your logic. E.g.,

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

      override protected def warmup(): Unit = {
        // YOUR WARMUP LOGIC HERE <----------------------------------------------
      }

`c.t.finatra.http.routing.HttpWarmup`
-------------------------------------

Finatra provides a utility, `c.t.finatra.http.routing.HttpWarmup <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/routing/HttpWarmup.scala>`__
which will route requests to correct service implementation configured by the
`HttpRouter <https://github.com/twitter/finatra/blob/ad4b1fb37d13ae7b27aae50b885fa4ecfd0ed105/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala#L41>`__.

The most common way to use the `c.t.finatra.http.routing.HttpWarmup <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/routing/HttpWarmup.scala>`__
is via a constructor-arg to an implementation of a
`c.t.inject.utils.Handler <https://github.com/twitter/finatra/blob/develop/inject/inject-utils/src/main/scala/com/twitter/inject/utils/Handler.scala>`__.

The Finatra `c.t.inject.serverTwitterServer` provides a `#handle[T <: Handler: Manifest]` utility
which simply instantiates a given `c.t.inject.utils.Handler <https://github.com/twitter/finatra/blob/develop/inject/inject-utils/src/main/scala/com/twitter/inject/utils/Handler.scala>`__
via the injector and invokes its `#handle()` function.

Combining the above, if we wanted to run an initial call through our HTTP service we could create the
following handler:

.. code:: scala

    import com.twitter.finatra.http.request.RequestBuilder._
    import com.twitter.finatra.http.routing.HttpWarmup
    import com.twitter.inject.utils.Handler
    import scala.util.control.NonFatal
    import javax.inject.Inject

    class ExampleWarmupHandler @Inject()(
      warmup: HttpWarmup)
      extends Handler {

      override def handle(): Unit = {
        try {
          warmup.send(get("/ping"))()
        } catch {
          case NonFatal(e) =>
            // Here we don't want a warmup failure to prevent server start-up --
            // this is important if your service will call downstream services
            // during warmup that could be temporarily down or unavailable.
            // We don't want that unavailability to cause our server to fail
            // warm-up and thus prevent the server from starting. So we simply
            // log the error message here.
            error(e.getMessage, e)
        }
        info("Warm-up done.")
      }
    }


The handler above simply tries to hit the `/ping` endpoint of the server.

.. warning::
    As noted above, be careful about exceptions which occur from calling endpoints on your service
    during warmup. Exceptions which escape the handler will potentially stop your server from
    starting and thus, you most likely **do not** want an exception from a warmup call to propagate out
    of the handler.

You would then run this handler in the `warmup()` lifecycle callback. E.g.,

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

      override protected def warmup(): Unit = {
        handle[ExampleWarmupHandler]() <----------------------------------------------
      }
    }

More information
----------------

For more information, we encourage you to take a look at the full
`finatra/examples <https://github.com/twitter/finatra/tree/master/examples>`__ in the
`github <https://github.com/twitter/finatra>`__ source.
