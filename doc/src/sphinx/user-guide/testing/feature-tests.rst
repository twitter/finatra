Feature Tests
=============

If you are familiar with `Gherkin <http://docs.behat.org/en/v2.5/guides/1.gherkin.html>`__ or `Cucumber <https://github.com/cucumber/cucumber/wiki/Feature-Introduction>`__ or other similar testing languages and frameworks, then `feature testing <https://wiki.documentfoundation.org/QA/Testing/Feature_Tests>`__ will feel somewhat familiar. In Finatra, a feature test always consists of an app or a server under test. See the `FeatureTest <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala>`__ trait.

We highly recommend writing feature tests for your services as they provide a very good signal of whether you have correctly implemented the features of your service. If you haven't implemented the feature correctly, it almost doesn't matter that you have lots of unit tests.

HTTP Server
-----------

For example, to write a feature test for an HTTP server, extend the `c.t.inject.server.FeatureTest` trait. Then override the `server` with an instance of your |EmbeddedHttpServer|_.

.. code:: scala

    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTest

    class ExampleServerFeatureTest extends FeatureTest {
      override val server = new EmbeddedHttpServer(new ExampleServer)

      test("ExampleServer#perform feature") {
          server.httpGet(
            path = "/",
            andExpect = Status.Ok)
            ...
        }
      }
    }

Thrift Server
-------------

Similarly, to write a feature test for a Thrift server and create a `client <#thrift-tests>`__ to it, extend the `c.t.inject.server.FeatureTest` trait, override the `server` with an instance of your |EmbeddedThriftServer|_, and create a thrift client from the |EmbeddedThriftServer|_.

.. code:: scala

    import com.twitter.finatra.thrift.EmbeddedThriftServer
    import com.twitter.inject.server.FeatureTest

    class ExampleThriftServerFeatureTest extends FeatureTest {
      override val server = new EmbeddedThriftServer(new ExampleThriftServer)

      lazy val client = server.thriftClient[ExampleThrift[Future]](clientId = "client123")

      test("ExampleThriftServer#return data accordingly") {
          Await.result(client.doExample("input")) should equal("output")
        }
      }
    }

Combined HTTP & Thrift Server
-----------------------------

If you are extending both `c.t.finatra.http.HttpServer` **and** `c.t.finatra.thrift.ThriftServer` then you can feature test by constructing an `EmbeddedHttpServer with ThriftClient`, e.g.,

.. code:: scala

    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTest

    class ExampleCombinedServerFeatureTest extends FeatureTest {
      override val server =
        new EmbeddedHttpServer(new ExampleCombinedServer) with ThriftClient

      lazy val client = server.thriftClient[ExampleThrift[Future]](clientId = "client123")

      "ExampleCombinedServer#perform feature") {
          server.httpGet(
            path = "/",
            andExpect = Status.Ok)
            ...
        }

       "ExampleCombinedServer#return data accordingly") {
          Await.result(client.doExample("input")) should equal("output")
        }
      }
    }


.. caution::

  The `server` is specified as a `def` in the |c.t.inject.server.FeatureTestMixin|_ trait. If you only want to start **one instance of your server per test file** make sure to override this `def` with a `val`.

For more advanced examples see:

-  the
   `DoEverythingServerFeatureTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/test/DoEverythingServerFeatureTest.scala>`__
   for an HTTP server.
-  the
   `DoEverythingThriftServerFeatureTest <https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/tests/DoEverythingThriftServerFeatureTest.scala>`__
   for a Thrift server.
-  the
   `DoEverythingCombinedServerFeatureTest <https://github.com/twitter/finatra/blob/develop/inject-thrift-client-http-mapper/src/test/scala/com/twitter/finatra/multiserver/test/DoEverythingCombinedServerFeatureTest.scala>`__
   for "combined" HTTP and Thrift server.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`integration-tests`
- :doc:`startup-tests`
- :doc:`mixins`
- :doc:`mocks`
- :doc:`override-modules`
- :doc:`bind-dsl`

.. Hidden ToC
.. toctree::
   :maxdepth: 2
   :hidden:

   index.rst
   embedded.rst
   integration-tests.rst
   startup-tests.rst
   mixins.rst
   mocks.rst
   override-modules.rst
   bind-dsl.rst



.. |c.t.inject.server.FeatureTestMixin| replace:: `c.t.inject.server.FeatureTestMixin`
.. _c.t.inject.server.FeatureTestMixin: https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTestMixin.scala#L24

.. |EmbeddedHttpServer| replace:: `EmbeddedHttpServer`
.. _EmbeddedHttpServer: https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala

.. |EmbeddedThriftServer| replace:: `EmbeddedThriftServer`
.. _EmbeddedThriftServer: https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala

