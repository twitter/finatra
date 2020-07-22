.. _bind_dsl:

Explicit Binding with ``#bind[T]``
==================================

.. important::

  Please see the section on including test-jar dependencies in your project: `Test Dependencies <../..#test-dependencies>`_.

Embedded ``#bind[T]``
----------------------------

In the cases where we'd like to easily replace a bound instance with another instance in our tests
(e.g. with a mock or a simple stub implementation), we do not need to create a specific module
for testing to compose into our server as an override module. Instead we can use the `#bind[T]`
function on the embedded server.

This is especially useful if you want to replace a bound instance with a mock under control of the
test. 

.. code:: scala

    import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
    import com.twitter.inject.server.FeatureTest
    import com.twitter.inject.Mockito

    class ExampleFeatureTest
      extends FeatureTest
      with Mockito
      with HttpTest {

      private val mockDownstreamServiceClient = smartMock[DownstreamServiceClient]
      private val mockIdService = smartMock[IdService]

      override val server =
        new EmbeddedHttpServer(new ExampleServer)
        .bind[DownstreamServiceClient].toInstance(mockDownstreamServiceClient)
        .bind[IdService].toInstance(mockIdService)

      test("service test") {
        /* Mock GET Request performed by DownstreamServiceClient */
        mockDownstreamServiceClient.get("/tweets/123.json")(manifest[FooResponse]) returns Future(None)
        ...
      }

For a complete example, see the
`TwitterCloneFeatureTest <https://github.com/twitter/finatra/blob/develop/examples/advanced/twitter-clone/src/test/scala/finatra/quickstart/TwitterCloneFeatureTest.scala>`__.

Note this is also available for `EmbeddedApp <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/EmbeddedApp.scala>`__ as well:

.. code:: scala

    import com.twitter.finagle.stats.InMemoryStatsReceiver
    import com.twitter.inject.{Mockito, Test}
    import com.twitter.inject.app.EmbeddedApp

    class MyAppTest extends Test with Mockito {
      private val inMemoryStatsReceiver: InMemoryStatsReceiver = new InMemoryStatsReceiver
      private val mockIdService = smartMock[IdService]

      // build an EmbeddedApp
      def app(): EmbeddedApp = app(new MyApp)
      def app(underlying: MyApp): EmbeddedApp = 
        new EmbeddedApp(underlying)
          .bind[IdService].toInstance(mockIdService)
          .bind[StatsReceiver].toInstance(inMemoryStatsReceiver)

      test("assert count") {
        val undertest = app()
        undertest.main("username" -> "jack")

        val statsReceiver = undertest.injector.instance[StatsReceiver].asInstanceOf[InMemoryStatsReceiver]
        statsReceiver.counter("my_counter")() shouldEqual 1
      }
    }

TestInjector ``#bind[T]``
-------------------------

As described in the `Integration Tests <#integration_tests>`__ section you can use the `TestInjector`
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
          .bind[IdService].toInstance(mockIdService)
          .create

      test("MyTest#perform feature") {
        ...
      }
    }

In this example, the bound `IdService` would be replaced with the `mockIdService`. For a more complete
example, see the `DarkTrafficCanonicalResourceHeaderTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/darktraffic/test/DarkTrafficCanonicalResourceHeaderTest.scala>`__.

|#bind[T]|_ DSL
---------------

The primary DSL can be expressed as such (and is similar to the Guice `Linked <https://github.com/google/guice/wiki/LinkedBindings>`__
and `Instance <https://github.com/google/guice/wiki/InstanceBindings>`__ bindings):

