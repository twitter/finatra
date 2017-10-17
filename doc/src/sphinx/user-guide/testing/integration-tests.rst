Integration Tests
=================

Whereas `Feature Tests <#feature-tests>`__ start a server or app under test (thereby loading its
entire object graph), integration tests generally only test across a few interfaces in the system.
In Finatra, we provide the `c.t.inject.app.TestInjector <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/TestInjector.scala>`__
which allows you to pass it a set of modules and flags to construct a minimal object graph.

To write an integration test, extend the `c.t.inject.IntegrationTest` trait. Then override the
`injector` val with your constructed instance of `c.t.inject.app.TestInjector`. You'll then be able
to access instances of necessary classes to execute tests.

.. code:: scala

    import com.twitter.inject.IntegrationTest

    class ExampleIntegrationTest extends IntegrationTest {
      override val injector =
        TestInjector(
          flags =
            Map("foo.flag" -> "meaningfulValue"),
          modules =
            Seq(ExampleModule))
          .create

      test("MyTest#perform feature") {
        val exampleThingy = injector.instance[ExampleThingy]
        ...
      }
    }


.. caution::

  The `injector` is specified as a `def` the in |c.t.inject.IntegrationTestMixin|_ trait. If you
  only want to start **one instance of your injector per test file** make sure to override this
  `def` with a `val`.

Http Tests
^^^^^^^^^^

If you are writing a test that has an HTTP server under test, you can also extend the
|c.t.finatra.http.HttpTest|_ trait. This trait provides some common utilities for HTTP testing,
specifically utilities for constructing a |resolverMap|_ flag value for setting on your server under
test.

Thrift Tests
^^^^^^^^^^^^

As shown above, thrift servers can be tested through a |c.t.finatra.thrift.ThriftClient|_. The
Finatra test framework provides an easy way get access to a real `Finagle client <https://twitter.github.io/finagle/guide/Clients.html>`__
for making calls to your running server in a test.

In the case here, creating a |c.t.finatra.thrift.ThriftClient|_ requires the thrift service type
`T`. This type is expected to be the trait subclass of `c.t.scrooge.ThriftService` in the form of
`YourService[+MM[_]]`.

Additionally, your test can also extend the |c.t.finatra.thrift.ThriftTest|_ trait which provides a
utility specifically for constructing a |resolverMap|_ flag value for setting on your server under
test.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature-tests`
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
   feature-tests.rst
   startup-tests.rst
   mixins.rst
   mocks.rst
   override-modules.rst
   bind-dsl.rst


.. |c.t.inject.IntegrationTestMixin| replace:: `c.t.inject.IntegrationTestMixin`
.. _c.t.inject.IntegrationTestMixin: https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-core/src/test/scala/com/twitter/inject/IntegrationTestMixin.scala#L27

.. |c.t.finatra.http.HttpTest| replace:: `c.t.finatra.http.HttpTest`
.. _c.t.finatra.http.HttpTest: https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/HttpTest.scala

.. |c.t.finatra.thrift.ThriftClient| replace:: `c.t.finatra.thrift.ThriftClient`
.. _c.t.finatra.thrift.ThriftClient: https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala

.. |c.t.finatra.thrift.ThriftTest| replace:: `c.t.finatra.thrift.ThriftTest`
.. _c.t.finatra.thrift.ThriftTest: https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftTest.scala

.. |resolverMap| replace:: `resolverMap`
.. _resolverMap: https://github.com/twitter/twitter-server/blob/15e35a3a3070c50168ff55fd83a2dff28b09795c/server/src/main/scala/com/twitter/server/FlagResolver.scala#L9>
