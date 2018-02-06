.. _feature_tests:

Feature Tests
=============

.. note:: If you are calling an |c.t.util.Await|_ function on a |c.t.util.Future|_ return type in a
    test, it is generally considered good practice to ensure that your |c.t.util.Await|_ call
    includes a timeout duration, e.g., |c.t.util.Await#ready|_.

If you are familiar with `Gherkin <http://docs.behat.org/en/v2.5/guides/1.gherkin.html>`__ or
`Cucumber <https://github.com/cucumber/cucumber/wiki/Feature-Introduction>`__ or other similar
testing languages and frameworks, then `feature testing <https://wiki.documentfoundation.org/QA/Testing/Feature_Tests>`__
will feel somewhat familiar. In Finatra, a feature test always consists of an app or a server under
test. See the `FeatureTest <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala>`__
trait.

We highly recommend writing feature tests for your services as they provide a very good signal of
whether you have correctly implemented the features of your service. If you haven't implemented the
feature correctly, it almost doesn't matter that you have lots of unit tests.

HTTP Server
-----------

For example, to write a feature test for an HTTP server, extend the `c.t.inject.server.FeatureTest`
trait. Then override the `server` definition with an instance of your |EmbeddedHttpServer|_.

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

Similarly, to write a feature test for a Thrift server and create a `Finagle <https://twitter.github.io/finagle/>`__
`client <#thrift-tests>`__ to it, extend the `c.t.inject.server.FeatureTest` trait, override the
`server` definition with an instance of your |EmbeddedThriftServer|_, and then create a Thrift client
from the |EmbeddedThriftServer|_.

.. code:: scala

    import com.example.thriftscala.ExampleThrift
    import com.twitter.conversions.time._
    import com.twitter.finatra.thrift.EmbeddedThriftServer
    import com.twitter.inject.server.FeatureTest
    import com.twitter.util.Await

    class ExampleThriftServerFeatureTest extends FeatureTest {
      override val server = new EmbeddedThriftServer(new ExampleThriftServer)

      lazy val client: ExampleThrift[Future] =
        server.thriftClient[ExampleThrift[Future]](clientId = "client123")

      test("ExampleThriftServer#return data accordingly") {
        Await.result(client.doExample("input"), 2.seconds) should equal("output")
      }
    }

Client Interface Types
~~~~~~~~~~~~~~~~~~~~~~

As mentioned in the Scrooge `Finagle Integration <https://twitter.github.io/scrooge/Finagle.html>`__
documentation, users have three API choices for building an interface to a Finagle Thrift client —
``ServicePerEndpoint``, ``ReqRepServicePerEndpoint``, and ``MethodPerEndpoint``. This is true even
when creating a test Thrift client to a Thrift server.

In the example above, we create a Thrift client in the form of the higher-kinded type interface,
e.g., `MyService[+MM[_]]`. We could choose to create a `ExampleThrift.MethodPerEndpoint`
interface instead by changing the type parameter given to the |c.t.finatra.thrift.ThriftClient#thriftClient[T]|_
method:

.. code:: scala

    lazy val client: ExampleThrift.MethodPerEndpoint =
      server.thriftClient[ExampleThrift.MethodPerEndpoint](clientId = "client123")

Users can also choose to create a `service-per-endpoint` Thrift client interface by calling the
|c.t.finatra.thrift.ThriftClient#servicePerEndpoint[T]|_ with either the ``ServicePerEndpoint`` or
``ReqReServicePerEndpoint`` type. E.g.,

.. code:: scala

    lazy val client: ExampleThrift.ServicePerEndpoint =
      server.servicePerEndpoint[ExampleThrift.ServicePerEndpoint](clientId = "client123")

or

.. code:: scala

    lazy val client: ExampleThrift.ReqRepServicePerEndpoint =
      server.servicePerEndpoint[ExampleThrift.ReqRepServicePerEndpoint](clientId = "client123")

Lastly, the Thrift client can also be expressed as a ``MethodPerEndpoint`` wrapping a
`service-per-endpoint` type by using |c.t.finatra.thrift.ThriftClient#methodPerEndpoint[T, U]|_.
This would allow for applying a set of filters on the Thrift client interface before interacting
with the Thrift client as a ``MethodPerEndpoint`` interface.

For example:

