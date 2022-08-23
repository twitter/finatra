.. _http_server:


Note: the most up-to-date examples are located in the `finatra/examples <https://github.com/twitter/finatra/tree/master/examples>`__ project.

See `examples/http-server <https://github.com/twitter/finatra/tree/develop/examples/http-server>`__ for a simple example HTTP Server.

HTTP Server Definition
======================

To start, add a dependency on the `finatra-http-server <https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20a%3A%22finatra-http-server_2.12%22>`__ library. We also recommend using `Logback <https://logback.qos.ch/>`__ as your `SLF4J <https://www.slf4j.org/manual.html>`__ implementation. E.g.,

with sbt:

.. parsed-literal::

    "com.twitter" %% "finatra-http-server" % "\ |release|\ ",
    "ch.qos.logback" % "logback-classic" % versions.logback,

For more information on logging with Finatra see: `Introduction to Logging With Finatra <../logging/index.html#introduction-to-logging-with-finatra>`__.

Create a new class which extends `c.t.finatra.http.HttpServer <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/HttpServer.scala>`__:

.. code:: scala

    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override def configureHttp(router: HttpRouter): Unit = {
        ???
      }
    }

Or in Java, extend the `c.t.finatra.http.AbstractHttpServer <https://github.com/twitter/finatra/blob/100b858ca2786199114247e1440a1bfb4a460b28/http-server/src/main/scala/com/twitter/finatra/http/servers.scala#L505>`__:

.. code:: java

    import com.twitter.app.Flaggable;
    import com.twitter.finatra.http.AbstractHttpServer;
    import com.twitter.finatra.http.routing.HttpRouter;

    public class ExampleServer extends AbstractHttpServer {

      public ExampleServer() {
        createFlag(
          /* name      = */ "magic.number",
          /* default   = */ 55,
          /* help      = */ "This is a magic number.",
          /* flaggable = */ Flaggable.ofJavaInteger());
      }

      @Override
      public void configureHttp(HttpRouter router) {
        ...
      }
    }

.. tip::

  See the server `naming conventions <#naming-convention>`__ which is especially important for Java users.

A more complete example includes adding Modules, a Controller, and Filters. In Scala:

.. code:: scala

    import com.google.inject.Module
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.filters.{LoggingMDCFilter, TraceIdMDCFilter}
    import com.twitter.finatra.http.routing.HttpRouter

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override val modules: Seq[Module] = Seq(
        ExampleModule)

      override def configureHttp(router: HttpRouter): Unit = {
        router
          .filter[LoggingMDCFilter[Request, Response]]
          .filter[TraceIdMDCFilter[Request, Response]]
          .filter[ExampleFilter]
          .add[ExampleController]
      }
    }

in Java:

.. code:: java

    import com.google.inject.Module;
    import com.google.inject.TypeLiteral;
    import com.twitter.app.Flaggable;
    import com.twitter.finatra.http.AbstractHttpServer;
    import com.twitter.finatra.http.routing.HttpRouter;
    import com.twitter.finatra.http.filters.LoggingMDCFilter;
    import com.twitter.finatra.http.filters.StatsFilter;
    import com.twitter.finatra.http.filters.TraceIdMDCFilter;
    import java.util.Collection;
    import java.util.Collections;
    import scala.reflect.ManifestFactory;

    public class ExampleServer extends AbstractHttpServer {

      public ExampleServer() {
          createFlag(
            /* name      = */ "magic.number",
            /* default   = */ 55,
            /* help      = */ "This is a magic number.",
            /* flaggable = */ Flaggable.ofJavaInteger());
      }

      @Override
      public Collection<Module> javaModules() {
          return Collections.singletonList(
            ExampleModule$.MODULE$);
      }

      @Override
      public void configureHttp(HttpRouter httpRouter) {
          httpRouter
              .filter(new TypeLiteral<LoggingMDCFilter<Request, Response>>(){})
              .filter(ManifestFactory.classType(TraceIdMDCFilter.class))
              .filter(new TypeLiteral<StatsFilter<Request>>() {})
              .filter(ExampleFilter.class)
              .add(ExampleController.class);
      }
    }


.. tip::

  Take a look at the server `naming conventions <#naming-convention>`__ which is especially important for Java users.

Adding filters in Java can happen in multiple ways which is somewhat dependent on your tolerance to
type erasure if you need to apply Filters that have `parameterized types <https://github.com/google/guice/wiki/FrequentlyAskedQuestions#how-to-inject-class-with-generic-type>`__.

Above, we show how you can choose to use the `.filter(typeLiteral: TypeLiteral[T])` method from the
`HttpRouter` which will obtain the Filter instance described by the `TypeLiteral[T] <https://github.com/google/guice/wiki/BuiltInBindings#typeliterals>`__
and append it to the filter chain. There is a version which also accepts a `binding annotation <../getting-started/binding_annotations.html>`__
which is useful if you need to construct a Filter that is not only parameterized but also discriminated
by a `binding annotation <../getting-started/binding_annotations.html>`__. **This is the recommended
way to configure generically typed Filters.**

