.. _thrift_filters:

Filtering Thrift Requests
=========================

`c.t.finatra.thrift.filters.AccessLoggingFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/AccessLoggingFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- provides for psuedo "Common Log Format" style logging for Thrift requests and responses.

`c.t.inject.thrift.filters.DarkTrafficFilter <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/filters/DarkTrafficFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- An implementation of `com.twitter.finagle.exp.AbstractDarkTrafficFilter <https://github.com/twitter/finagle/blob/develop/finagle-exp/src/main/scala/com/twitter/finagle/exp/AbstractDarkTrafficFilter.scala>`__ which extends `com.twitter.finagle.Filter.TypeAgnostic <https://github.com/twitter/finagle/blob/afb191ab8eda132197dd2679cd614fc968767fc4/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L295>`__ and thus works in a Finatra `ThriftRouter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/routing/routers.scala#L128Z>`__ Filter chain.

`c.t.finatra.thrift.filters.ExceptionMappingFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/ExceptionMappingFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Filter for use in mapping exceptions to Thrift responses. See `ExceptionMappingFilter <exceptions.html#exceptionmappingfilter>`__ for more information.

`c.t.finatra.thrift.filters.LoggingMDCFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/LoggingMDCFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- properly initializes the framework's `Mapped Diagnostic Context (MDC) <https://www.slf4j.org/manual.html#mdc>`__ adapter for logging.

`c.t.finatra.thrift.filters.StatsFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/StatsFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Tracks `per-method stats <controllers.html#per-method-stats>`__ scoped under `per_method_stats/<method>` including success/failure (with exceptions) counters, and a latency (in millis) histogram.

`c.t.finatra.thrift.filters.ThriftMDCFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/ThriftMDCFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- places the current Finagle `MethodMetadata <https://github.com/twitter/finagle/blob/develop/finagle-thrift/src/main/scala/com/twitter/finagle/thrift/MethodMetadata.scala>`__ and `ClientId <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/thrift/ClientId.scala>`__ locals into the `Mapped Diagnostic Context (MDC) <https://www.slf4j.org/manual.html#mdc>`__.

`c.t.finatra.thrift.filters.TraceIdMDCFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/TraceIdMDCFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- places the current Finagle `TraceId <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/tracing/TraceId.scala>`__ local into the `Mapped Diagnostic Context (MDC) <https://www.slf4j.org/manual.html#mdc>`__.

Global Filters
--------------

The `ThriftRouter` allows you to build a global Filter chain which will trigger on the request path
when executing an RPC call to methods implemented by the added `Thrift Controller <controllers.html>`__.

Filters must be a subclass of the `c.t.finagle.Filter.TypeAgnostic <https://github.com/twitter/finagle/blob/ee9cb4ec2c17b810354b36ff97816fc97efb6394/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L295>`__ which is a

.. code:: scala

    c.t.finagle.Filter[T, Rep, T, Rep]

that is polymorphic in `T`.

If you want to apply a Filter or Filters to **all** methods of your Thrift Controller, call the
`ThriftRouter#filter` method, to register a `Filter.TypeAgnostic`:

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

Note, like `HTTP <../http/filters.html>`__, Filters are applied **in the order** they are defined on
all methods. Filters can be added to the `ThriftRouter` by type (as in the example above) or by instance.

For more information see the `Finagle User\'s Guide <https://twitter.github.io/finagle/guide/index.html>`__
section on `Filters <https://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters>`__.

Per-method Filtering
--------------------

`TypeAgnostic` Filters
~~~~~~~~~~~~~~~~~~~~~~

You can filter by a `TypeAgnostic Filter <https://github.com/twitter/finagle/blob/ee9cb4ec2c17b810354b36ff97816fc97efb6394/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L295>`__
per-method implemented in a Controller, by calling the `handle(ThriftMethod)#filtered` Function e.g.:

