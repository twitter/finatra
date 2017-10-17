Explicit Binding with ``#bind[T]``
==================================

Embedded Server ``#bind[T]``
----------------------------

In the cases where we'd like to easily replace a bound instance with another instance in our tests
(e.g. with a mock or a simple stub implementation), we do not need to create a specific module
for testing to compose into our server as an override module. Instead we can use the `bind[T]`
function on the embedded server, eg. `c.t.inject.server.EmbeddedTwitterServer#bind <https://github.com/twitter/finatra/blob/92bfb74ecf7b299adb6602847ca9daa89895f3af/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L138>`__.

.. code:: scala

    import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
    import com.twitter.inject.server.FeatureTest
    import com.twitter.inject.Mockito

    class ExampleFeatureTest
      extends FeatureTest
      with Mockito
      with HttpTest {

      val mockDownstreamServiceClient = smartMock[DownstreamServiceClient]
      val mockIdService = smartMock[IdService]

      override val server =
        new EmbeddedHttpServer(new ExampleServer)
        .bind[DownstreamServiceClient](mockDownstreamServiceClient)
        .bind[IdService](mockIdService)

      test("service test") {
        /* Mock GET Request performed by DownstreamServiceClient */
        mockDownstreamServiceClient.get("/tweets/123.json")(manifest[FooResponse]) returns Future(None)
        ...
      }

For a complete example, see the
`TwitterCloneFeatureTest <https://github.com/twitter/finatra/blob/develop/examples/twitter-clone/src/test/scala/finatra/quickstart/TwitterCloneFeatureTest.scala>`__.

Using ``@Bind`` (the `com.google.inject.testing.fieldbinder.Bind` annotation) is to be considered
deprecated.

TestInjector ``#bind[T]``
-------------------------

As described in the `Integration Tests <#integration-tests>`__ section you can use the `TestInjector`
to construct a minimal object graph for testing. The `TestInjector` also supports a `bind[T]` function
to let you easily replace bound instances in the constructed object graph with another instance, like
a mock or stub.

E.g.,

.. code:: scala

    import com.twitter.inject.IntegrationTest

    class ExampleIntegrationTest extends IntegrationTest {
      val mockIdService = smartMock[IdService]

      override val injector =
        TestInjector(
          flags =
            Map("foo.flag" -> "meaningfulValue"),
          modules =
            Seq(ExampleModule, IdServiceModule))
          .bind[IdService](mockIdService)
          .create

      test("MyTest#perform feature") {
        ...
      }
    }

In this example, the bound `IdService` would be replaced with the `mockIdService`. For a more complete
example, see the `DarkTrafficCanonicalResourceHeaderTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/darktraffic/test/DarkTrafficCanonicalResourceHeaderTest.scala>`__.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature-tests`
- :doc:`integration-tests`
- :doc:`startup-tests`
- :doc:`mocks`
- :doc:`mixins`
- :doc:`override-modules`

.. Hidden ToC
.. toctree::
   :maxdepth: 2
   :hidden:

   index.rst
   embedded.rst
   feature-tests.rst
   integration-tests.rst
   startup-tests.rst
   mocks.rst
   mixins.rst
   override-modules.rst
