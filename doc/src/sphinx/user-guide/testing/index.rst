.. _testing:

.. image:: http://imgs.xkcd.com/comics/exploits_of_a_mom.png

Testing With Finatra
====================

Finatra provides the following testing features:

-  the ability to start a locally running server, issue requests, and assert responses.
-  the ability to easily replace class implementations throughout the object graph.
-  the ability to retrieve classes in the object graph to perform assertions on them.
-  the ability to write powerful tests without deploying test code to production.

Types of Tests
--------------

What are we talking about when we talk about *testing*? At a high-level
the philosophy of testing in Finatra revolves around the following
definitions:

-  `Feature Tests`_ - the most powerful tests enabled
   by Finatra. These tests allow you to verify feature requirements of
   the service by exercising its external interface. Finatra supports
   both “black-box testing” and “white-box testing” against a locally
   running version of your server. You can selectively swap out certain
   classes, insert mocks, and perform assertions on internal state. It’s
   worth noting that we sometimes re-use these tests for regression
   testing in larger “System Tests” that run post-release on live
   services. Take a look at an example feature test
   `here <https://github.com/twitter/finatra/blob/develop/examples/hello-world/src/test/scala/com/twitter/hello/HelloWorldFeatureTest.scala>`__.
-  `Integration Tests`_ - similar to feature
   tests, but the entire service is not started. Instead, a list of
   `modules <../getting-started/modules.html>`__ are loaded
   and then method calls and assertions are performed at the
   class-level. You can see an example integration test
   `here <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/marshalling/CallbackConverterIntegrationTest.scala>`__.
-  Unit Tests - these are tests of a single class and since constructor
   injection is used throughout the framework, Finatra stays out of your
   way.

`ScalaTest <http://www.scalatest.org/>`__
-----------------------------------------

The Finatra testing framework is in transition from the `WordSpec <http://doc.scalatest.org/3.0.0/#org.scalatest.WordSpec>`__
ScalaTest `testing style <http://www.scalatest.org/user_guide/selecting_a_style>`__ to `FunSuite <http://doc.scalatest.org/3.0.0/#org.scalatest.FunSuite>`__
for framework testing and to facilitate the types of testing outlined above we have several testing traits to aid in creating simple andpowerful tests.

For more information on `ScalaTest <http://www.scalatest.org/>`__, see the `ScalaTest User Guide <http://www.scalatest.org/user_guide>`__.

To make use of another ScalaTest test style, such as `FunSpec <http://doc.scalatest.org/3.0.0/#org.scalatest.FunSpec>`__ 
or others, see `Test Mixins`_.

Embedded Servers and Apps
-------------------------

Finatra provides a way to run an embedded version of your service or app running locally on ephemeral ports. This allows you to run *actual* requests against an *actual* version of your server when testing. Embedding is an especially powerful way of running and testing your application through an IDE, e.g., like `IntelliJ <https://www.jetbrains.com/idea/>`__.