.. code:: scala

    bind[T].to[U <: T]
    bind[T].to[Class[U <: T]]
    bind[T].toInstance(T)

    bind[T].annotatedWith[Ann].to[U <: T]
    bind[T].annotatedWith[Ann].to[Class[U <: T]]
    bind[T].annotatedWith[Ann].toInstance(T)

    bind[T].annotatedWith[Class[Ann]].to[U <: T]
    bind[T].annotatedWith[Class[Ann]].to[Class[U <: T]]
    bind[T].annotatedWith[Class[Ann]].toInstance(T)

    bind[T].annotatedWith(Annotation).to[U <: T]
    bind[T].annotatedWith(Annotation).to[Class[U <: T]]
    bind[T].annotatedWith(Annotation).toInstance(T)

    bindClass(Class[T]).to[T]
    bindClass(Class[T]).to[Class[U <: T]]
    bindClass(Class[T]).toInstance(T)

    bindClass(Class[T]).annotatedWith[Class[Ann]].to[T]
    bindClass(Class[T]).annotatedWith[Class[Ann]].[Class[U <: T]]
    bindClass(Class[T]).annotatedWith[Class[Ann]].toInstance(T)

    bindClass(Class[T]).annotatedWith(Annotation).to[T]
    bindClass(Class[T]).annotatedWith(Annotation).[Class[U <: T]]
    bindClass(Class[T]).annotatedWith(Annotation).toInstance(T)

Usage from Java
---------------

The `#bind[T]` DSL also provides several Java-friendly methods for binding:

.. code:: scala

    bindClass(Class[T], T)
    bindClass(Class[T], Annotation, T)
    bindClass(Class[T], Class[Annotation], T)

    bindClass(Class[T], Class[U <: T])
    bindClass(Class[T], Annotation, Class[U <: T])
    bindClass(Class[T], Class[Annotation], Class[U <: T])

Example:

.. code:: java

    import java.util.Collections;

    import com.google.inject.Stage;

    import org.junit.AfterClass;
    import org.junit.Assert;
    import org.junit.BeforeClass;
    import org.junit.Test;

    import com.twitter.finagle.http.Request;
    import com.twitter.finagle.http.Response;
    import com.twitter.finagle.http.Status;
    import com.twitter.finatra.http.EmbeddedHttpServer;
    import com.twitter.finatra.httpclient.RequestBuilder;
    import com.twitter.inject.annotations.Flags;

    public class HelloWorldServerFeatureTest extends Assert {

        private static final EmbeddedHttpServer SERVER = setup();

        private static EmbeddedHttpServer setup() {
            EmbeddedHttpServer server = new EmbeddedHttpServer(
                new HelloWorldServer(),
                Collections.emptyMap(),
                Stage.DEVELOPMENT);

            server.bindClass(Integer.class, Flags.named("magic.number"), 42);
            server.bindClass(Integer.class, Flags.named("module.magic.number"), 9999);
            return server;
        }

        ...

        /** test magicNum endpoint */
        @Test
        public void testMagicNumEndpoint() {
            Request request = RequestBuilder.get("/magicNum");
            Response response = SERVER.httpRequest(request);
            assertEquals(Status.Ok(), response.status());
            assertEquals("42", response.contentString());
        }
    }

See the `java-http-server <https://github.com/twitter/finatra/tree/develop/examples/http-server/java/src/main/java>`__
for a full example of using the `#bind[T]` DSL in test to override a binding in a server.

Injecting Members of a Test
---------------------------

.. warning::

    Do not inject members of a test class into the server or application under test.

We strongly discourage injecting members of a test into your server or application. This includes using `@Inject` or 
`@Bind` (the com.google.inject.testing.fieldbinder.Bind annotation) on a test member field then using the server or 
application's `c.t.inject.Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__ to do `injector.underlying.injectMembers(this)`.

Why?
~~~~

The server or application under test has a different lifecycle than the test class. Mixing them in this manner is first,
hard to reason about and second, can lead to possibly non-deterministic tests. You will also make it hard to test 
`multiple servers or applications <./feature_tests.html#testing-multiple-applications-or-servers>`__ in a single test file.

Lastly, but perhaps most importantly, the `Injector` of a server or application under test is meant to be encapsulated to that server or application. Any mutation of its object graph should happen within the constraints of its lifecycle and thus you should not mutate its object graph after it has started. The framework thus provides ways to do this safely taking into account the server
or application's lifecyle: either provide an `override module <./override_modules.html>`__ or use the `#bind[T]` DSL here
to replace a binding **when instantiating** the server or application under test.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature_tests`
- :doc:`integration_tests`
- :doc:`startup_tests`
- :doc:`mocks`
- :doc:`mixins`
- :doc:`override_modules`

.. |#bind[T]| replace:: `#bind[T]`
.. _#bind[T]: https://github.com/twitter/finatra/tree/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/BindDSL.scala
