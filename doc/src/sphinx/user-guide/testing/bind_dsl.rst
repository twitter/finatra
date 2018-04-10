.. _bind_dsl:

Explicit Binding with ``#bind[T]``
==================================

Embedded Server ``#bind[T]``
----------------------------

In the cases where we'd like to easily replace a bound instance with another instance in our tests
(e.g. with a mock or a simple stub implementation), we do not need to create a specific module
for testing to compose into our server as an override module. Instead we can use the `#bind[T]`
function on the embedded server.

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
        .bind[DownstreamServiceClient].toInstance(mockDownstreamServiceClient)
        .bind[IdService].toInstance(mockIdService)

      test("service test") {
        /* Mock GET Request performed by DownstreamServiceClient */
        mockDownstreamServiceClient.get("/tweets/123.json")(manifest[FooResponse]) returns Future(None)
        ...
      }

For a complete example, see the
`TwitterCloneFeatureTest <https://github.com/twitter/finatra/blob/develop/examples/twitter-clone/src/test/scala/finatra/quickstart/TwitterCloneFeatureTest.scala>`__.

.. warning::
    Using ``@Bind`` (the `com.google.inject.testing.fieldbinder.Bind` annotation) is to be considered
    deprecated.

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

See the `java-http-server <https://github.com/twitter/finatra/tree/develop/examples/java-http-server>`__
for a full example of using the `#bind[T]` DSL in test to override a binding in a server.

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
