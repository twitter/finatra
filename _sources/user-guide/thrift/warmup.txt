.. _thrift_warmup:

Thrift Server Warmup
====================

There may be occasions where we want to exercise specific code paths before accepting traffic to the
server (e.g., for triggering JIT in the JVM). We can do this via server warmup.

Warmup
------

Finatra provides a `warmup` lifecycle callback function which will be executed in the correct phase
of server startup, i.e, before the server's external Thrift port is bound and thus before the
TwitterServer `Lifecycle Management <https://twitter.github.io/twitter-server/Features.html#lifecycle-management>`__
`/health` endpoint responds with `OK`.

See `here <../getting-started/lifecycle.html>`__ for more information on the lifecycle of a Finatra
server.

To perform warmup, simply override this callback method and implement your logic. E.g.,

.. code:: scala

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

      override protected def warmup(): Unit = {
        // YOUR WARMUP LOGIC HERE <----------------------------------------------
      }
    }

`c.t.finatra.thrift.routing.ThriftWarmup`
-----------------------------------------

Finatra provides a utility, `c.t.finatra.thrift.routing.ThriftWarmup <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/routing/ThriftWarmup.scala>`__
which will route requests to underlying service implementation configured by the
`ThriftRouter <https://github.com/twitter/finatra/blob/ad4b1fb37d13ae7b27aae50b885fa4ecfd0ed105/thrift/src/main/scala/com/twitter/finatra/thrift/routing/ThriftRouter.scala#L35>`__.

The most common way to use the `c.t.finatra.thrift.routing.ThriftWarmup <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/routing/ThriftWarmup.scala>`__
is via a constructor-arg to an implementation of a
`c.t.inject.utils.Handler <https://github.com/twitter/finatra/blob/develop/inject/inject-utils/src/main/scala/com/twitter/inject/utils/Handler.scala>`__.

The Finatra `c.t.inject.serverTwitterServer` provides a `#handle[T <: Handler: Manifest]` utility
which simply instantiates a given `c.t.inject.utils.Handler <https://github.com/twitter/finatra/blob/develop/inject/inject-utils/src/main/scala/com/twitter/inject/utils/Handler.scala>`__
via the injector and invokes its `#handle()` function.

Combining the above, if we wanted to run an initial call through our Thrift service we could create
the following handler:

.. code:: scala

    import ExampleThriftController
    import com.twitter.example.thriftscala.ExampleService.Add1
    import com.twitter.finatra.thrift.routing.ThriftWarmup
    import com.twitter.inject.Logging
    import com.twitter.inject.utils.Handler
    import com.twitter.util.{Await, Return, Throw, Try}
    import javax.inject.Inject

    class ExampleThriftWarmupHandler @Inject()(
     warmup: ThriftWarmup)
     extends Handler
     with Logging {

      /* Should be a ClientId that is white-listed to your service. */
      private val clientId = ClientId("client123")

      override def handle(): Unit = {
        try {
            clientId.asCurrent {
              warmup.send(
                method = Add1,
                args = Add1.Args(5)) { result: Try[Add1.SuccessType] =>
                  result match {
                    case Return(value) => assert(value == 6, "Warmup request failed.")
                    case Throw(_) => assert(false, "Warmup request failed.")
                  }
                }
            }
        } catch {
          case e: Throwable =>
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

The handler above simply tries to call the `Add1` endpoint of the server.

.. warning::
    As noted above, be careful about exceptions which occur from calling endpoints on your service
    during warmup. Exceptions which escape the handler will potentially stop your server from
    starting and thus, you most likely **do not** want an exception from a warmup call to propagate out
    of the handler.

You would then run this handler in `warmup()` lifecycle callback E.g.,

E.g.,

.. code:: scala

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

      override protected def warmup(): Unit = {
        handle[ExampleThriftWarmupHandler]() <----------------------------------------------
      }
    }

More information
----------------

For more information, we encourage you to take a look at the full
`finatra/examples <https://github.com/twitter/finatra/tree/master/examples>`__ in the
`github <https://github.com/twitter/finatra>`__ source.
