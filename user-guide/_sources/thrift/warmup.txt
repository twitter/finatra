.. _thrift_warmup:

Thrift Server Warmup
====================

There may be occasions where we want to exercise specific code paths before accepting traffic to the server (e.g., for triggering JIT in the JVM).
In this case you can implement a `c.t.inject.utils.Handler <https://github.com/twitter/finatra/blob/develop/inject/inject-utils/src/main/scala/com/twitter/inject/utils/Handler.scala>`__ and your handler should be constructed with a `c.t.finatra.thrift.routing.ThriftWarmup <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/routing/ThriftWarmup.scala>`__ instance.

For example, if we wanted to run an initial call through our Thrift service we could create the following handler:

.. code:: scala

    import ExampleThriftController
    import com.twitter.example.thriftscala.ExampleService.Add1
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

      override def handle() = {
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
            // warm-up and prevent the server from starting. So we simply log
            // the error message here.
            error(e.getMessage, e)
        } finally {
          warmup.close()
        }
        info("Warm-up done.")
      }
    }


You can then run this handler in the `warmup` lifecycle method:

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

      override def warmup() {
        handle[ExampleThriftWarmupHandler]()
      }
    }


The `c.t.inject.app.App#warmup <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L122>`__ lifecycle method is called before the server's external Thrift port is bound and thus before the TwitterServer `Lifecycle Management <https://twitter.github.io/twitter-server/Features.html#lifecycle-management>`__ `/health` endpoint responds with `OK`.

See `here <../getting-started/lifecycle.html>`__ for more information on the lifecycle of a Finatra server.

More information
----------------

For more information, we encourage you to take a look at the full `finatra/examples <https://github.com/twitter/finatra/tree/master/examples>`__ in the `github <https://github.com/twitter/finatra>`__ source.