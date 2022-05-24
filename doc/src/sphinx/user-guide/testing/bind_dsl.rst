.. _bind_dsl:

Explicit Binding with ``#bind[T]``
==================================

.. important::

  Please see the section on including test-jar dependencies in your project:
  `Test Dependencies <../..#test-dependencies>`_.

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
    import com.twitter.util.mock.Mockito

    class ExampleFeatureTest
      extends FeatureTest
      with Mockito
      with HttpTest {

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

For a complete example, see the
`TwitterCloneFeatureTest <https://github.com/twitter/finatra/blob/develop/examples/advanced/twitter-clone/src/test/scala/finatra/quickstart/TwitterCloneFeatureTest.scala>`__.

Note this is also available for `EmbeddedApp <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/EmbeddedApp.scala>`__
as well:

.. code:: scala

    import com.twitter.finagle.stats.InMemoryStatsReceiver
    import com.twitter.inject.Test
    import com.twitter.inject.app.EmbeddedApp
    import com.twitter.util.mock.Mockito

    class MyAppTest extends Test with Mockito {
      private val inMemoryStatsReceiver: InMemoryStatsReceiver = new InMemoryStatsReceiver
      private val mockIdService = mock[IdService]

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

As described in the `Integration Tests <./integration_tests.html>`__ section you can use the
|TestInjector|_ to construct a minimal object graph for testing. The `TestInjector` also supports a
`bind[T]` function to let you replace bound instances in the constructed object graph with another
instance, like a mock or stub.

E.g.,

.. code:: scala

    import com.twitter.inject.IntegrationTest

    class ExampleIntegrationTest extends IntegrationTest {
      val mockIdService = mock[IdService]

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
example, see the `DarkTrafficCanonicalResourceHeaderTest <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/darktraffic/test/DarkTrafficCanonicalResourceHeaderTest.scala>`__.

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
    import com.twitter.finatra.http.request.RequestBuilder;
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

    Do not inject members of a test class into the server, application or |TestInjector|_ object
    graph under test.

We strongly **discourage** injecting members of a test via your server, application or
|TestInjector|_. This includes using `@Inject` or `@Bind` (the `com.google.inject.testing.fieldbinder.Bind`
annotation with the `BoundFieldModule <https://github.com/google/guice/wiki/BoundFields>`__) on a
test member field then using the `c.t.inject.Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__
of the server, application or |TestInjector|_ under test to do `injector.underlying.injectMembers(this)`.

.. code:: scala

    class MyIntegrationTest extends IntegrationTest {

      // THIS IS NOT RECOMMENDED
      @Bind(to = classOf[StubBarImp]) private val bar: Bar
      // Foo depends on Bar.
      @Inject private val foo: Foo
      @Bind private val mockDatasource: Datasource = mock[DatasourceImplementation]

      override val injector =
        TestInjector(
          modules = Seq(new OurGreatModule(), BoundFieldModule.of(this)),
        ).create()

        injector.injectMembers(this)

        test("test something") {
          ...
        }
    }

Or you could choose to do the test class member injection in a `beforeAll`:

.. code:: scala

    class MyIntegrationTest extends IntegrationTest {

      // THIS IS NOT RECOMMENDED
      @Bind(to = classOf[StubBarImp]) private val bar: Bar
      // Foo depends on Bar.
      @Inject private val foo: Foo
      @Bind private val mockDatasource = mock[DatasourceImplementation]

      override val injector =
        TestInjector(
          modules = Seq(new OurGreatModule(), BoundFieldModule.of(this)),
        ).create()

      override protected def beforeAll(): Unit = {
        super.beforeAll()
        injector.injectMembers(this)
      }

        test("test something") {
          ...
        }
    }

Or with a `FeatureTest <./feature_tests.html>`__:

.. code:: scala

    class MyFeatureTest extends FeatureTest {
      // THIS IS NOT RECOMMENDED
      @Inject val fakeFilesystem: Filesystem
      @Inject val mockSubsystem: Subsystem

      override val server = new EmbeddedHttpServer(
        twitterServer = new OurHttpTwitterServerUnderTest {
          val modules: Seq[Module] = defaultModules ++ Seq(TestModuleWithTheSystemBindings)
        }
      )

      override protected def beforeAll(): Unit = {
        super.beforeAll()
        server.injector.underlying.injectMembers(this)
      }

      test("test something") {
        ...
      }

    }

Why?
~~~~

You can perhaps see from the above that there is an impedance mismatch from the creation of the
object graph under test via creation of the `Injector` and calling `injectMembers(this)`.

At the very least, this usage tends to violate the encapsulation of the system under test. As
mentioned previously, the object graph under test is created from a stateful `Injector` with a
lifecycle that is managed by the framework that is different from the lifecycle of the test class.
Mixing them in this manner is hard to reason about and can lead to possibly non-deterministic tests.
This usage also makes it harder to test `multiple servers or applications <./feature_tests.html#testing-multiple-applications-or-servers>`__
in a single test file.

If something accesses a value from the `Injector` before `beforeAll()` was called, e.g., another eager
`val` in the constructor of the test class, this could potentially fail, or worse, an incorrect value
could be returned (since the `Injector` will try to instantiate whatever is asked for, relying on
any default no-arg constructor). Moving the `injectMembers(this)` to also be in the test class
constructor can help, but then ordering of calls is important as the `injectMembers(this)` would need
to happen before any access of `Injector` values that are expected to be bound. As such, we have
generally found that in any sufficiently large or complex test suite, this usages tends towards
brittle tests.

Alternatives
~~~~~~~~~~~~

Replacing object graph bindings
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The framework provides ways to safely replace bindings in the object graph under test, properly
taking into account the lifecycle to handle parsing of flag values. Users should prefer to either
provide an `override module <./override_modules.html>`__ or use the `#bind[T]` DSL to replace a
binding **when instantiating** the object graph under test when it is necessary to *replace* a
binding in the object graph under test.

Managing test fixtures
^^^^^^^^^^^^^^^^^^^^^^

One reason for test member injection is to be able to easily create test fixtures. If it is
necessary to use an object graph for managing some set of test fixture instances that are separate
from the server or application, **do not use** the `Injector` from the server, application, nor the
|TestInjector|_ under test. Instead, create a separate `Injector` -- you can use a new
`c.t.inject.app.TestInjector <integration_tests.html#id2>`__ to aid in this.

Thus, we recommend that you explicitly create a separate object graph and obtain any necessary
instances from the appropriate object graph over using the indirection of injecting test members. In
this way, all object graphs are established at instantiation of the test member making it easier to
reason about the test lifecycle.

For example:

.. code:: scala

    class MyIntegrationTest extends IntegrationTest {

      private val mockDatasource = mock[DatasourceImplementation]

      // create an object graph for obtaining test fixtures. `OurIntegrationTestModule`
      // provides bindings for `Foo` and `Bar`
      private[this] val testFixturesInjector =
        TestInjector(
          modules = Seq(new OurIntegrationTestModule())
        )
        .bind[Datasource].toInstance(mockDatasource)
        .create()

      private val foo: Foo = testFixturesInjector.instance[Foo]
      private val bar: Bar = testFixturesInjector.instance[Bar]

      // the object graph under test
      override val injector =
        TestInjector(
          modules = Seq(new OurGreatModule())
        ).create()

      test("test something") {
        ...
      }
    }

Or with a `FeatureTest <./feature_tests.html>`__:

.. code:: scala

    class MyFeatureTest extends FeatureTest {

      // create an object graph for obtaining test fixtures, `OurIntegrationTestModule`
      // provides bindings for `Filesystem` and `Subsystem`
      private[this] val testFixturesInjector =
        TestInjector(
          modules = Seq(new OurIntegrationTestModule())
        ).create()

      private val fakeFilesystem: Filesystem = testFixturesInjector.instance[Filesystem]
      private val mockSubsystem: Subsystem = testFixturesInjector.instance[Subsystem]

      // the object graph under test is encapsulated in the embedded server
      override val server = new EmbeddedHttpServer(
        twitterServer = new OurHttpTwitterServerUnderTest
      )

      test("test something") {
        ...
      }
    }

This is the same tactic taken in testing `multiple servers or applications <./feature_tests.html#testing-multiple-applications-or-servers>`__
in a single test file. Each server or application encapsulates an `Injector` which is established at
instantiation of each server or application within the test. Any necessary instances are then explicitly
obtained from the appropriate object graph for use in testing.

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
- :doc:`mocks`
- :doc:`mixins`
- :doc:`override_modules`

.. |#bind[T]| replace:: `#bind[T]`
.. _#bind[T]: https://github.com/twitter/finatra/tree/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/BindDSL.scala

.. |TestInjector| replace:: `TestInjector`
.. _TestInjector: ./integration_tests.html#id2
