.. _http_filters:

Filtering HTTP Requests
=======================

`c.t.finatra.http.filters.AccessLoggingFilter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/AccessLoggingFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- provides for "Common Log Format" style logging for HTTP requests and responses.

`c.t.finatra.http.filters.ExceptionMappingFilter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/ExceptionMappingFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Filter for use in mapping exceptions to HTTP responses. See `ExceptionMappingFilter <exceptions.html#exceptionmappingfilter>`__ for more information.

`c.t.finatra.http.filters.HttpNackFilter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/HttpNackFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- ensures that Finagle `Nacks <https://twitter.github.io/finagle/guide/Glossary.html?highlight=nack>`__ are propagated as HTTP headers in responses.

`c.t.finatra.http.filters.HttpResponseFilter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/HttpResponseFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- an HTTP response conformance Filter which, among other things, ensures `Location` response headers are properly specified.

`c.t.finatra.http.filters.LoggingMDCFilter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/LoggingMDCFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- properly initializes the framework's `Mapped Diagnostic Context (MDC) <https://www.slf4j.org/manual.html#mdc>`__ adapter for logging.

`c.t.finatra.http.filters.StatsFilter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/StatsFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- a drop-in replacement for the `Finagle StatsFilter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/service/StatsFilter.scala>`__ which can additionally provide `per-route stats <controllers.html#per-route-stats>`__.

`c.t.finatra.http.filters.TraceIdMDCFilter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/TraceIdMDCFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- places the current Finagle `TraceId <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/tracing/TraceId.scala>`__ local into the `Mapped Diagnostic Context (MDC) <https://www.slf4j.org/manual.html#mdc>`__.


.. admonition:: c.t.finatra.http.filters.CommonFilters

  Finatra composes -- in a recommended order -- several typically useful HTTP Filters into 
  `c.t.finatra.http.filters.CommonFilters <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala>`__.
  `CommonFilters` can be added in the same manner as any other Filter.

Global Filters
--------------

Filters by default execute **after** route matching. Meaning, for a given request, the URI path is
matched in the routing table **before** executing any Filter. If you need to be able to run a Filter
*before* route matching, you can add the Filter via `HttpRouter#filter[T](beforeRouting = true)`,
this is especially useful if the Filter manually inspects to see if it should apply on a given
request URI path that may not exist in the routing table (e.g., is not defined by any controller
added to the server).

If you want to apply a Filter or Filters to **all** added controllers you can do the following:

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.finatra.http.{Controller, HttpServer}

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override val modules = Seq(
        DoEverythingModule)

      override def configureHttp(router: HttpRouter) {
        router
          .filter[CommonFilters]
          .add[ExampleController]
      }
    }

Note that Filters -- much like controller routes -- are applied **in the order defined**. Filters
can be added to the `HttpRouter` by type (as in the example above) or by instance.

For more information see the `Finagle User\'s Guide <https://twitter.github.io/finagle/guide/index.html>`__
section on `Filters <https://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters>`__.

Per-controller Filters
----------------------

It is also possible to add Filters per controller, using `HttpRouter#add[F1 <: HttpFilter, C <: Controller]`.

These Filters will apply to all routes in the Controller.

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import ExampleFilter
    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.filters.AccessLoggingFilter
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.finatra.http.{Controller, HttpServer}

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override val modules = Seq(
        DoEverythingModule)

      override def configureHttp(router: HttpRouter) {
        router
          .add[ExampleFilter, ExampleController]
      }
    }

Currently, `HttpRouter#add` supports in-lining up to ten (10) filters before a Controller. If you
need to include more than ten Filters please consider combining them with `c.t.finatra.filters.MergedFilter <https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/filters/MergedFilter.scala>`__
in the same manner as `c.t.finatra.http.filters.CommonFilters <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala>`__
then using the combined Filter in your call to `HttpRouter#add`.

In all the above usages, we are applying the Filter *by type* allowing the framework to instantiate
instances of the Filters. However, all of these methods support passing constructed instances.

Per-route Filters
-----------------

Additionally, you can specify Filters *inside* of a Controller per-route,
e.g.,

.. code:: scala

    class ExampleController @Inject()(
      exampleService: ExampleService
    ) extends Controller {

      filter[ExampleFilter].get("/ping") { request: Request =>
        "pong"
      }

      filter[ExampleFilter]
        .filter[AnotherExampleFilter]
        .get("/name") { request: Request =>
        response.ok.body("Bob")
      }

      filter(new OtherFilter).post("/foo") { request: Request =>
        exampleService.do(request)
        "bar"
      }
    }

As you can see, you can choose to apply the Filter either by type or provide an instance. Note that 
you can chain `Controller#filter` calls arbitrarily deep.

Request Scope
-------------

|Guice|_ supports `custom scopes <https://github.com/google/guice/wiki/CustomScopes>`__ in addition 
to the defined ``@Singleton``, ``@SessionScoped``, and |@RequestScoped|_
`scopes <https://github.com/google/guice/wiki/Scopes>`__. |@RequestScoped|_ is often used to allow
injection of instances which can change depending on the incoming request (e.g. the currently
authenticated User).

Finatra provides a custom implementation of the default |Guice|_ |@RequestScoped|_ functionality
which works across Finagle non-blocking threads. The default |Guice|_ |@RequestScoped|_
implementation uses `ThreadLocals <https://docs.oracle.com/javase/7/docs/api/java/lang/ThreadLocal.html>`__ 
which will not work within the context of a Twitter `c.t.util.Future <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__.

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

    Remember to include this Module in your `server's list of Modules <../http/server.html#http-server-definition>`__.

