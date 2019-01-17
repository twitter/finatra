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

Creating an HTTPS Server
------------------------

Finatra provides a default for the `defaultHttpPort` of `":8888"` which means that Finatra will
always attempt to start a non-ssl HTTP server on port `8888` if no other configuration is done. The
framework allows for users to specify starting an HTTPS server, either additionally or instead.

An HTTPS server can be started by passing in a value for the `-https.port` flag or overriding the
`defaultHttpsPort` with a non-empty value. To configure the underlying Finagle `c.t.finagle.Http.Server`
transport correctly, override the `configureHttpsServer` method in your HttpServer definition. E.g.,

.. code:: scala

    import com.twitter.finagle.Http
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    object ExampleHttpsServerMain extends ExampleHttpsServer

    class ExampleHttpsServer extends HttpServer {

      override val defaultHttpsPort: String = ":443"

      // HTTP server configuration
      override def configureHttpServer(server: Http.Server): Http.Server = {
        server
          .withResponseClassifier(???)
          .withMaxInitialLineSize(???)
      }

      // HTTPS server configuration
      override def configureHttpsServer(server: Http.Server): Http.Server = {
        server
          .withResponseClassifier(???)
          .withMaxInitialLineSize(???)
          .withTransport.tls(???)
      }

      override def configureHttp(router: HttpRouter): Unit = {
        router
          .add[ExampleController]
      }
    }

For convenience, a `Tls <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/Tls.scala>`__
trait is provided which encapsulates standard `TLS <https://en.wikipedia.org/wiki/Transport_Layer_Security>`__
configuration for an HTTPS server. Thus you can also do:

.. code:: scala

    import com.twitter.finagle.Http
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    object ExampleHttpsServerMain extends ExampleHttpsServer

    class ExampleHttpsServer
      extends HttpServer
      with Tls {

      override val defaultHttpsPort: String = ":443"

      override def configureHttp(router: HttpRouter): Unit = {
        router
          .add[ExampleController]
      }
    }

Disabling the Default HTTP Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As mentioned, the above configuration will still attempt to start a non-ssl HTTP server. To disable
the non-ssl HTTP server, override the `defaultHttpPort` value to an empty String (and do not
pass a value for the `-http.port` flag), e.g.,

.. code:: scala

    import com.twitter.finagle.Http
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    object ExampleHttpsServerMain extends ExampleHttpsServer

    class ExampleHttpsServer
      extends HttpServer
      with Tls {

      override val defaultHttpPort: String = "" // disable the default HTTP port
      override val defaultHttpsPort: String = ":443"

      override def configureHttp(router: HttpRouter): Unit = {
        router
          .add[ExampleController]
      }
    }

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

    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    class ExampleServer extends HttpServer {

      override val defaultHttpPort: String = ":8080"

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

    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    class ExampleServer extends HttpServer {

      override def jacksonModule = MyCustomJacksonModule

      override def configureHttp(router: HttpRouter): Unit = {
        ???
      }
    }


If your module is defined as a class, you would pass an instance of the
class, e.g.,

.. code:: scala

    override def jacksonModule = new MyCustomJacksonModule


Finagle Server Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you want to further configure the underlying `Finagle <https://github.com/twitter/finagle>`__ server you can override `configureHttpServer` (or `configureHttpsServer`)
in your server to specify additional configuration on (or override the default configuration of) the underlying Finagle server.

For example:

.. code:: scala

    import com.twitter.finagle.Http
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    class ExampleServer extends HttpServer {

      override def configureHttp(router: HttpRouter): Unit = {
        ???
      }

      override def configureHttpServer(server: Http.Server): Http.Server = {
        server
          .withMaxRequestSize(???)
          .withAdmissionControl.concurrencyLimit(
            maxConcurrentRequests = ???,
            maxWaiters = ???
      }
    }


For more information on `Finagle <https://github.com/twitter/finagle>`__ server configuration see the documentation `here <https://twitter.github.io/finagle/guide/Configuration.html>`__;
specifically the server documentation `here <https://twitter.github.io/finagle/guide/Servers.html>`__.

Server-side Response Classification
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The default Response Classifier for HTTP servers is `HttpResponseClassifier.ServerErrorsAsFailures <https://github.com/twitter/finatra/blob/8b448065f5f74c1eedd744bd15618cbf932ea1bc/http/src/main/scala/com/twitter/finatra/http/response/HttpResponseClassifier.scala#L15>`__,
which classifies any HTTP 5xx response code as a failure. To configure server-side `Response Classification <https://twitter.github.io/finagle/guide/Servers.html#response-classification>`__ you could choose to
set the classifier directly on the underlying Finagle server by overriding the `configureHttpServer` (or `configureHttpsServer`) in your server, e.g.,

.. code:: scala

    override def configureHttpServer(server: Http.Server): Http.Server = {
        server.withResponseClassifier(???)
    }

However, since the server-side ResponseClassifier could affect code not just at the Finagle level, we actually recommend overriding the specific `framework module <#framework-modules>`__,
`HttpResponseClassifierModule` instead. This binds an instance of an `HttpResponseClassifier <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/response/HttpResponseClassifier.scala>`__
to the object graph that is then available to be injected into things like the HTTP `StatsFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/StatsFilter.scala>`__
for a more accurate reporting of metrics that takes into account server-side response classification.

For example, in your `HttpServer` you would do:

.. code:: scala

    import com.google.inject.Module
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    class ExampleServer extends HttpServer {

      override httpResponseClassifierModule: Module = ???
    }

The bound value is also then `set on the underlying Finagle server <https://github.com/twitter/finatra/blob/9d7b430ce469d1542b603938e3ec24cf6ff79d64/http/src/main/scala/com/twitter/finatra/http/internal/server/BaseHttpServer.scala#L150>`__
before serving.

Testing
-------

For information on testing an HTTP server see the HTTP Server `Feature Tests <../testing/feature_tests.html#http-server>`__ section.