The embedded utilities are also useful for testing and debugging your code when prototyping. If your service or API makes calls to other services, instead of mocking out or overriding those dependencies with dummy implementations you can always write a test using an Embedded version of your server which talks to *real* downstream services (of course you'd never want to commit a test like this to your source repository, especially if you run any type of `continuous integration <https://en.wikipedia.org/wiki/Continuous_integration>`__ system). You'll be able to run this test normally through the test runner of an IDE which would allow you to easily set breakpoints and step-through code for debugging. As opposed to needing to build and run your service locally and attach a remote debugger.

See:

-  `c.t.inject.app.EmbeddedApp <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/EmbeddedApp.scala>`__
-  `c.t.inject.server.EmbeddedTwitterServer <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala>`__
-  `c.t.finatra.http.EmbeddedHttpServer <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala>`__
-  `c.t.finatra.thrift.EmbeddedThriftServer <https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala>`__


.. image:: ../_static/embedded.png

You'll notice that this hierarchy generally follows the server class
hierarchy as
`c.t.finatra.http.HttpServer <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala>`__
and
`c.t.finatra.thrift.ThriftServer <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/ThriftServer.scala>`__
extend from
`c.t.server.TwitterServer <https://github.com/twitter/twitter-server/blob/develop/src/main/scala/com/twitter/server/TwitterServer.scala>`__
which extends from
`c.t.app.App <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala>`__.

Test Helper Classes
-------------------

.. image:: ../_static/test-classes.png

Feature Tests
^^^^^^^^^^^^^

If you are familiar with `Gherkin <http://docs.behat.org/en/v2.5/guides/1.gherkin.html>`__ or `Cucumber <https://github.com/cucumber/cucumber/wiki/Feature-Introduction>`__ or other similar testing languages and frameworks, then `feature testing <https://wiki.documentfoundation.org/QA/Testing/Feature_Tests>`__ will feel somewhat familiar. In Finatra, a feature test always consists of an app or a server under test. See the `FeatureTest <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala>`__ trait.

We highly recommend writing feature tests for your services as they provide a very good signal of whether you have correctly implemented the features of your service. If you haven't implemented the feature correctly, it almost doesn't matter that you have lots of unit tests.

For example, to write a feature test for an HTTP server, extend the `c.t.inject.server.FeatureTest` trait. Then override the `server` with an instance of your `EmbeddedHttpServer <#embedded-servers-and-apps>`__.

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


Similarly, to write a feature test for a Thrift server and create a `client <#thrift-tests>`__ to it,

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


Notes:
~~~~~~  

The `server` is specified as a `def` in `c.t.inject.server.FeatureTestMixin` `trait <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTestMixin.scala#L11>`__.

If you only want to start **one instance of your server per test file** make sure to override this `def` with a `val`.

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

Integration Tests
^^^^^^^^^^^^^^^^^

Whereas feature tests start the server or app under test thus loading the entire object graph, integration tests generally only test across
a few interfaces in the system. In Finatra, we provide the `c.t.inject.app.TestInjector <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/TestInjector.scala>`__  which allows you to pass it a set of modules and flags to construct a minimal object graph.

To write an integration test, extend the `c.t.inject.IntegrationTest` trait. Then override the `injector` val with your constructed instance of `c.t.inject.app.TestInjector`. You'll then be able to access instances of necessary classes to execute tests.

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


Note:
~~~~~

The `injector` is specified as a `def` the in `c.t.inject.IntegrationTestMixin` `trait <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/IntegrationTestMixin.scala#L15>`__. If you only want to start **one instance of your injector per test file** make sure to override this `def` with a `val`.

Http Tests
^^^^^^^^^^

If you are writing a test that has an HTTP server under test, you can also extend the `c.t.finatra.http.HttpTest` `trait <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/HttpTest.scala>`__. This trait provides some common utilities for HTTP testing, specifically
utilities for constructing a `resolverMap <https://github.com/twitter/twitter-server/blob/develop/src/main/scala/com/twitter/server/FlagResolver.scala#L9>`__ flag value for setting on your server under test.

Thrift Tests
^^^^^^^^^^^^

As shown above, thrift servers can be tested through a `c.t.finatra.thrift.ThriftClient <https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala>`__. The Finatra test framework provides an easy way get access to a real `Finagle client <https://twitter.github.io/finagle/guide/Clients.html>`__ for making calls to your running server in a test.

In the case here, creating a `c.t.finatra.thrift.ThriftClient <https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala>`__ requires the thrift service type `T`. This type is expected to be the trait subclass of `c.t.scrooge.ThriftService` in the form of `YourService[+MM[_]]`.

Additionally, your test can also extend the `c.t.finatra.thrift.ThriftTest` `trait <https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftTest.scala>`__ which provides a utility specifically for constructing a `resolverMap <https://github.com/twitter/twitter-server/blob/develop/src/main/scala/com/twitter/server/FlagResolver.scala#L9>`__ flag value for setting on your server under test.

Test Mixins
-----------

Twitter's recommended ScalaTest test style is `FunSuite <http://doc.scalatest.org/3.0.0/#org.scalatest.FunSuite>`__.

You can use this ScalaTest test style by extending either

-  `c.t.inject.Test`, or
-  `c.t.inject.IntegrationTest`, or
-  `c.t.inject.server.FeatureTest`.

There are also deprecated versions which mix-in the `WordSpec <http://doc.scalatest.org/3.0.0/#org.scalatest.WordSpec>`__ testing style:

-  `c.t.inject.WordSpecTest`,
-  `c.t.inject.WordSpecIntegrationTest`, and
-  `c.t.inject.server.WordSpecFeatureTest`.

However, you are free to choose a ScalaTest testing style that suits your team by using the test mixin companion classes directly and mix in your preferred ScalaTest style:

-  `c.t.inject.TestMixin <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/TestMixin.scala>`__
-  `c.t.inject.IntegrationTestMixin <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/IntegrationTestMixin.scala>`__
-  `c.t.inject.server.FeatureTestMixin <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTestMixin.scala>`__

An example of using the `c.t.inject.server.FeatureTestMixin` with the `FunSpec` ScalaTest test style:

.. code:: scala

    import com.google.inject.Stage
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTestMixin
    import org.scalatest.FunSpec

    class SampleApiStartupTest
      extends FunSpec
      with FeatureTestMixin {

      override val server = new EmbeddedHttpServer(
        twitterServer = new SampleApiServer,
        stage = Stage.PRODUCTION,
        flags = Map(
          "foo.flag" -> "bar"
        )
      )

      describe("Sample Server") {
        it("should startup") {
          server.assertHealthy()
        }
      }
    }

Working with Mocks
------------------

`c.t.inject.Mockito <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/Mockito.scala>`__
provides `Specs2 <https://etorreborre.github.io/specs2/>`__ Mockito
syntax sugar for `ScalaTest <http://www.scalatest.org/>`__.

This is a drop-in replacement for `org.specs2.mock.Mockito <http://etorreborre.github.io/specs2/guide/SPECS2-3.0/org.specs2.guide.UseMockito.html>`__. We encourage you to not use `org.specs2.mock.Mockito` directly. Otherwise, match failures will not be propagated up as ScalaTest test failures.

See the next few sections on how you can use mocks in testing with either `Override Modules`_ or using `Embedded Server #bind[T] <#embedded-server-bind-t>`__.

Override Modules
----------------

For basic information on Modules in Finatra, see `Modules <../getting-started/modules.html>`__.

Defining a module is generally used to tell Guice *how* to instantiate an object to be provided to the object graph. When testing, however, we may want to provide an alternative instance of a type to the object graph. For instance, instead of making network calls to an external service through a real client we want to instead use a mock version of the client. Or load an in-memory implementation to which we can keep a reference in order to make assertions on its internal state. In these cases we can compose a server with a collection of override modules that selectively replace bound instances.

.. code:: scala

    override val server = new EmbeddedHttpServer(
      twitterServer = new ExampleServer {
        override def overrideModules = Seq(OverrideSomeBehaviorModule)
      },
      ...


For instance if you have a controller which takes in a type of `ServiceA`:

.. code:: scala

    class MyController(serviceA: ServiceA) extends Controller {
      get("/:id") { request: Request => 
        serviceA.lookupInformation(request.params("id"))
      }
    }


With a `Module <../getting-started/modules.html>`__ that provides the implementation of `ServiceA` to the injector:

.. code:: scala

    object MyServiceAModule extends TwitterModule {
      val key = flag("key", "defaultkey", "The key to use.")

      @Singleton
      @Provides
      def providesServiceA: ServiceA = {
        new ServiceA(key())
      }
    }


In order to test, you may want to use a mock or stub version of `ServiceA` in your controller instead of the real version. You could do this by writing a re-usable module for testing and compose it into the server when testing by including it as an override module.

.. code:: scala

    object StubServiceAModule extends TwitterModule {
      @Singleton
      @Provides
      def providesServiceA: ServiceA = {
        new StubServiceA("fake")
      }
    }

And in your test, add this stub module as a override module:

.. code:: scala

    override val server = new EmbeddedHttpServer(
      twitterServer = new MyGreatServer {
        override def overrideModules = Seq(StubServiceAModule)
      },
      ...


An "override module" does what it sounds like. It overrides any bound instance in the object graph with the version it provides. As seen above, the `StubServiceAModule` provided a version of `ServiceA` that happens to be a stub. In this manner the main server does not need to change and we can replace parts of its object graph during testing.

Note, modules used specifically for testing should be placed alongside your test code (as opposed to in your production code) to prevent any mistaken production usage of a test module. Also, it not always necessary to create a test module (see: `Embedded Server #bind[T] <#embedded-server-bind-t>`__ section) for use as an override module. However, we encourage creating a test module when the functionality provided by the module is re-usable across your codebase.

Also note, that you can always create an override module over a mock, however it is generally preferable to want control over the expected mock behavior per-test and as such it's more common to keep a reference to a mock and use it with the `embedded server #bind[T] <#embedded-server-bind-t>`__ functionality in a test.

Embedded Server ``#bind[T]``
----------------------------

In the cases where we'd like to easily replace a bound instance with another instance in our tests (e.g., like with a mock or a simple stub implementation), we do not need to create a specific module for testing to compose into our server as an override module. Instead we can use the `bind[T]` function on the embedded server, eg. `c.t.inject.server.EmbeddedTwitterServer#bind <https://github.com/twitter/finatra/blob/92bfb74ecf7b299adb6602847ca9daa89895f3af/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L138>`__.

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

Using ``@Bind`` (the `com.google.inject.testing.fieldbinder.Bind` annotation) is to be considered deprecated.

TestInjector ``#bind[T]``
-------------------------

As described in the `Integration Tests <#integration-tests>`__ section you can use the `TestInjector` to construct a minimal object graph for testing. The `TestInjector` also supports a `bind[T]` function to let you easily replace bound instances in the constructed object graph with another instance, like a mock or stub.

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

In this example, the bound `IdService` would be replaced with the `mockIdService`. For a more complete example, see the
`DarkTrafficCanonicalResourceHeaderTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/darktraffic/test/DarkTrafficCanonicalResourceHeaderTest.scala>`__.

Startup Tests
-------------

By default the Finatra embedded testing infrastructure sets the `Guice` `com.google.inject.Stage <https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Stage.html>`__ to `DEVELOPMENT`. For testing we choose the trade-off of a fast start-up time for the embedded server at the expense of some runtime performance as classes are lazily loaded when accessed by the test features.

However, this also means that if you have mis-configured dependencies (e.g., you attempt to inject a type that the injector cannot construct because it either has no no-arg constructor nor was it provided by a module) you may not run into this error during testing as dependencies are satisfied lazily by default.

As such, we recommend creating a simple test -- a `StartupTest` to check that your service can start up and report itself as healthy. This checks the correctness of the dependency graph, catching errors that could otherwise cause the server to fail to start.

A `StartupTest` should mimic production as closely as possible. Therefore, you should

-  set the `com.google.inject.Stage <https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Stage.html>`__ to `PRODUCTION` so that all singletons will be eagerly created at startup (integration/feature tests run in `Stage.DEVELOPMENT` by default).
-  avoid using `embedded server #bind[T] <#embedded-server-bind-t>`__ or `Override Modules`_ to replace bound types.
-  prevent Finagle clients from making outbound connections during startup tests by setting any `c.t.server.resolverMap` entries to `nil!`.

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


**Note:** this works for either `EmbeddedHttpServer` or `EmbeddedThriftServer` as `assertHealthy()` is defined on the super class `EmbeddedTwitterServer <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L144>`__.
