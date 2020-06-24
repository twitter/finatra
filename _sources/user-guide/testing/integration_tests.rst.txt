.. _integration_tests:

Integration Tests
=================

.. important::

  Please see the section on including test-jar dependencies in your project: `Test Dependencies <../..#test-dependencies>`_.

.. note:: If you are calling an |c.t.util.Await|_ function on a |c.t.util.Future|_ return type in a
    test, it is generally considered good practice to ensure that your |c.t.util.Await|_ call
    includes a timeout duration, e.g., |c.t.util.Await#ready|_.

`c.t.inject.app.TestInjector <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/TestInjector.scala>`_
-----------------------------------------------------------------------------------------------------------------------------------------------------------

Whereas `Feature Tests <feature_tests.html>`__ start a server or app under test (thereby loading its
entire object graph), integration tests generally only test across a few interfaces in the system.
In Finatra, we provide the `c.t.inject.app.TestInjector <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/TestInjector.scala>`__
which allows you to pass it a set of `TwitterModules` and `Flags` to construct a minimal object graph.

.. important::

    Because `TwitterModules <../getting-started/modules.html>`_ `differ from regular <../getting-started/modules.html#differences-with-google-guice-modules>`_
    `Guice Modules <https://github.com/google/guice/wiki/GettingStarted#guice-modules>`_, it is
    important to use the `c.t.inject.app.TestInjector` for creating an object graph over a set of
    `TwitterModules`.

    The `TestInjector` properly handles lifecycle execution and `Flag` parsing.

    Manually creating a `c.t.inject.Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`_
    over a raw `Guice Injector <https://github.com/google/guice/wiki/GettingStarted#guice-injectors>`_
    created from `TwitterModules` will skip both the lifecycle functions and `Flag` parsing of any
    `Flag` instances defined in the set of `TwitterModules`.

To write an integration test, extend the `c.t.inject.IntegrationTest` trait. Then override the
`injector` val with your constructed instance of on a `c.t.inject.Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`_
created from the `c.t.inject.app.TestInjector`.

You'll then be able to access instances of necessary classes to execute tests.

.. code:: scala

    import com.twitter.inject.Injector
    import com.twitter.inject.IntegrationTest
    import com.twitter.inject.app.TestInjector

    class ExampleIntegrationTest extends IntegrationTest {
      override val injector: Injector =
        TestInjector(
          flags = Map("foo.flag" -> "meaningfulValue"),
          modules = Seq(ExampleModule)
        ).create

      test("MyTest#perform feature") {
        val exampleThingy = injector.instance[ExampleThingy]
        ...
      }
    }


.. caution::

  The `injector` is specified as a `def` the in |c.t.inject.IntegrationTestMixin|_ trait. If you
  only want to start **one instance of your injector per test file** make sure to override this
  `def` with a `val`.

`bind[T]` DSL
-------------

Note that the `c.t.inject.app.TestInjector` also supports the `bind[T]` DSL for overriding
bound types. See the `bind[T]` `documentation <./bind_dsl.html#testinjector-bind-t>`_ for more
information.

Http Tests
----------

If you are writing a test that has an HTTP server under test, you can also extend the
|c.t.finatra.http.HttpTest|_ trait. This trait provides some common utilities for HTTP testing,
specifically utilities for constructing a |resolverMap|_ flag value for setting on your server under
test.

Thrift Tests
------------

Thrift servers can be tested through a |c.t.finatra.thrift.ThriftClient|_. The Finatra test
framework provides an easy way get access to a real `Finagle client <https://twitter.github.io/finagle/guide/Clients.html>`__
for making calls to your running server in a test.

See the `Feature Tests - Thrift Server <feature_tests.html#thrift-server>`__ section for more
information on creating a Finagle Thrift client.

Additionally, your test can also extend the |c.t.finatra.thrift.ThriftTest|_ trait which provides a
utility specifically for constructing a |resolverMap|_ flag value for setting on your server under
test.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature_tests`
- :doc:`startup_tests`
- :doc:`mixins`
- :doc:`mocks`
- :doc:`override_modules`
- :doc:`bind_dsl`


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

.. |c.t.util.Await| replace:: `c.t.util.Await`
.. _c.t.util.Await: https://github.com/twitter/util/blob/54f314d1f4b37d302f685e99b1ac416e48532a04/util-core/src/main/scala/com/twitter/util/Awaitable.scala#L77

.. |c.t.util.Future| replace:: `c.t.util.Future`
.. _c.t.util.Future: https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala

.. |c.t.util.Await#ready| replace:: `c.t.util.Await#ready`
.. _c.t.util.Await#ready: https://github.com/twitter/util/blob/54f314d1f4b37d302f685e99b1ac416e48532a04/util-core/src/main/scala/com/twitter/util/Awaitable.scala#L127

