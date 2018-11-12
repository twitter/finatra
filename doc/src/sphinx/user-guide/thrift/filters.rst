.. _thrift_filters:

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

Request Scope
-------------

|Guice|_ supports `custom scopes <https://github.com/google/guice/wiki/CustomScopes>`__ in addition 
to the defined ``@Singleton``, ``@SessionScoped``, and ``@RequestScoped`` `scopes <https://github.com/google/guice/wiki/Scopes>`__.

``@RequestScoped`` is often used to allow injection of instances which can change depending on the incoming request 
(e.g. the currently authenticated User). 

Finatra provides a custom implementation of the default |Guice|_ ``@RequestScoped`` functionality which works
across Finagle non-blocking threads. The default |Guice|_ `@RequestScoped <https://github.com/google/guice/wiki/Scopes#scopes>`__ 
implementation uses `ThreadLocals <https://docs.oracle.com/javase/7/docs/api/java/lang/ThreadLocal.html>`__ 
which will not work within the context of a Twitter `c.t.util.Future <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__.

.. note:: 

     Fields added to the Finatra Request Scope will remain present in threads launched from a 
     Finagle `FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__.

Adding Classes into the Finatra Request Scope
---------------------------------------------

First add a dependency on `com.twitter:inject-request-scope`

Then define a module which mixes in the `c.t.inject.requestscope.RequestScopeBinding` trait.
This trait defines `#bindRequestScope[T]` which will bind the given type to an "unseeded" 
`Provider[T]` of the type *in* the "FinagleRequestScope". E.g.,

.. code:: scala

    import com.twitter.inject.TwitterModule
    import com.twitter.inject.requestscope.RequestScopeBinding   

    object UserModule
      extends TwitterModule
      with RequestScopeBinding {

      override def configure(): Unit = {
        bindRequestScope[User]
      }
    }

You must remember to "seed" this `Provider[T]` by obtaining an instance of the `FinagleRequestScope`
and calling `#seed[T](instance)`. For request scoping, you would generally do this in another 
`Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__ 
executed on the request path.

For example, to define a `Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__ 
which seeds a `User` into the "FinagleRequestScope":

.. code:: scala
    
    import com.twitter.finagle.Service
    import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
    import com.twitter.inject.requestscope.FinagleRequestScope
    import com.twitter.util.Future
    import javax.inject.{Inject, Singleton}

    @Singleton
    class UserFilter @Inject()(
      requestScope: FinagleRequestScope)
      extends ThriftFilter {

      override def apply[T, U](request: ThriftRequest[T], service: Service[ThriftRequest[T], U]): Future[U] = {
        val userId = parseUserId(request) // User-defined method to parse a "user id" from the `ThriftRequest[T]`
        val user = User(userId)
        requestScope.seed[User](user)
        service(request)
      }
    }


Next, you must ensure to add the `FinagleRequestScopeFilter <https://github.com/twitter/finatra/tree/master/inject/inject-request-scope/src/main/scala/com/twitter/inject/requestscope/FinagleRequestScopeFilter.scala>`__ to your
server before the defined `Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__ which seeds the provided instance.

E.g., for the `UserFilter` defined above (shown with common filters in a recommended filter order):

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
          .filter[ClientIdWhitelistFilter]
          .filter[FinagleRequestScopeFilter]
          .filter[UserFilter]
          .exceptionMapper[FinatraThriftExceptionMapper]
          .add[MyController]
        }
    }

Lastly, wherever you need to access the Request scoped `User` inject a `User` or a `Provider[User]` type. 

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

.. |Guice| replace:: Guice
.. _Guice: https://github.com/google/guice
