.. _http_server:


Note: the most up-to-date examples are located in the `finatra/examples <https://github.com/twitter/finatra/tree/master/examples>`__ project.

See `examples/hello-world <https://github.com/twitter/finatra/tree/develop/examples/hello-world>`__ for a simple example HTTP Server.

HTTP Server Definition
======================

To start, add a dependency on the `finatra-http <http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20a%3A%22finatra-http_2.12%22>`__ library. We also recommend using `Logback <http://logback.qos.ch/>`__ as your `SLF4J <http://www.slf4j.org/manual.html>`__ implementation. E.g.,

with sbt:

.. parsed-literal::

    "com.twitter" %% "finatra-http" % "\ |release|\ ",
    "ch.qos.logback" % "logback-classic" % versions.logback,

For more information on logging with Finatra see: `Introduction to Logging With Finatra <../logging/index.html#introduction-to-logging-with-finatra>`__.

Create a new class which extends `c.t.finatra.http.HttpServer <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala>`__:

.. code:: scala

    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override def configureHttp(router: HttpRouter): Unit = {
        ???
      }
    }


A more complete example includes adding Modules, a Controller, and Filters.

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
    import com.twitter.finatra.http.routing.HttpRouter

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override val modules = Seq(
        DoEverythingModule)

      override def configureHttp(router: HttpRouter): Unit = {
        router
          .filter[LoggingMDCFilter[Request, Response]]
          .filter[TraceIdMDCFilter[Request, Response]]
          .filter[CommonFilters]
          .add[ExampleController]
      }
    }


Simplistically, a server can be thought of as a collection of `controllers <controllers.html>`__ composed with `filters <filters.html>`__.
Additionally, a server can define `modules <../getting-started/modules.html>`__ for providing instances to the object graph and how to `map exceptions <../http/exceptions.html>`__ to HTTP responses.

Naming Convention
-----------------

The Finatra convention is to create a Scala `object <https://twitter.github.io/scala_school/basics2.html#object>`__ with a name ending in "Main" that extends your server class.
The server class can be used in testing as this allows your server to be instantiated multiple times in tests without worrying about static state persisting across test runs in the same JVM.
The static object, e.g., `ExampleServerMain`, which contains the static main method for the server would then be used as the `application entry point <https://docs.oracle.com/javase/tutorial/deployment/jar/appman.html>`__ for running the server in all other cases.

Override Default Behavior
-------------------------

Flags
~~~~~

Some deployment environments may make it difficult to set `Flag values <../getting-started/flags.html>`__ with command line arguments. If this is the case, Finatra's `HttpServer <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala>`__'s 
core flags can be set from code. 

For example, instead of setting the `-http.port` flag, you can override the following method in your server.

.. code:: scala

    class ExampleServer extends HttpServer {

      override val defaultFinatraHttpPort: String = ":8080"

      override def configureHttp(router: HttpRouter): Unit = {
        ...
      }
    }


For a list of what flags can be set programmatically, please see the `BaseHttpServer <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/server/BaseHttpServer.scala>`__ class.

Framework Modules
~~~~~~~~~~~~~~~~~

You can override some of the modules provided by default in `HttpServer <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala>`__.

An example use-case would be to provide a custom Jackson module implementation in place of the default `FinatraJacksonModule <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala>`__.

To do so you would override the `protected def jacksonModule` in your server.

.. code:: scala

    class ExampleServer extends HttpServer {

      override def jacksonModule = MyCustomJacksonModule

      override def configureHttp(router: HttpRouter): Unit = {
        ...
      }
    }


If your module is defined as a class, you would pass an instance of the
class, e.g.,

.. code:: scala

    override def jacksonModule = new MyCustomJacksonModule


Finagle Server Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you want to further configure the underlying `Finagle <https://github.com/twitter/finagle>`__ server you can override `configureHttpServer` (or `configureHttpsServer`) in your server and to additional configuration on, or override the default configuration of the underlying Finagle server.

For example:

.. code:: scala

    class ExampleServer extends HttpServer {

      override def configureHttp(router: HttpRouter): Unit = {
        ...
      }

      override def configureHttpServer(server: Http.Server): Http.Server = {
        server
          .withMaxRequestSize(...)
          .withAdmissionControl.concurrencyLimit(
            maxConcurrentRequests = ...,
            maxWaiters = ...
      }
    }


For more information on `Finagle <https://github.com/twitter/finagle>`__ server configuration see the documentation `here <https://twitter.github.io/finagle/guide/Configuration.html>`__; specifically the server documentation `here <https://twitter.github.io/finagle/guide/Servers.html>`__.