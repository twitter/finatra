.. _thrift_Controllers:

Defining Thrift Controllers
===========================

A *Thrift Controller* is an implementation of your thrift service. To create the Controller, extend the `c.t.finatra.thrift.Controller <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/Controller.scala>`__ with the generated thrift service as its argument. Scrooge generates a `GeneratedThriftService` which is a class containing information about the various `ThriftMethods` and types that this service defines. Each `ThriftMethod` defines an `Args` type and `SuccessType` type. When creating a `Controller`, you must provide exactly one implementation for each method defined in your Thrift service using the `handle(ThriftMethod)` DSL.

Implementing methods with `handle(ThriftMethod)`
------------------------------------------------

The Finatra `c.t.finatra.thrift.Controller` provides a DSL with which you can implement your thrift service methods via the `handle(ThriftMethod) <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/Controller.scala#L124>`__ function. Using this DSL, you can apply `TypeAgnostic` `Filters` to handling of methods as well as provide an implementation in the form of a function from `ThriftMethod.Args => Future[ThriftMethod.SuccessType]`, `Request[ThriftMethod.Args] => Future[Response[ThriftMethod.SuccessType]]` or `Service[Request[ThriftMethod.Args], Response[ThriftMethod.Args]]`.

For example, given the following thrift IDL: `example_service.thrift`

::

    namespace java com.twitter.example.thriftjava
    #@namespace scala com.twitter.example.thriftscala
    namespace rb ExampleService

    include "finatra-thrift/finatra_thrift_exceptions.thrift"

    service ExampleService {
      i32 add1(
        1: i32 num
      ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError kClientError
      )
    }


We can implement the following Thrift Controller:

.. code:: scala

    import com.twitter.example.thriftscala.ExampleService
    import com.twitter.finatra.thrift.Controller
    import com.twitter.util.Future

    class ExampleThriftController
      extends Controller(ExampleService) {

      val addFilter: Filter.TypeAgnostic = { ... }

      handle(Add1).filtered(addFilter) { args: Add1.Args =>
        Future(args.num + 1)
      }
    }


The `handle(ThriftMethod)` function may seem magical but it serves an important purpose. By implementing your service method via this function, it allows the framework to apply the configured global filter chain defined in your `server definition <../build-new-thrift-server#server-definition>`__ to your method implementation (passed as the callback to `handle(ThriftMethod)`).

That is to say, the `handle(ThriftMethod)` function captures filters that you apply to that particular method plus your method implementation and then exposes it for the `ThriftRouter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/routing/ThriftRouter.scala>`__ to combine with the configured global filter chain to build the `Finagle Service <https://twitter.github.io/finagle/guide/ServicesAndFilters.html>`__ that represents your server.

See the `Filters <filters.html>`__ section for more information on adding filters to your server definition.

When creating a Controller to handle a `ThriftSerice`, all methods defined in the thrift service must have one and only one implementation - that is, there should be exactly one call to `handle(ThriftMethod)` for each thrift method defined. Anything else will result in the Finatra service failing at runtime.

Scrooge `Request` and `Response` Wrappers
-----------------------------------------
By providing an implementation that is aware of the Scrooge-generated `Request` and `Response` wrappers, header data is available. Using the earlier `ExampleThrift`, we can construct a Controller that examines header information like this:

.. code:: scala

    import com.twitter.example.thriftscala.ExampleService
    import com.twitter.finatra.thrift.Controller
    import com.twitter.util.Future
    import com.twitter.scrooge.{Request, Response}

    class ExampleThriftController extends Controller(ExampleService) {

      handle(Add1).withFn { request: Request[Add1.Args] =>
        val num = request.args.num
        val headers = request.headers

        log(s"Add1 called with $num and headers: $headers")
        Future(Response(num + 1))
      }
    }

Add the Controller to the Server
--------------------------------

As previously shown, the server can then be defined with this Thrift
Controller:

