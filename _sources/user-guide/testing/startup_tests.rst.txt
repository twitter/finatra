.. _startup_tests:

Startup Tests
=============

.. important::

  Please see the section on including test-jar dependencies in your project: `Test Dependencies <../..#test-dependencies>`_.

By default the Finatra embedded testing infrastructure sets the `Guice <https://github.com/google/guice>`__
|com.google.inject.Stage|_ to `DEVELOPMENT` for the object graph of the server or application under
test. For purposes of testing we choose the trade-off of a fast start-up time for the embedded
server at the expense of some runtime performance as classes are lazily loaded when accessed by the
test features.

However, this also means that if you have mis-configured dependencies (e.g., you attempt to inject
a type that the injector cannot construct because it does not have a no-arg constructor nor was it
provided by a module) you may not run into this with regular `FeatureTesting <feature_tests.html>`__
as dependencies are satisfied lazily by default.

As such, we recommend creating a test -- a `StartupTest` to check that your service can start
up and report itself as healthy. This is useful for checking the correctness of the object graph,
catching errors that could otherwise cause the server to fail to start.

Best Practices
--------------

A `StartupTest` should mimic production as closely as possible.

-  The |com.google.inject.Stage|_ SHOULD be set to `PRODUCTION` so that all singletons will be
   eagerly created at startup (`Stage.DEVELOPMENT` is set by default).
-  Avoid replacing any bound types (do not use `Explicit Binding with #bind[T] <bind_dsl.html>`__ or
   `Override Modules <override_modules.html>`__).
-  Prevent any Finagle clients from making outbound connections during startup by setting the
   resolution of clients to `nil`. See: `Disabling Client using Dtabs <feature_tests.html#disabling-clients-using-dtabs>`__.

For example:

.. code:: scala

    import com.google.inject.Stage
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTest
    import scala.collection.immutable.ListMap

    class MyServiceStartupTest extends FeatureTest {
      val server = new EmbeddedHttpServer(
        stage = Stage.PRODUCTION,
        twitterServer = new SampleApiServer,
        globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
        flags = Map(
          "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil")
      )

      test("SampleApiServer#startup") {
        server.assertHealthy()
      }
    }

.. tip::

  This works for |EmbeddedHttpServer|_ or |EmbeddedThriftServer|_ as well because ``#assertHealthy()``
  is defined on the |EmbeddedTwitterServer|_ super class.

For examples of how to accomplish this in Java, see |HelloWorldServerStartupTest|_.

|c.t.server.TwitterServer|_
---------------------------

As seen previously, Finatra’s |c.t.inject.server.FeatureTest|_ utility can be used for testing any
extension of |c.t.server.TwitterServer|_ -- the server under test does not specifically have to
be a server written using the Finatra framework.

Here is an example of writing a StartupTest that starts a |c.t.server.TwitterServer|_ and asserts
that it reports itself “healthy”.

Given a simple |c.t.server.TwitterServer|_:

.. code:: scala

    import com.twitter.finagle.{Http, ListeningServer, Service}
    import com.twitter.finagle.http.{Status, Response, Request}
    import com.twitter.server.TwitterServer
    import com.twitter.util.{Await, Future}

    class MyTwitterServer extends TwitterServer {
      private[this] val httpPortFlag =
        flag(name = "http.port", default = ":8888", help = "External HTTP server port")

      private[this] def responseString: String = "Hello, world!"
      private[this] val service = Service.mk[Request, Response] { request =>
        val response =
          Response(request.version, Status.Ok)
        response.contentString = responseString
        Future.value(response)
      }

      /** Simple way to expose the bound port once the external listening server is started */
      @volatile private[this] var _httpExternalPort: Option[Int] = None
      def httpExternalPort: Option[Int] = this._httpExternalPort

      def main(): Unit = {
        val server: ListeningServer = Http.server
          .withLabel("http")
          .serve(httpPortFlag(), service)
        info(s"Serving on port ${httpPortFlag()}")
        info(s"Serving admin interface on port ${adminPort()}")
        onExit {
          Await.result(server.close())
        }
        this._httpExternalPort = Some(server.boundAddress.asInstanceOf[InetSocketAddress].getPort)
        Await.ready(server)
      }
    }

For examples of how to accomplish this in Java, see |ExampleTwitterServerStartupTest|_.

Writing the StartupTest
~~~~~~~~~~~~~~~~~~~~~~~

First, extend the |c.t.inject.server.FeatureTest|_ trait. Then override the `server` definition
with an instance of your |EmbeddedTwitterServer|_ which wraps your |c.t.server.TwitterServer|_
under test.

.. code:: scala

    import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest, PortUtils}
    import scala.collection.immutable.ListMap

    class MyTwitterServerFeatureTest extends FeatureTest {

      override protected val server =
        new EmbeddedTwitterServer(
          twitterServer = new MyTwitterServer,
          globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
          flags = Map(
            "http.port" -> PortUtils.ephemeralLoopback,
            "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil"
          )
        )

      test("MyTwitterServer#starts") {
        server.isHealthy should be(true)
      }
    }

