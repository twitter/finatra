.. _filters:

Filtering HTTP Requests
=======================

c.t.finatra.http.filters.CommonFilters
--------------------------------------

Finatra composes -- in defined order -- several generally useful HTTP filters into `c.t.finatra.http.filters.CommonFilters <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala>`__.

`CommonFilters <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala>`__ can be added in the same manner as any other filter. Included filters:

`c.t.finatra.http.filters.StatsFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/StatsFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- a drop-in replacement for the `Finagle StatsFilter <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/service/StatsFilter.scala>`__ and can provide `per-route stats <controllers.html#per-route-stats>`__.

`c.t.finatra.http.filters.AccessLoggingFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/AccessLoggingFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- provides for "Common Log Format" style logging for HTTP requests and responses.

`c.t.finatra.http.filters.HttpResponseFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/HttpResponseFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- an HTTP response conformance filter which, among other things, ensures `Location` response headers are properly specified.

`c.t.finatra.http.filters.ExceptionMappingFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/ExceptionMappingFilter.scala>`__ |rarrow|
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- filter for use in mapping exceptions to HTTP responses. See `ExceptionMappingFilter <exceptions.html#exceptionmappingfilter>`__ for more information.

Global Filters
--------------

Filters by default execute **after** route matching. Meaning, for a given request the URI path is matched in the routing table before executing any filter. If you need to be able to run a filter *before* route matching, you can add the filter via `HttpRouter#filter[T](beforeRouting = true)`, this is especially useful if the filter manually inspects to see if it should apply on a given request URI path that may not exist in the routing table (e.g., is not defined by any controller added to the server).

If you want to apply a filter or filters to **all** added controllers you can do the following:

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

Note that filters -- much like controller routes -- are applied **in the order defined**.

For more information see the `Finagle User\'s Guide <https://twitter.github.io/finagle/guide/index.html>`__ section on `Filters <https://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters>`__.

Per-controller Filters
----------------------

It is also possible to add filters per controller, using `HttpRouter#add[F1 <: HttpFilter, C <: Controller]`.

These filters will apply to all routes in the Controller.

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

Currently, `HttpRouter#add` supports in-lining up to ten (10) filters before a controller. If you need to include more than ten filters please consider combining them with `c.t.finatra.filters.MergedFilter <https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/filters/MergedFilter.scala>`__ in the same manner as `c.t.finatra.http.filters.CommonFilters <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala>`__ then using the combined filter in your call to `HttpRouter#add`.

In all the above usages, we are applying the filter *by type* allowing the framework to instantiate instances of the filters. However all of these methods support passing constructed instances.

Per-route Filters
-----------------

Additionally, you can define filters *inside* of a Controller per-route,
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

As you can see, you can choose to apply the filter either by type or provide an instance. Note that you can chain `Controller#filter` calls arbitrarily deep.

Request Scope
-------------

Guice supports `custom scopes <https://github.com/google/guice/wiki/CustomScopes>`__ in addition to the most common `Singleton and *Unscoped* <https://github.com/google/guice/wiki/Scopes>`__. 

Request scopes are often used to allow injecting classes that change depending  on the incoming request (e.g. the authenticated User). Finatra provides an implementation of *RequestScoped* that works across Finagle
non-blocking threads (`Guice's included @RequestScoped implementation <https://github.com/google/guice/wiki/Scopes#scopes>`__ uses ThreadLocals which will not work with `TwitterUtil <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__ `c.t.util.Future`).

Note: fields added to the Finagle request scope will remain present in threads launched from a Finagle `FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__.

Adding Classes into the Finatra Request Scope
---------------------------------------------

First add a dependency on `com.twitter:inject-request-scope`

Then define a module,

.. code:: scala

    import com.myapp.User
    import com.twitter.finatra.requestscope.RequestScopeBinding
    import com.twitter.inject.TwitterModule

    object UserModule
      extends TwitterModule
      with RequestScopeBinding {

      override def configure() {
        bindRequestScope[User]
      }
    }


Next define a Filter to seed the `User` into the Finatra Request Scope:

.. code:: scala

    class UserFilter @Inject()(
      requestScope: FinagleRequestScope)
      extends SimpleFilter[Request, Response] {

      override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
        val userId = parseUserId(request.cookie)
        val user = User(userId)
        requestScope.seed[User](user)
        service(request)
      }
    }


Next add the `FinagleRequestScopeFilter <https://github.com/twitter/finatra/tree/master/inject/inject-request-scope/src/main/scala/com/twitter/inject/requestscope/FinagleRequestScopeFilter.scala>`__ filter to your
server before the `UserFilter` (shown below with other common filters in a recommended filter order):

.. code:: scala

    class Server extends HttpServer {
      override def configureHttp(router: HttpRouter) {
        router.
          filter[FinagleRequestScopeFilter].
          filter[UserFilter].
          add[MyController1]
        }
    }

Then inject a `User` or a `Provider[User]` wherever you need to access the request scope user. Note, `Provider[User]` must be used when injecting into a Singleton class.

.. code:: scala

    import javax.inject.Provider

    @Singleton
    class MyController @Inject()(
      dao: GroupsDAO,
      user: Provider[User])
      extends Controller {

      get("/") { request: Request =>
        "The incoming user has id " + user.get.id
      }
    }


Using `c.t.finagle.http.Request#ctx`
------------------------------------

Above we saw how to seed classes to the Finatra Request scope using a `Provider[T]`.

However, we recommend *not* seeding with a request-scope `Provider[T]` but instead using Finagle's `c.t.finagle.http.Request#ctx <https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Request.scala#L33>`__.
Internally, we generally use the `Request#ctx` over `Provider[T]` even though we use Guice extensively.

To use the `Request#ctx` technique, first create a `RecordSchema <https://github.com/twitter/util/blob/develop/util-collection/src/main/scala/com/twitter/collection/RecordSchema.scala#L6>`__ `request field <https://github.com/twitter/finagle/blob/develop/finagle-base-http/src/main/scala/com/twitter/finagle/http/Request.scala#L34>`__, a context and a filter which can set the context:

.. code:: scala

    // domain object to set as a RecordSchema field
    case class User(id: Long)

    // create a context
    object UserContext {
      private val UserField = Request.Schema.newField[User]()

      implicit class UserContextSyntax(val request: Request) extends AnyVal {
        def user: User = request.ctx(UserField)
      }

      private[twitter] def setUser(request: Request): Unit = {
        val user = User(1) //Parse user from request headers/cookies/etc.
        request.ctx.update(UserField, user)
      }
    }

    // create a filter
    class UserFilter extends SimpleFilter[Request, Response] {
      override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
        UserContext.setUser(request)
        service(request)
      }
    }


Then to use import the context into scope where you want to access the field. Methods defined in the context will then be available.

.. code:: scala

    // import the UserContext into scope, the method Request#user
    // will now be available on the Request object.
    import UserContext._

    class MyController() extends Controller {
      get("/") { request: Request =>
        "Hi " + request.user.id
      }
    }

.. |rarrow| unicode:: U+02192 .. right arrow