Or you can choose to use the `scala.reflect.ManifestFactory <https://www.scala-lang.org/api/2.12.4/scala/reflect/ManifestFactory$.html>`__
to pass the Manifest type for a Filter class. Note in the usage above, the `TraceIdMDCFilter <https://twitter.github.io/finatra/scaladocs/com/twitter/finatra/http/filters/TraceIdMDCFilter.html>`__
has two type params, `[Req,Rep]`. They are not specified when building the Manifest which amounts
to the types being erased. In this case, this is OK since we are a.) appending the instance
to a chain which will adapt the types appropriately and b.) we don't expect to obtain the Filter
instance from the injector anywhere else in the code (which would need to be asked for as just
`TraceIdMDCFilter` and not `TraceIdMDCFilter[Request, Response]`).

Finally, you can always pass the class of the Filter to append. The instance will be obtained from
the injector and appended to the filter chain.

.. tip::

    Note: to add `Modules <../getting-started/modules.html>`__ to your Java server override the
    `javaModules()` method.

Simplistically, a server can be thought of as a collection of `controllers <controllers.html>`__ composed with `filters <filters.html>`__.
Additionally, a server can define `modules <../getting-started/modules.html>`__ for providing instances to the object graph and how to `map exceptions <../http/exceptions.html>`__ to HTTP responses.

Naming Convention
-----------------

The Finatra convention for defining a server is to create a Scala `object <https://twitter.github.io/scala_school/basics2.html#object>`__ 
with a name ending in "Main" that extends your server class. The server *class* can be used in 
testing as this allows your server to be instantiated multiple times in tests without worrying about 
static state persisting across test runs in the same JVM.

.. code:: scala

    object ExampleServerMain extends ExampleServer

The static object, e.g., `ExampleServerMain`, would then contain a static `main()` method for the server
to serve as the `application entry point <https://docs.oracle.com/javase/tutorial/deployment/jar/appman.html>`__
for running the server in all other cases.

Java Naming Convention
~~~~~~~~~~~~~~~~~~~~~~

Similarly, in Java the best practice is to create a separate "main" class which defines a Java `main() <https://docs.oracle.com/javase/tutorial/getStarted/application/index.html#MAIN>`__ method that accepts the Java `command-line arguments <https://docs.oracle.com/javase/tutorial/essential/environment/cmdLineArgs.html#:~:text=A%20Java%20application%20can%20accept,when%20the%20application%20is%20launched.&text=When%20an%20application%20is%20launched,an%20array%20of%20String%20s.>`__ for flag parsing:

.. code:: java

    public final class ExampleServerMain {
        private ExampleServerMain() {
        }

        public static void main(String[] args) {
            new ExampleServer().main(args);
        }
    }

The `ExampleServerMain` would be the class used as the `application entry point <https://docs.oracle.com/javase/tutorial/deployment/jar/appman.html>`__
for running the server.

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

For convenience, a `Tls <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/Tls.scala>`__
trait is provided which encapsulates standard `TLS <https://en.wikipedia.org/wiki/Transport_Layer_Security>`__
configuration for an HTTPS server. Thus you can also do:

.. code:: scala

    import com.twitter.finagle.Http
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.Tls
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
    import com.twitter.finatra.http.Tls
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

Serving a Finagle `Service[Request, Response]`
----------------------------------------------

You have the option to serve a manually constructed Finagle `Service[Request, Response]` instead of
creating an `HTTP Controller <./controllers.html>`_ and having the framework construct a
`Service[Request, Response]` via the `HttpRouter`.

To do so, extend the `HttpServerTrait` (or `AbstractHttpServerTrait` in Java) and implement the
`httpService: Service[Request, Response]` method:

.. code:: scala

    import com.google.inject.Module
    import com.twitter.finagle.Service
    import com.twitter.finatra.http.HttpServerTrait
    import com.twitter.finatra.http.filters.{LoggingMDCFilter, TraceIdMDCFilter}

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServerTrait {

      override val modules: Seq[Module] = Seq(
        ExampleModule)

      // the `Service[Request, Response]` to serve
      override def httpService: Service[Request, Response] = ???
    }

in Java:

.. code:: java

    import com.google.inject.Module;
    import com.twitter.finagle.Service;
    import com.twitter.finagle.Request;
    import com.twitter.finagle.Response;
    import com.twitter.finatra.http.AbstractHttpServer;
    import java.util.Collections;
    import java.util.Collection;

    public class ExampleServer extends AbstractHttpServer {

      @Override
      public Collection<Module> javaModules() {
        return Collections.singletonList(ExampleModule$.MODULE$);
      }

      @Override
      public Service<Request, Response> httpService() {
        // the `Service<Request, Response>` to serve
        return null;
      }
    }

