.. _exceptions:

HTTP Exception Mapping
======================

It is recommended that you use exceptions for flow control in your controller and services and rely on the `c.t.finatra.http.exceptions.ExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/exceptions/ExceptionMapper.scala>`__ to convert exceptions into proper HTTP responses.

Why?
----

Exception mapping is meant to provide a server-wide mechanism for handling an exception in a standard manner.

The framework is not proscriptive about this, however. Sometimes it may make sense to catch and handle `FooException` directly in your controller (e.g., convert it to an HTTP response or perform another action). You are
encouraged to do what makes sense for your use case or team.

How?
----

The Finatra framework adds a set of `mappers by default <#default-exception-mappers>`__ to your HttpServer which provide high-level mapping for exceptions. You can register additional mappers or `override the default mappers <#override-default-behavior>`__ altogether.

For instance, if you wanted to map a `java.net.MalformedURLException` to a `400 - BadRequest` response you could create the following ExceptionMapper:

.. code:: scala

    @Singleton
    class MalformedURLExceptionMapper @Inject()(response: ResponseBuilder)
      extends ExceptionMapper[MalformedURLException] {

      override def toResponse(request: Request, exception: MalformedURLException): Response = {
        response.badRequest(s"Malformed URL - ${exception.getMessage}")
      }
    }


Then register this exception mapper in your server.

.. code:: scala

    class MyHttpServer extends HttpServer {

      override def configureHttp(router; HttpRouter): Unit = {
        router
          .exceptionMapper[MalformedURLExceptionMapper]
        ...
      }

      ...
    }

ExceptionMappingFilter
----------------------

Using exception mappers requires you to include the `c.t.finatra.http.filters.ExceptionMappingFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/ExceptionMappingFilter.scala>`__ in your server's filter chain.

**Note**: the `ExceptionMappingFilter` is included as part of `c.t.finatra.http.filters.CommonFilters <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala>`__. Thus if you are using the `c.t.finatra.http.filters.CommonFilters <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala>`__ the ExceptionMappingFilter is already installed.

See `Filters <filters.html#c-t-finatra-http-filters-commonfilters>`__ for more information on the `c.t.finatra.http.filters.CommonFilters`.

To manually add the `ExceptionMappingFilter`:

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import ExampleFilter
    import MalformedURLExceptionMapper
    import com.twitter.finagle.http.{Request, Response}
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.filters.{ExceptionMappingFilter, LoggingMDCFilter, TraceIdMDCFilter}
    import com.twitter.finatra.http.routing.HttpRouter

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override val modules = Seq(
        DoEverythingModule)

      override def configureHttp(router: HttpRouter): Unit = {
        router
          .filter[LoggingMDCFilter[Request, Response]]
          .filter[TraceIdMDCFilter[Request, Response]]
          .filter[ExceptionMappingFilter[Request]]
          .add[ExampleFilter, ExampleController]
          .exceptionMapper[MalformedURLExceptionMapper]
      }
    }


Again, you can see we register the exception mapper *by type* allowing the framework to instantiate an instance.

Default Exception Mappers
-------------------------

The framework adds several mappers to the `ExceptionManager` by default. To swap out any of these defaults simply need add a mapper to the manager for the exception type to map.

As noted above the last registered mapper for a type wins.

By default the framework will add the follow mappers:

==============================  ============================================================================================================================================================================================
`Throwable`                     `ThrowableExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/ThrowableExceptionMapper.scala>`__

`JsonParseException`            `JsonParseExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/json/JsonParseExceptionMapper.scala>`__

`CaseClassMappingException`     `CaseClassExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/json/CaseClassExceptionMapper.scala>`__

`CancelledRequestException`     `CancelledRequestExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/CancelledRequestExceptionMapper.scala>`__

`c.t.finagle.Failure`           `FailureExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/FailureExceptionMapper.scala>`__

`HttpException`                 `HttpExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/HttpExceptionMapper.scala>`__

`HttpResponseException`         `HttpResponseExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/HttpResponseExceptionMapper.scala>`__

`org.apache.thrift.TException`  `ThriftExceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/exceptions/ThriftExceptionMapper.scala>`__
==============================  ============================================================================================================================================================================================

The `ExceptionManager` walks the exception type hierarchy starting at the given exception type moving up the inheritance chain until it finds mapper configured for the type.
In this manner an `ExceptionMapper[Throwable]` will be the last mapper invoked and acts as the "default".

Therefore to change the framework "default" mapper, simply add a new mapper over the `Throwable` type (i.e., `ExceptionMapper[Throwable]`) to the `ExceptionManager`.

Override Default Behavior
-------------------------

The `ExceptionManager <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/exceptions/ExceptionManager.scala>`__ is the class that handles registration of exception mappers.
In the example above, the `HttpRouter#exceptionMapper <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala#L45>`__ method is simply registering the given mapper
with the `ExceptionManager`.

The `ExceptionManager` is configured by the inclusion of the `ExceptionManagerModule <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/modules/ExceptionManagerModule.scala>`__
as a framework module in every `HttpServer <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala#L22>`__.

If a new mapper is added over an exception type already registered in the `ExceptionManager`, the previous mapper will be overwritten.

Thus, the last registered mapper for an exception type wins.

Register an Exception Mapper
----------------------------

There are multiple ways to add a mapper.

Either directly through the `HttpRouter`:

.. code:: scala

      override def configureHttp(router: HttpRouter): Unit = {
        router
          .filter[ExceptionMappingFilter[Request]]
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