It is important to note that for a "non-injectable" TwitterServer, i.e., a direct extension of
`c.t.server.TwitterServer`, the above testing assumes that many of your service startup issues
can be determined at class construction, or in the `init` or `premain`
`lifecycle <../getting-started/lifecycle.html#c-t-server-twitterserver-lifecycle>`_ phases.

**Why?**

By default, the `EmbeddedTwitterServer` will start the underlying server in an different thread,
then `wait <https://github.com/twitter/finatra/blob/416cb3467c88e26704d695c1d6b8176172afa9c4/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L692>`_
for the server to `start <https://github.com/twitter/finatra/blob/416cb3467c88e26704d695c1d6b8176172afa9c4/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L684>`_
before allowing a test to proceed. However, this differs when the underlying server is a
`c.t.server.TwitterServer` vs. when it is a `c.t.inject.server.TwitterServer`.

For a `c.t.server.TwitterServer` the `EmbeddedTwitterServer` has no hook to determine if a server
has fully started, so relies solely on the `HTTP Admin Interface <../getting-started/twitter_server.html#http-admin-interface>`_
reporting itself as healthy. Note, therefore, if you configure your server to `disable <https://github.com/twitter/twitter-server/blob/696076263178ffb99d4e61d314e49bb8710c74e3/server/src/main/scala/com/twitter/server/AdminHttpServer.scala#L130>`__ 
the `TwitterServer HTTP Admin Interface <https://twitter.github.io/twitter-server/Admin.html#admin-interface>`__, then you
will not be able to test your server in this manner as the framework will have no way to determine when the server has started.

For a `c.t.inject.server.TwitterServer` the `EmbeddedTwitterServer` is able to wait for the server
to report itself as "started" in the `c.t.inject.app.App#main <https://github.com/twitter/finatra/blob/416cb3467c88e26704d695c1d6b8176172afa9c4/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L135>`_.

Thus, testing your server is healthy for a `c.t.server.TwitterServer` is merely a check against
the `HTTP Admin Interface <../getting-started/twitter_server.html#http-admin-interface>`_
which is started in the `premain` phase.

.. caution::

    If all of your `c.t.server.TwitterServer` logic is contained in the `main` of your server (like
    Finagle client creation, external ListeningServer creation, etc), it is very possible when the
    server under test is started in a separate thread, the TwitterServer `HTTP Admin Interface` will
    start and report that it is healthy, then the test process will exit before the server under
    test in the other thread has gotten to executing its `main` method and thus exiting before
    exercising any logic.

    In cases like this, you should also ensure to test the logic of your server in regular `FeatureTests <feature_tests.html>`__
    and not only assert it is reported as healthy. Again, see the documentation on the
    `Application and Server Lifecycle <../getting-started/lifecycle.html>`_ for more information.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature_tests`
- :doc:`integration_tests`
- :doc:`mixins`
- :doc:`override_modules`
- :doc:`bind_dsl`

.. |c.t.inject.server.FeatureTest| replace:: `c.t.inject.server.FeatureTest`
.. _c.t.inject.server.FeatureTest: https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala

.. |c.t.server.TwitterServer| replace:: `c.t.server.TwitterServer`
.. _c.t.server.TwitterServer: https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala

.. |com.google.inject.Stage| replace:: `com.google.inject.Stage`
.. _com.google.inject.Stage: https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Stage.html

.. |c.t.server.resolverMap| replace:: `c.t.server.resolverMap`
.. _c.t.server.resolverMap: https://github.com/twitter/twitter-server/blob/15e35a3a3070c50168ff55fd83a2dff28b09795c/server/src/main/scala/com/twitter/server/FlagResolver.scala#L9>

.. |EmbeddedTwitterServer| replace:: `EmbeddedTwitterServer`
.. _EmbeddedTwitterServer: https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L264

.. |EmbeddedHttpServer| replace:: `EmbeddedHttpServer`
.. _EmbeddedHttpServer: https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala

.. |EmbeddedThriftServer| replace:: `EmbeddedThriftServer`
.. _EmbeddedThriftServer: https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala

.. |HelloWorldServerStartupTest| replace:: `HelloWorldServerStartupTest`
.. _HelloWorldServerStartupTest: https://github.com/twitter/finatra/blob/develop/examples/http-server/java/src/test/java/com/twitter/finatra/example/HelloWorldServerStartupTest.java

.. |ExampleTwitterServerStartupTest| replace:: `ExampleTwitterServerStartupTest`
.. _ExampleTwitterServerStartupTest: https://github.com/twitter/finatra/blob/develop/examples/injectable-twitter-server/java/src/test/java/com/twitter/finatra/example/ExampleTwitterServerStartupTest.java