.. important::

    The `HttpRouter` exposes a DSL for users which the framework uses to construct a filtered
    `Service[Request, Response]`.

    Note, you must choose one or the other: either implement your service with a
    `Controller <./controllers.html>`_ added via the `HttpRouter` **or** serve a manually
    constructed Finagle `Service[Request, Response]`.

Override Default Behavior
-------------------------

Flags
~~~~~

Some deployment environments may make it difficult to set `Flag values <../getting-started/flags.html>`__ with command line arguments. If this is the case, Finatra's `HttpServer <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/HttpServer.scala>`__'s
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


For a list of what flags can be set programmatically, please see the `HttpServerTrait <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/servers.scala>`__ class.

Framework Modules
~~~~~~~~~~~~~~~~~

The `HttpServer <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/servers.scala>`__
provides some base configurations in the form of `modules <../getting-started/modules.html>`_ added
by default to a server's object graph. This includes:

- the `FileResolverModule <https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/modules/FileResolverModule.scala>`_ (see: `Working With Files <../files/index.html>`_)
- the `ExceptionManagerModule <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/modules/ExceptionManagerModule.scala>`_ (see: `HTTP Exception Mapping <./exceptions.html>`_)
- an `overridable <https://github.com/twitter/finatra/blob/356f242a8b9a340374646ae577efa99f132125cb/http/src/main/scala/com/twitter/finatra/http/servers.scala#L363>`__ default `HttpResponseClassifierModule <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/modules/HttpResponseClassifierModule.scala>`_ (see: `Server-side Response Classification <#id2>`_)
- an `overridable <https://github.com/twitter/finatra/blob/356f242a8b9a340374646ae577efa99f132125cb/http/src/main/scala/com/twitter/finatra/http/servers.scala#L508>`__ default `AccessLogModule <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/modules/AccessLogModule.scala>`_
- an `overridable <https://github.com/twitter/finatra/blob/356f242a8b9a340374646ae577efa99f132125cb/http/src/main/scala/com/twitter/finatra/http/servers.scala#L526>`__ default `ScalaObjectMapperModule <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala>`_ (see: `Jackson Integration <../json/index.html>`_)
- an `overridable <https://github.com/twitter/finatra/blob/bfb6e22c9260eb3150b6768d6628ee6b3498183c/http/src/main/scala/com/twitter/finatra/http/servers.scala#L539>`__ default `ValidatorModule <https://github.com/twitter/finatra/blob/bfb6e22c9260eb3150b6768d6628ee6b3498183c/validation/src/main/scala/com/twitter/finatra/validation/ValidatorModule.scala>`_ (see: `Validation Framework <../validation/index.html>`_)
- an `overridable <https://github.com/twitter/finatra/blob/356f242a8b9a340374646ae577efa99f132125cb/http/src/main/scala/com/twitter/finatra/http/servers.scala#L519>`__ default `MessageBodyManagerModule <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/modules/MessageBodyModule.scala>`_ (see: `Message Body Components <./message_body.html>`_)

As expressed above, some of the `modules <../getting-started/modules.html>`_ provided in the `HttpServer <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/servers.scala>`__
are overridable. An example use-case would be to provide a custom `ScalaObjectMapperModule`
implementation in place of the default `ScalaObjectMapperModule <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala>`_

To do so you would override the `protected def jacksonModule` in your server with your custom
implementation.

.. code:: scala

    import com.fasterxml.jackson.databind.PropertyNamingStrategy
    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    object MyCustomJacksonModule extends ScalaObjectMapperModule {
      override val propertyNamingStrategy: PropertyNamingStrategy =
          new PropertyNamingStrategy.KebabCaseStrategy
    }

    ...

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

    import com.fasterxml.jackson.databind.PropertyNamingStrategy
    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

    class MyCustomJacksonModule extends ScalaObjectMapperModule {
      override val propertyNamingStrategy: PropertyNamingStrategy =
          new PropertyNamingStrategy.KebabCaseStrategy
    }

    ...

    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    class ExampleServer extends HttpServer {

      override def jacksonModule = new MyCustomJacksonModule

      override def configureHttp(router: HttpRouter): Unit = {
        ???
      }
    }

.. caution::

    Modules are de-duplicated before being installed to create the Injector. If a Framework
    Module is also configured in the server's `list of Modules <../getting-started/modules.html#module-configuration-in-servers>`_,
    the Framework Module will be replaced.

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
`HttpResponseClassifierModule` instead. This binds an instance of an `HttpResponseClassifier <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/response/HttpResponseClassifier.scala>`__
to the object graph that is then available to be injected into things like the HTTP `StatsFilter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/filters/StatsFilter.scala>`__
for a more accurate reporting of metrics that takes into account server-side response classification.

For example, in your `HttpServer` you would do:

.. code:: scala

    import com.google.inject.Module
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    class ExampleServer extends HttpServer {

      override httpResponseClassifierModule: Module = ???
    }

The bound value is also then set on the underlying Finagle server before serving.

Testing
-------

For information on testing an HTTP server see the HTTP Server `Feature Tests <../testing/feature_tests.html#http-server>`__ section.
