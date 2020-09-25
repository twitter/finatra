.. _mocks:

Working with Mocks
==================

|Mockito|_
----------

Finatra recommends and integrates with the excellent |Mockito|_ Java mocking framework. As mentioned in the main `Testing <./index.html#scalatest>`__ section, it is recommended that Scala users use the `ScalaTest <https://www.scalatest.org/>`__ 
testing framework, however it is also recommended that users prefer |MockitoScala|_ for |Mockito|_ mocking in Scala over the `ScalaTest MockitoSugar <https://www.scalatest.org/user_guide/testing_with_mock_objects#mockito>`__ utility (which provides only basic syntax sugar for |Mockito|_).

|c.t.util.mock.Mockito|_
---------------------

|c.t.util.mock.Mockito|_ provides a |MockitoScala|_ integration for Scala users of the framework. Java
users are encouraged to use |Mockito|_ directly. The |c.t.util.mock.Mockito|_ will work as a full
replacement of the `ScalaTest <https://www.scalatest.org/>`__ `org.scalatestplus.mockito.MockitoSugar <http://doc.scalatest.org/3.0.8/org/scalatestplus/mockito/MockitoSugar.html>`_
trait.

The trait uses |MockitoScala|_ `Idiomatic Mockito <https://github.com/mockito/mockito-scala#idiomatic-mockito>`__
which is heavily influenced by ScalaTest Matchers and provides a mocking DSL similar to the previous
`Specs2 Mockito <https://etorreborre.github.io/specs2/guide/SPECS2-3.4/org.specs2.guide.UseMockito.html>`__
integration.

.. note::

  Usage of the |MockitoScala|_ integration via |c.t.util.mock.Mockito|_ is optional. Users are free to
  choose `ScalaTest <https://www.scalatest.org/>`__ `MockitoSugar <http://doc.scalatest.org/3.0.8/org/scalatestplus/mockito/MockitoSugar.html>`_ or another mocking library entirely for use in writing tests.

Using Mocks
-----------

When working with mocks it is **strongly recommended** that users choose to use `Explicit Binding with #bind[T] <bind_dsl.html>`__
to bind mocks to the object graph of a server or application under test. It is preferable to retain
control over the lifecycle of a mock within a given test rather than binding a mock inside of a
`TwitterModule` passed as an `Override Module <./override_modules.html>`__. Using the
`#bind[T] <bind_dsl.html>`__ DSL allows for the test to retain control of the mock in order to
expect or if necessary, reset, behavior.

To use, first add a dependency on `util/util-mock` and then mix in the `c.t.util.mock.Mockito` trait to your test. E.g.,

