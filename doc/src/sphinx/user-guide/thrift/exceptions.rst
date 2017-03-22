.. _exceptions:

Thrift Exception Mapping
========================

It is recommended in Finatra Scala Framework that you use exceptions for flow control in your controller and services and rely on the `c.t.finatra.thrift.exceptions.ExceptionMapper <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions/ExceptionMapper.scala>`__ to convert exceptions into proper finatra-thrift exceptions or thrift responses.

Look at `Http Exception Mapping <https://twitter.github.io/finatra/user-guide/http/exceptions.html#why>`__ for why the framework provides this.

``Note: Thrift Exception Mapping is only supported for Finatra Scala Framework now.``

How?
----

In order to turn on Exception Mapping, you should include the `c.t.finatra.thrift.filters.ExceptionMappingFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/ExceptionMappingFilter.scala>`__ in your server's filter chain. The Finatra framework adds a `ThrowableExceptionMapper <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions/ThrowableExceptionMapper.scala>`__ by default to ExceptionMappingFilter which provides root-level mapping for exceptions. You can register additional mappers or override the default one altogether.

For instance, if you want to map a `ClassCastException` to a `ThriftException`, such as `ClientError(ClientErrorCause, errorMessage)`, which is defined in `finatra_thrift_exceptions.thrift <https://github.com/twitter/finatra/blob/develop/thrift/src/main/thrift/finatra-thrift/finatra_thrift_exceptions.thrift>`__ . You could create the following ExceptionMapper:

.. code:: scala

  @Singleton
  class ClassCastExceptionMapper extends ExceptionMapper[ClassCastException, ClientError] {

    def handle(throwable: ClassCastException): Future[ClientError] = {
      Future.exception(ClientError(BadRequest, throwable.getMessage))
    }
  }


Then register this exception mapper in your server.

.. code:: scala

    class MyThriftServer extends ThriftServer {

      override def configureThrift(router: ThriftRouter): Unit = {
        router
          .filter[ExceptionMappingFilter]
          .exceptionMapper[ClassCastExceptionMapper]
        ...
      }

      ...
    }

Two more examples mapping exceptions to actual thrift responses are located at `mappers <https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/tests/doeverything/exceptions/mappers.scala>`__.

Also, you can see we register the exception mapper *by type* allowing the framework to instantiate an instance.

Override Default Behavior
-------------------------

The `ExceptionManager <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions/ExceptionManager.scala>`__ is the class that handles registration of exception mappers.
In the example above, the `ThriftRouter#exceptionMapper <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/routing/ThriftRouter.scala#L38>`__ method is simply registering the given mapper
with the `ExceptionManager`.

The `ExceptionManager` is configured by the inclusion of the `ExceptionManagerModule <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/modules/ExceptionManagerModule.scala>`__
as a framework module in every `ThriftServer <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/ThriftServer.scala#L93>`__.

If a new mapper is added over an exception type already registered in the `ExceptionManager`, the previous mapper will be overwritten.

Thus, the user registered mapper for an exception type wins.

Default Exception Mappers
-------------------------

The framework adds only `ThrowableExceptionMapper` to the `ExceptionManager` by default, which simply throws back all uncaught `Throwable` s. The `ExceptionManager` walks the exception type hierarchy starting at the given exception type, moving up the inheritance chain until it finds mapper configured for the type. In this manner, an `ExceptionMapper[Throwable]` will be the last mapper invoked and acts as the "default". Therefore to change the framework "default" mapper, simply add a new mapper over the `Throwable` type (i.e., `ExceptionMapper[Throwable]`) to the `ExceptionManager`.

As noted above the user registered mapper for a type wins.

Finatra framework also provides a `FinatraThriftExceptionMapper` for mapping other exceptions to known ThriftException. If you are also using `finatra_thrift_exceptions.thrift <https://github.com/twitter/finatra/blob/develop/thrift/src/main/thrift/finatra-thrift/finatra_thrift_exceptions.thrift>`__, this mapper is recommended to be registered.


There are two ways to add a mapper.

Either directly through the `ThriftRouter`:

.. code:: scala

      override def configureThrift(router: ThriftRouter): Unit = {
        router
          .filter[ExceptionMappingFilter]
          .exceptionMapper[MyThrowableExceptionMapper]
          .exceptionMapper[OtherExceptionMapper]
      }

Or in a module which is then added to the Server, e.g.,

.. code:: scala

      object MyExceptionMapperModule extends TwitterModule {
        override def singletonStartup(injector: Injector): Unit = {
          val manager = injector.instance[ExceptionManager]
          manager.add[MyThrowableExceptionMapper]
          manager.add[OtherExceptionMapper]
        }
      }

      ...

      override val modules = Seq(
        MyExceptionMapperModule)