.. code:: scala

    lazy val servicePerEndpoint: ExampleThrift.ServicePerEndpoint =
      server
        .servicePerEndpoint[ExampleThrift.ServicePerEndpoint](clientId = "client123")
        .filtered(???)

    lazy val client: ExampleThrift.MethodPerEndpoint =
      server.methodPerEndpoint[
        ExampleThrift.ServicePerEndpoint,
        ExampleThrift.MethodPerEndpoint](servicePerEndpoint)

See the `Communicate with a Thrift Service <../thrift/clients.html>`__ section for more information
on Thrift clients.

Combined HTTP & Thrift Server
-----------------------------

If you are extending both `c.t.finatra.http.HttpServer` **and** `c.t.finatra.thrift.ThriftServer`
then you can feature test by constructing an `EmbeddedHttpServer with ThriftClient`, e.g.,

.. code:: scala

    import com.example.thriftscala.ExampleThrift
    import com.twitter.conversions.time._
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.finatra.thrift.ThriftClient
    import com.twitter.inject.server.FeatureTest

    class ExampleCombinedServerFeatureTest extends FeatureTest {
      override val server =
        new EmbeddedHttpServer(new ExampleCombinedServer) with ThriftClient

      lazy val client: ExampleThrift[Future] =
        server.thriftClient[ExampleThrift[Future]](clientId = "client123")

      "ExampleCombinedServer#perform feature") {
          server.httpGet(
            path = "/",
            andExpect = Status.Ok)
            ...
        }

       "ExampleCombinedServer#return data accordingly") {
          Await.result(client.doExample("input"), 2.seconds) should equal("output")
        }
      }
    }


.. caution::

  The `server` is specified as a `def` in the |c.t.inject.server.FeatureTestMixin|_ trait.

  If you only want to start **one instance** of your server per test file make sure to override this
  `def` with a `val`.

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
- :doc:`integration_tests`
- :doc:`startup_tests`
- :doc:`mixins`
- :doc:`mocks`
- :doc:`override_modules`
- :doc:`bind_dsl`


.. |c.t.inject.server.FeatureTestMixin| replace:: `c.t.inject.server.FeatureTestMixin`
.. _c.t.inject.server.FeatureTestMixin: https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTestMixin.scala#L24

.. |EmbeddedHttpServer| replace:: `EmbeddedHttpServer`
.. _EmbeddedHttpServer: https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala

.. |EmbeddedThriftServer| replace:: `EmbeddedThriftServer`
.. _EmbeddedThriftServer: https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala

.. |c.t.finatra.thrift.ThriftClient#thriftClient[T]| replace:: `c.t.finatra.thrift.ThriftClient#thriftClient[T]`
.. _c.t.finatra.thrift.ThriftClient#thriftClient[T]: https://github.com/twitter/finatra/blob/72664be4439da4425dfe63fa325f4c1ebbc5bf4b/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala#L77

.. |c.t.finatra.thrift.ThriftClient#servicePerEndpoint[T]| replace:: `c.t.finatra.thrift.ThriftClient#servicePerEndpoint[T]`
.. _c.t.finatra.thrift.ThriftClient#servicePerEndpoint[T]: https://github.com/twitter/finatra/blob/72664be4439da4425dfe63fa325f4c1ebbc5bf4b/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala#L103

.. |c.t.finatra.thrift.ThriftClient#methodPerEndpoint[T, U]| replace:: `c.t.finatra.thrift.ThriftClient#methodPerEndpoint[T, U]`
.. _c.t.finatra.thrift.ThriftClient#methodPerEndpoint[T, U]: https://github.com/twitter/finatra/blob/72664be4439da4425dfe63fa325f4c1ebbc5bf4b/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala#L134

.. |c.t.util.Await| replace:: `c.t.util.Await`
.. _c.t.util.Await: https://github.com/twitter/util/blob/54f314d1f4b37d302f685e99b1ac416e48532a04/util-core/src/main/scala/com/twitter/util/Awaitable.scala#L77

.. |c.t.util.Future| replace:: `c.t.util.Future`
.. _c.t.util.Future: https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala

.. |c.t.util.Await#ready| replace:: `c.t.util.Await#ready`
.. _c.t.util.Await#ready: https://github.com/twitter/util/blob/54f314d1f4b37d302f685e99b1ac416e48532a04/util-core/src/main/scala/com/twitter/util/Awaitable.scala#L127
