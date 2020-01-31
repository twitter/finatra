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
provided by a module) you may not run into this error during testing as dependencies are satisfied
lazily by default.

As such, we recommend creating a simple test -- a `StartupTest` to check that your service can start
up and report itself as healthy. This checks the correctness of the object graph, catching errors
that could otherwise cause the server to fail to start.

.. important::

  A `StartupTest` should mimic production as closely as possible.

  -  The |com.google.inject.Stage|_ SHOULD be set to `PRODUCTION` so that all singletons will be
     eagerly created at startup (`Stage.DEVELOPMENT` is set by default).
  -  Avoid replacing any bound types (using `Explicit Binding with #bind[T] <bind_dsl.html>`__ or
     `Override Modules <override_modules.html>`__).
  -  Prevent any Finagle clients from making outbound connections during startup by setting all
     |c.t.server.resolverMap|_ entries to `nil! <https://github.com/twitter/finagle/blob/f970bd5b0c1b3f968694dcde33b47b21869b9f0e/finagle-core/src/main/scala/com/twitter/finagle/Resolver.scala#L82>`__.

For example:

.. code:: scala

    import com.google.inject.Stage
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTest

    class MyServiceStartupTest extends FeatureTest {
      val server = new EmbeddedHttpServer(
        stage = Stage.PRODUCTION,
        twitterServer = new SampleApiServer,
        flags = Map(
          "com.twitter.server.resolverMap" -> "some-thrift-service=nil!"
        ))

      test("SampleApiServer#startup") {
        server.assertHealthy()
      }
    }


.. tip::

  This works for |EmbeddedHttpServer|_ or |EmbeddedThriftServer|_ as ``#assertHealthy()`` is defined on
  the |EmbeddedTwitterServer|_ super class.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature_tests`
- :doc:`integration_tests`
- :doc:`mixins`
- :doc:`override_modules`
- :doc:`bind_dsl`

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


