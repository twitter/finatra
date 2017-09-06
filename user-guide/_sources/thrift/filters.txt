.. _thrift_filter:

Filtering Thrift Requests
=========================

The `ThriftRouter` allows you to build a global filter chain which will trigger on the request path when executing an RPC call to the ThriftController.

Filters must be a subclass of the `c.t.finatra.thrift.ThriftFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/ThriftFilter.scala>`__ which is a

.. code:: scala

    c.t.finagle.Filter[ThriftRequest[T], Rep, ThriftRequest[T], Rep]

that is polymorphic in `T`.

If you want to apply a filter or filters in your Thrift server simply call the `ThriftRouter#filter` method, to register a `ThriftFilter`.

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
    }

Note, like `HTTP <../http/filters.html>`__ filters are applied **in the order** they are defined.

For more information see the `Finagle User\'s Guide <https://twitter.github.io/finagle/guide/index.html>`__ section on `Filters <https://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters>`__.