You must then "seed" this `Provider[T]` by obtaining an instance of the `FinagleRequestScope`
and calling `#seed[T](instance)`. For request scoping, you would generally do this in a
`Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__ 
executed on the request path.

For example, to define a `Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__ 
which seeds a `User` into the "FinagleRequestScope":

.. code:: scala

    import com.twitter.finagle.{Service, SimpleFilter}
    import com.twitter.finagle.http.{Request, Response}
    import com.twitter.inject.requestscope.FinagleRequestScope
    import com.twitter.util.Future
    import javax.inject.{Inject, Singleton}

    @Singleton
    class UserFilter @Inject()(
      finagleRequestScope: FinagleRequestScope
    ) extends SimpleFilter[Request, Response] {

      def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
        val userId = parseUserId(request) // User-defined method to parse a "user id" from the request
        val user = User(userId)
        finagleRequestScope.seed[User](user)
        service(request)
      }
    }


Next, add the `FinagleRequestScopeFilter <https://github.com/twitter/finatra/tree/master/inject/inject-request-scope/src/main/scala/com/twitter/inject/requestscope/filters.scala>`__ to your
server _above_ the defined `Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__ which seeds the provided instance.

.. note::
  The `FinagleRequestScopeFilter <https://github.com/twitter/finatra/tree/master/inject/inject-request-scope/src/main/scala/com/twitter/inject/requestscope/FinagleRequestScopeFilter.scala>`__ expects
  "request" and "response" type parameters. In this case, they would be
  `com.twitter.finagle.http.Request` and `com.twitter.finagle.http.Response`.

E.g., for the `UserFilter` defined above (shown with common Filters in a recommended Filter order):

.. code:: scala
    
    import com.twitter.finagle.http.{Request, Response}
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.inject.requestscope.FinagleRequestScopeFilter

    class Server extends HttpServer {
      override def configureHttp(router: HttpRouter) {
        router
          .filter[LoggingMDCFilter[Request, Response]]
          .filter[TraceIdMDCFilter[Request, Response]]
          .filter[CommonFilters]
          .filter[FinagleRequestScopeFilter[Request, Response]]
          .filter[UserFilter]
          .add[MyController]
        }
    }

Lastly, wherever you need to access the Request scoped `User` inject a `User` or a `Provider[User]`
type.

.. code:: scala

    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.Controller
    import javax.inject.{Inject, Provider, Singleton}

    @Singleton
    class MyController @Inject()(
      dao: GroupsDAO,
      user: Provider[User])
      extends Controller {

      get("/") { request: Request =>
        "The incoming user has id " + user.get.id
      }
    }

.. note:: The `Provider[User]` type must be used when injecting into a Singleton class.


Using `c.t.finagle.http.Request#ctx`
------------------------------------

Above we saw how to seed classes to the Finatra Request Scope using a `Provider[T]`.

However, we recommend *not* seeding with a request scope `Provider[T]` but instead using Finagle's
`c.t.finagle.http.Request#ctx <https://github.com/twitter/finagle/blob/f970bd5b0c1b3f968694dcde33b47b21869b9f0e/finagle-base-http/src/main/scala/com/twitter/finagle/http/Request.scala#L29>`__.
Internally, for HTTP, we generally use the `Request#ctx` over `Provider[T]` even though we use
|Guice|_ extensively.

To use the `Request#ctx` technique, first create a `RecordSchema <https://github.com/twitter/util/blob/9fa550a269d2287b24e94921a352ba954f9f4bfb/util-collection/src/main/scala/com/twitter/collection/RecordSchema.scala#L6>`__
`request field <https://github.com/twitter/finagle/blob/f970bd5b0c1b3f968694dcde33b47b21869b9f0e/finagle-base-http/src/main/scala/com/twitter/finagle/http/Request.scala#L23>`__,
a "context", and an HTTP `Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__
which can set the value of the "context".

The "context" should define a method to retrieve the value from the `Request#ctx`. Typically, this
method is defined in an `implicit class` which takes a `c.t.finagle.http.Request` as an argument.
Importing the "context" members into scope thus allows for calling the method defined in the
`implicit class` as though it were a method on the HTTP `Request` object.

For example, a UserContext

.. code:: scala

    import com.twitter.finagle.http.Request

    // domain object to set as a RecordSchema field
    case class User(id: Long)

    // create a context
    object UserContext {
      private val UserField = Request.Schema.newField[User]() // provide a default value

      // methods from this implicit will be available on the `Request` when UserContext._ is imported
      implicit class UserContextSyntax(val request: Request) extends AnyVal {
        def user: User = request.ctx(UserField)
      }

      private[twitter] def setUser(request: Request): Unit = {
        val user = User(1) //Parse user from request headers/cookies/etc.
        request.ctx.update(UserField, user)
      }
    }

And a `Filter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala>`__ 
which can set the `User`:

.. code:: scala

    // create a Filter
    class UserFilter extends SimpleFilter[Request, Response] {
      override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
        UserContext.setUser(request)
        service(request)
      }
    }


In the above example, the retrieval method defined in the implicit class `UserContextSyntax` will
then be available on the the `request` when the `UserContext._` members are imported:

.. code:: scala

    // import the UserContext members into scope, the method Request#user
    // will now be available on the Request object.
    import UserContext._

    class MyController() extends Controller {
      get("/") { request: Request =>
        "Hi " + request.user.id
      }
    }

.. |rarrow| unicode:: U+02192 .. right arrow

.. |Guice| replace:: Guice
.. _Guice: https://github.com/google/guice

.. |@RequestScoped| replace:: ``@RequestScoped``
.. _@RequestScoped: https://github.com/google/guice/blob/master/extensions/servlet/src/com/google/inject/servlet/RequestScoped.java