.. code:: scala

    import com.twitter.finagle.{Filter, Service, SimpleFilter}
    import com.twitter.util.Future

    val countEchoFilter = new Filter.TypeAgnostic {
      private[this] val echos = stats.counter("echo_calls")
      def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new SimpleFilter[Req, Rep]{
        def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
          echos.incr()
          service(request)
        }
      }
    }

    ...

    import com.foo.bar.thriftscala.EchoService.Echo
    import com.twitter.finatra.thrift.Controller
    import com.twitter.util.Future
    import scala.util.control.NoStackTrace

    class ExampleController extends Controller {

      handle(Echo).filtered(countEchoFilter) { args: Echo.Args =>
        if (args.msg == "clientError") {
          Future.exception(new Exception("client error") with NoStackTrace)
        } else {
          Future.value(args.msg)
        }
      }
    }

Note that you can chain `handle(ThriftMethod)#filtered` calls arbitrarily deep.

Typed Filters
~~~~~~~~~~~~~

If you'd like to specify a typed Filter, use the `handle(ThriftMethod)#withService` Function and apply
your typed `Filter[-ReqIn, +RepOut, +ReqOut, -RepIn]` to your `Service[-ReqOut, +RepIn]` implementation.

.. code:: scala

    import com.foo.bar.thriftscala.EchoService.Echo
    import com.twitter.finagle.{Filter, Service, SimpleFilter}
    import com.twitter.inject.Logging
    import com.twitter.util.Future

    val echoLoggingFilter = new Filter[Echo.Args, String, Echo.Args, String] with Logging {
      def apply(request: Echo.Args, service: Service[Echo.Args, String]): Future[String] = {
        info(s"Received request message: ${request.msg}")
        service(request)
      }
    }

    ...

    import com.foo.bar.thriftscala.EchoService.Echo
    import com.twitter.finatra.thrift.Controller
    import com.twitter.util.Future
    import scala.util.control.NoStackTrace

    class ExampleController extends Controller {

      val svc: Service[Echo.Args, String] = Service.mk { args: Echo.Args =>
        if (args.msg == "clientError") {
          Future.exception(new Exception("client error") with NoStackTrace)
        } else {
          Future.value(args.msg)
        }
      }

      handle(Echo).withService(echoLoggingFilter.andThen(svc))
    }

For more information on the `handle(ThriftMethod)` DSL of the Controller, see the documentation on `Thrift
Controllers <controllers.html#implementing-methods-with-handle-thriftmethod>`__.

Request Scope
-------------

|Guice|_ supports `custom scopes <https://github.com/google/guice/wiki/CustomScopes>`__ in addition
to the defined ``@Singleton``, ``@SessionScoped``, and ``@RequestScoped``
`scopes <https://github.com/google/guice/wiki/Scopes>`__. ``@RequestScoped`` is often used to allow
injection of instances which can change depending on the incoming request (e.g. the currently
authenticated User).

Finatra provides a custom implementation of the default |Guice|_ ``@RequestScoped`` functionality
which works across Finagle non-blocking threads. The default |Guice|_
`@RequestScoped <https://github.com/google/guice/wiki/Scopes#scopes>`__ implementation uses
`ThreadLocals <https://docs.oracle.com/javase/7/docs/api/java/lang/ThreadLocal.html>`__ which will
not work within the context of a Twitter `c.t.util.Future <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__.

.. note::

    Fields added to the Custom Request Scope will remain present in threads launched from a
    `FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__.

Adding Classes into the Custom Request Scope
--------------------------------------------

First add a dependency on `com.twitter:inject-request-scope` (`finatra/inject/inject-request-scope`).

Then define a module which mixes in the `c.t.inject.requestscope.RequestScopeBinding` trait.
This trait defines `#bindRequestScope[T]` which will bind the given type to an "unseeded"
`Provider[T]` of the type *in* the custom "FinagleRequestScope". E.g.,