.. code:: scala

    class ExampleServer extends ThriftServer {
      ...
      override def configureThrift(router: ThriftRouter): Unit = {
        router
          .add[ExampleThriftController]
      }
    }


Please note that Finatra only currently supports adding a **single** Thrift Controller to the `ThriftRouter`. The expectation is that you are implementing a single Thrift *service* and thus a single `ThriftService`.

But I don't want to write all of my code inside of one Controller class
-----------------------------------------------------------------------

Don't worry. You don't have to.

The only requirement is a single class which implements the service's defined thrift methods. Nothing specifies that *this* class needs to contain all of your service implementation or logic.

If you want to modularize or componentize to have a better separation of concerns in your code, your `Controller` implementation can be easily written to inject other services or handlers such that complicated logic can be handled in other classes as is generally good practice. E.g.,

.. code:: scala

    class ExampleThriftController @Inject() (
      add1Service: Add1Service,
      add2Service: Add2Service,
    ) extends Controller(ExampleService) {

      // add1Service must be of a unique type for injection but also extends:
      // Service[Request[Add1.Args], Response[Add1.SuccessType]]
      // which is what the withService method is looking for.
      handle(Add1).withService(add1Service)

      handle(Add2).withService(add2Service)
    }

In the above example the `Controller` implementation forwards handling of the various methods to the injected services directly.

How you structure and call other classes from the `Controller` implementation is completely up to you to implement in whatever way makes sense for your service or team.

Per-Method Stats
----------------

Per-method stats recording is provided by Finatra in the `c.t.finatra.thrift.filters.StatsFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/StatsFilter.scala>`__.

.. code:: scala

    import com.twitter.example.thriftscala.ExampleService
    import com.twitter.finatra.thrift.Controller
    import com.twitter.util.Future
    import com.twitter.scrooge.{Request, Response}

    class ExampleThriftController extends Controller(ExampleService) {

      handle(Add1).withFn { request: Request[Add1.Args] =>
        val num = request.args.num
        val headers = request.headers

        log(s"Add1 called with $num and headers: $headers")
        Future(Response(num + 1))
      }
    }


yields the following stats:

::

    per_method_stats/add1/failures 0
    per_method_stats/add1/success 1
    per_method_stats/add1/latency_ms 8.666667 [5.0, 3.0, 2.0]

Deprecated/Legacy Controller Information
----------------------------------------

Prior to constructing a `Controller` by extending `Controller(GeneratedThriftSerivce)`, a Controller was constructed by creating a class that extended `Controller with GeneratedThriftSerivce.BaseServiceIface`. Constructing a Controller this way is still possible but deprecated.

Since a legacy-style `Controller` extends the `BaseServiceIface` directly, it must provide implementations for each of the thrift methods, but it also must still use the `handle(ThriftMethod)` method to make Finatra aware of which methods are being served for reporting and filtering reasons. If this is not done, none of the configured global filters will be applied (including things like per-method stats).

It is important that when constructing the overrides for the `BaseServiceIface`, they must be implemented as a `val` instead of a `def`. If they're `defs`, the service/filters will be re-created for each incoming request, incurring very serious overhead.

Legacy style Controllers cannot use per-method filtering or have access to headers via Scrooge's `Request` and `Response` types.

A properly configured legacy-style Controller looks like this:

.. code:: scala

    import com.twitter.example.thriftscala.ExampleService
    import com.twitter.finatra.thrift.Controller
    import com.twitter.util.Future

    class ExampleThriftController
      extends Controller with ExampleService.BaseServiceIface {

      // Note that this is a val instead of a def
      override val add1 = handle(Add1) { args: Add1.Args =>
        Future(args.num + 1)
      }
    }


More information
----------------

For more information, see the `Finagle Integration <https://twitter.github.io/scrooge/Finagle.html>`__ section of the `Scrooge <https://twitter.github.io/scrooge/index.html>`__ documentation.
