.. _thrift_controllers:

Defining Thrift Controllers
===========================

A *Thrift Controller* is an implementation of your thrift service. To create the controller, extend the `c.t.finatra.thrift.Controller <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/Controller.scala>`__ trait and mix-in the `Scrooge <https://twitter.github.io/scrooge/>`__-generated `BaseServiceIface` trait for your service. Scrooge generates a `ServiceIface` which is a case class containing a `Service` for each thrift method over the corresponding `Args` and `SuccessType` structures for the method that extends from the `BaseServiceIface` trait. E.g,

.. code:: scala

    case class ServiceIface(
      fetchBlob: Service[FetchBlob.Args, FetchBlob.SuccessType]
    ) extends BaseServiceIface


For Thrift Controllers we use the `BaseServiceIface` trait since we are not able to extend the `ServiceIface` case class.

`handle(ThriftMethod)` DSL
--------------------------

The Finatra `c.t.finatra.thrift.Controller` provides a DSL with which you can easily implement your thrift service methods via the `handle(ThriftMethod) <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/Controller.scala#L12>`__ function which takes a callback from `ThriftMethod.Args => Future[ThriftMethod.SuccessType]`.

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
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
      )
    }


We can implement the following Thrift Controller:

.. code:: scala

    import com.twitter.example.thriftscala.ExampleService
    import com.twitter.finatra.thrift.Controller
    import com.twitter.util.Future

    class ExampleThriftController
      extends Controller
      with ExampleService.BaseServiceIface {

      override val add1 = handle(Add1) { args: Add1.Args =>
        Future(args.num + 1)
      }
    }


The `handle(ThriftMethod)` function may seem magical but it serves an important purpose. By implementing your service method via this function, it allows the framework to apply the configured filter chain defined in your `server definition <../build-new-thrift-server#server-definition>`__ to your method implementation (passed as the callback to `handle(ThriftMethod)`).

That is to say, the `handle(ThriftMethod)` function captures your method implementation then exposes it for the `ThriftRouter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/routing/ThriftRouter.scala>`__ to combine with the configured filter chain to build the `Finagle Service <https://twitter.github.io/finagle/guide/ServicesAndFilters.html>`__ that represents your server.

See the `Filters <filters.html>`__ section for more information on adding filters to your server definition.

Note, in the example above we implement the `ExampleService.BaseServiceIface#add1` method to satisfy the `ExampleService.BaseServiceIface` interface -- however, the framework will not call the `add1` method in this way as it uses the implementation of the thrift method captured by the `handle(ThriftMethod)` function (as mentioned above this in order to apply the configured filter chain to requests). Thus if you were to directly call `ExampleThriftController.add1(request)` this would by-pass any configured `filters <filters.html>`__ from the server definition.

Ensure you override using `val`
-------------------------------

You will see above that we use `override val` since the computed `ThriftMethodService <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/internal/ThriftMethodService.scala>`__ instance returned `is effectively constant <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/Controller.scala#L19>`__. However, you MUST override as a `val` when using the `handle(ThriftMethod)` function as using a `def` here will cause indeterminate behavior that will be hard to debug.

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


Please note that Finatra only currently supports adding a **single** Thrift controller to the `ThriftRouter`. The expectation is that you are implementing a single Thrift *service* and thus a single `BaseServiceIface` which is implementable in a single controller.

But I don't want to write all of my code inside of one Controller class
-----------------------------------------------------------------------

Don't worry. You don't have to.

The only requirement is a single class which implements the service's `BaseServiceIface`. Nothing specifies that *this* class needs to contain all of your service implementation or logic.

If you want to modularize or componentize to have a better separation of concerns in your code, your `BaseServiceIface` implementation can be easily written to inject other services or handlers such that complicated logic can be handled in other classes as is generally good practice. E.g.,

.. code:: scala

    class ExampleThriftController @Inject() (
      add1Service: Add1Service,
      add2Service: Add2Service,
    ) extends Controller
      with ExampleService.BaseServiceIface {

          override val add1 = handle(Add1) { args: Add1.Args =>
            add1Service.add1(args)
          }

          override val add2 = handle(Add2) { args: Add2.Args =>
            add2Service.add2(args)
          }
        }

In the above example the `BaseServiceIface` implementation merely calls the methods of other classes to provide the service's Thrift Controller method implementations.

How you structure and call other classes from the `BaseServiceIface` implementation is completely up to you to implement in whatever way makes sense for your service or team.

More information
----------------

For more information, see the `Finagle Integration <https://twitter.github.io/scrooge/Finagle.html>`__ section of the `Scrooge <https://twitter.github.io/scrooge/index.html>`__ documentation.