.. code:: scala

    import com.twitter.inject.TwitterModule
    import com.twitter.inject.requestscope.RequestScopeBinding

    object UserModule extends TwitterModule with RequestScopeBinding {

      override def configure(): Unit = {
        bindRequestScope[User]
      }
    }

.. important::

    Remember to include this Module in your `server's list of Modules <../thrift/server.html#thrift-server-definition>`__.

You must then "seed" this `Provider[T]` by obtaining an instance of the `FinagleRequestScope`
and calling `#seed[T](instance)`. For request scoping, you would generally do this in a
`TypeAgnostic Filter <https://github.com/twitter/finagle/blob/ee9cb4ec2c17b810354b36ff97816fc97efb6394/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L295>`__
executed on the request path.

For example, to define a `TypeAgnostic Filter <https://github.com/twitter/finagle/blob/ee9cb4ec2c17b810354b36ff97816fc97efb6394/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L295>`__
which seeds a `User` into the "FinagleRequestScope":

.. code:: scala

    import com.twitter.finagle.{Filter, Service}
    import com.twitter.inject.requestscope.FinagleRequestScope
    import com.twitter.util.Future
    import javax.inject.{Inject, Singleton}

    @Singleton
    class UserFilter @Inject()(
      finagleRequestScope: FinagleRequestScope
    ) extends Filter.TypeAgnostic {

      def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] =
        new Filter[Req, Rep, Req, Rep] {
          def apply[Req, Rep](request: Req, service: Service[Req, Rep]): Future[Rep] = {
            val userId = parseUserId(request) // User-defined method to parse a "user id" from the request
            val user = User(userId)
            finagleRequestScope.seed[User](user)
            service(request)
          }
        }
    }


Next, add the `FinagleRequestScopeFilter.TypeAgnostic <https://github.com/twitter/finatra/tree/master/inject/inject-request-scope/src/main/scala/com/twitter/inject/requestscope/FinagleRequestScopeFilter.scala>`__
to your server _above_ the defined `Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__ which seeds the provided instance.

E.g., for the `UserFilter` defined above (shown with commonly recommended Filters in the recommended
order):

.. code:: scala

    import com.google.inject.Module
    import com.twitter.finatra.thrift.exceptions.FinatraThriftExceptionMapper
    import com.twitter.finatra.thrift.ThriftServer
    import com.twitter.finatra.thrift.routing.ThriftRouter
    import com.twitter.finatra.thrift.filters._
    import com.twitter.finatra.thrift.modules.ClientIdAcceptlistModule

    class Server extends ThriftServer {
      override def modules: Seq[Module] = Seq(ClientIdAcceptlistModule)

      override def configureThrift(router: ThriftRouter): Unit = {
        router
          .filter[LoggingMDCFilter]
          .filter[TraceIdMDCFilter]
          .filter[ThriftMDCFilter]
          .filter[AccessLoggingFilter]
          .filter[StatsFilter]
          .filter[ExceptionMappingFilter]
          .filter[ClientIdAcceptlistFilter]
          .filter[FinagleRequestScopeFilter.TypeAgnostic]
          .filter[UserFilter]
          .exceptionMapper[FinatraThriftExceptionMapper]
          .add[MyController]
        }
    }

Lastly, wherever you need to access the Request scoped `User` inject a `User` or a `Provider[User]`
type.

.. code:: scala

    import com.twitter.finagle.Service
    import com.twitter.finatra.thrift.Controller
    import javax.inject.{Inject, Provider, Singleton}

    @Singleton
    class MyController @Inject()(
      dao: GroupsDAO,
      user: Provider[User])
      extends Controller with MyService.BaseServiceIface {

      val getUser: Service[GetUser.Args, GetUser.SuccessType] = handle(GetUser) { args: GetUser.Args =>
        "The incoming user has id " + user.get.id
      }
    }

.. note:: The `Provider[User]` type must be used when injecting into a Singleton class.

.. |rarrow| unicode:: U+02192 .. right arrow

.. |Guice| replace:: Guice
.. _Guice: https://github.com/google/guice