.. code:: scala

    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTest
    import com.twitter.util.mock.Mockito

    class ExampleFeatureTest
      extends FeatureTest
      with Mockito {

      private val mockDownstreamServiceClient = mock[DownstreamServiceClient]
      private val mockIdService = mock[IdService]

      override val server =
        new EmbeddedHttpServer(new ExampleServer)
        .bind[DownstreamServiceClient].toInstance(mockDownstreamServiceClient)
        .bind[IdService].toInstance(mockIdService)

      test("service test") {
        /* Mock GET Request performed by DownstreamServiceClient */
        mockDownstreamServiceClient.get("/tweets/123.json")(manifest[FooResponse]) returns Future(None)
        ...
      }

You can then bind the mocked instance to the object graph of the server or application under test
using the `#bind[T] <bind_dsl.html>`__ DSL and then expect behavior for the mocked instance as
necessary.

This is preferable over creating a `Module <../getting-started/modules.html>`__ then binding the
mock to the object graph in the module, e.g.,

.. code:: scala

    import com.twitter.inject.TwitterModule
    import com.twitter.util.mock.Mockito

    // NOT RECOMMENDED
    object MyTestModule extends TwitterModule with Mockito {
      override def configure(): Unit = {
        val mockFoo = mock[Foo]
        mockFoo.doSomething returns "Hello, world!"

        bind[Foo].toInstance(mockFoo)
      }
    }

In the above example, the user of the module has no access to the mock object to be able to expect
behavior of the mock (this is in essence a `stub <https://martinfowler.com/articles/mocksArentStubs.html#TheDifferenceBetweenMocksAndStubs>`__ and you may very well find this useful). 

You could change the code to instead expose passing of the mock object into the module,

.. code:: scala

    import com.twitter.inject.TwitterModule

    class MyTestModule(foo: Foo) extends TwitterModule {
      override def configure(): Unit = {
        bind[Foo].toInstance(foo)
      }
    }

This would then allow a test to create the mock and retain control to be able to expect behavior.
But at this point, the module does not provide much utility as this behavior can be more concisely
done via the `#bind[T] <bind_dsl.html>`__ DSL. 

.. note::

    If you are trying to create reusable functionality and there is some ceremony that needs to be 
    done on the mock and want to encapsulate the ceremony, then the above is a viable path. In practice, 
    we've found this case to be rare and thus generally recommend using the `#bind[T] <bind_dsl.html>`__ DSL 
    inside of a test when mocking.

Resetting Mocks
---------------

.. warning::

    Note that the `Mockito <https://site.mockito.org/>`__ documentation `frowns upon needing to reset
    mocks in a test <https://github.com/mockito/mockito/wiki/FAQ#can-i-reset-a-mock>`__ and in most
    cases when testing a stateless service or application it is not expected that you would need to
    reset a mocked instance between test cases.

Generally, users set up a single server or application as a test member variable then test it against
various cases. When doing so, mocks may be bound to the server or application's object graph
as in the example above and thus there may be occasion where the mocked instances need to be reset 
between test cases. However, if you are using a mock to capture the effects of state changes or transitions
we recommend instead having the state changes emit `Metrics <../twitter-server/stats_receiver.html>`__
and using stat assertions.

If you do need to reset mocks in test, it is recommended that you do so within an override of `afterEach()`
if using Scala or similar functionality with an `@After` annotated method in JUnit if using Java.
The Finatra framework mixes in the ScalaTest `BeforeAndAfterEach <http://doc.scalatest.org/3.1.2/org/scalatest/BeforeAndAfterEach.html>`__
trait, thus the ScalaTest `afterEach()` function is available to override. E.g.,

.. code:: scala

    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTest
    import com.twitter.util.mock.Mockito

    class ExampleFeatureTest
      extends FeatureTest
      with Mockito {

      private val mockDownstreamServiceClient = mock[DownstreamServiceClient]
      private val mockIdService = mock[IdService]

      override val server =
        new EmbeddedHttpServer(new ExampleServer)
        .bind[DownstreamServiceClient].toInstance(mockDownstreamServiceClient)
        .bind[IdService].toInstance(mockIdService)

      override afterEach(): Unit = {
        // c.t.util.mock.Mockito provides a `reset(mocks*)` function
        reset(mockDownstreamServiceClient, mockIdService)
      }

      test("service test") {
        /* Mock GET Request performed by DownstreamServiceClient */
        mockDownstreamServiceClient.get("/tweets/123.json")(manifest[FooResponse]) returns Future(None)
        ...
      }

This will `reset <https://javadoc.io/static/org.mockito/mockito-core/3.3.3/org/mockito/Mockito.html#resetting_mocks>`__
the mocks passed to the `c.t.util.mock.Mockito.reset` function *after* each test case has been run.

.. note::

    In some cases you may want to reset a mock **before** each test case (because you do some pre-work
    with the mock in the constructor of the Test class). While it is generally more common to reset
    mocks **after** each test case, you an always do something similar before each test case by overriding
    the `beforeEach()` ScalaTest method or use an `@Before` annotated method with JUnit.

With |MockitoScala|_
--------------------

Resetting mocks *after* each test is generally considered to be the preferred manner such
that |MockitoScala|_ provides specific ScalaTest helper `traits <https://github.com/mockito/mockito-scala#orgmockitointegrationsscalatestresetmocksaftereachtest--orgmockitointegrationsscalatestresetmocksaftereachasynctest>`__
to do so. Note that as documented, you should use the `org.mockito.scalatest` versions from `mockito-scala-scalatest <https://search.maven.org/artifact/org.mockito/mockito-scala-scalatest_2.12>`__:
  * `org.mockito.scalatest.ResetMocksAfterEachTest <https://github.com/mockito/mockito-scala/blob/release/1.x/scalatest/src/main/scala/org/mockito/scalatest/ResetMocksAfterEachTest.scala>`__
  * `org.mockito.scalatest.ResetMocksAfterEachAsyncTest <https://github.com/mockito/mockito-scala/blob/release/1.x/scalatest/src/main/scala/org/mockito/scalatest/ResetMocksAfterEachAsyncTest.scala>`__

These traits will reset any mock instance created within the test, e.g., `val f = mock[Foo]` within a ScalaTest `afterEach()`. Any mock created differently, e.g., `val f = MockitoSugar.mock[Foo]` will not be reset since only the currently scoped `mock` methods are augmented to collect the mock instances for resetting.

Further Reading
---------------

More resources on `test doubles <http://xunitpatterns.com/Test%20Double.html>`__:

* `Mocks Aren't Stubs <https://martinfowler.com/articles/mocksArentStubs.html>`__

* `Test Doubles — Fakes, Mocks and Stubs. <https://blog.pragmatists.com/test-doubles-fakes-mocks-and-stubs-1a7491dfa3da>`__


More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature_tests`
- :doc:`integration_tests`
- :doc:`startup_tests`
- :doc:`mixins`
- :doc:`override_modules`
- :doc:`bind_dsl`

.. |Mockito| replace:: Mockito
.. _Mockito: https://site.mockito.org/

.. |MockitoScala| replace:: Mockito Scala
.. _MockitoScala: https://github.com/mockito/mockito-scala

.. |c.t.util.mock.Mockito| replace:: `c.t.util.mock.Mockito`
.. _c.t.util.mock.Mockito: https://github.com/twitter/util/blob/develop/util-mock/src/main/scala/com/twitter/util/mock/Mockito.scala


