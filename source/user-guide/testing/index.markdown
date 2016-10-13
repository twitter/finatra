---
layout: user_guide
title: "Testing"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Testing</li>
</ol>

![Testing](http://imgs.xkcd.com/comics/exploits_of_a_mom.png)

## Basics

Finatra provides the following testing features:

- the ability to start a locally running server, issue requests, and assert responses.
- the ability to easily replace class implementations throughout the object graph.
- the ability to retrieve classes in the object graph to perform assertions on them.
- the ability to write powerful tests without deploying test code to production.

## <a class="anchor" name="testing-types" href="#testing-types">Types of Tests</a>

What are we talking about when we talk about *testing*? At a high-level the philosophy of testing in Finatra revolves around the following definitions:

- [Feature Tests](#feature-tests) -  the most powerful tests enabled by Finatra. These tests allow you to verify feature requirements of the service by exercising its external interface. Finatra supports both “black-box testing” and “white-box testing” against a locally running version of your server. You can selectively swap out certain classes, insert mocks, and perform assertions on internal state. It’s worth noting that we sometimes re-use these tests for regression testing in larger “System Tests” that run post-release on live services. Take a look at an example feature test [here](https://github.com/twitter/finatra/blob/develop/examples/hello-world/src/test/scala/com/twitter/hello/HelloWorldFeatureTest.scala).
- [Integration Tests](#integration-tests) - similar to feature tests, but the entire service is not started. Instead, a list of [modules](/finatra/user-guide/getting-started#modules) are loaded and then method calls and assertions are performed at the class-level. You can see an example integration test [here](https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/marshalling/CallbackConverterIntegrationTest.scala).
- Unit Tests - these are tests of a single class and since constructor injection is used throughout the framework, Finatra stays out of your way.

## [ScalaTest](http://www.scalatest.org/)
===============================

The Finatra testing framework uses the [`WordSpec`](http://doc.scalatest.org/2.2.4/#org.scalatest.WordSpec) ScalaTest [testing style](http://www.scalatest.org/user_guide/selecting_a_style) testing style for framework testing and to facilitate the types of testing outlined above we have several testing traits to aid in creating simple and powerful tests. For more information on [ScalaTest](http://www.scalatest.org/), see the [ScalaTest User Guide](http://www.scalatest.org/user_guide).

## <a class="anchor" name="embedded-server" href="#embedded-server">Embedded Servers and Apps</a>
===============================

Finatra provides a way to run an embedded version of your service or app running locally on ephemeral ports. This allows you to run *actual* requests against an *actual* version of your server when testing. Embedding is an especially powerful way of running and testing your application through an IDE, e.g., like [IntelliJ](https://www.jetbrains.com/idea/).

The embedded utilities are also useful for testing and debugging your code when prototyping. If your service or API makes calls to other services, instead of mocking out or overriding those dependencies with dummy implementations you can always write a test using an Embedded version of your server which talks to *real* downstream services (of course you'd never want to commit a test like this to your source repository, especially if you run any type of [continuous integration](https://en.wikipedia.org/wiki/Continuous_integration) system). You'll be able to run this test normally through the test runner of an IDE which would allow you to easily set breakpoints and step-through code for debugging. As opposed to needing to build and run your service locally and attach a remote debugger.

See:

- [`c.t.inject.app.EmbeddedApp`](https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/EmbeddedApp.scala)
- [`c.t.inject.server.EmbeddedTwitterServer`](https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala)
- [`c.t.finatra.http.EmbeddedHttpServer`](https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala)
- [`c.t.finatra.thrift.EmbeddedThriftServer`](https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala)

<div></div>

![Embedded Classes](/finatra/images/embedded.png)

You'll notice that this hierarchy generally follows the server class hierarchy as [`c.t.finatra.http.HttpServer`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala) and [`c.t.finatra.thrift.ThriftServer`](https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/ThriftServer.scala) extend from [`c.t.server.TwitterServer`](https://github.com/twitter/twitter-server/blob/develop/src/main/scala/com/twitter/server/TwitterServer.scala) which extends from [`c.t.app.App`](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala).

## <a class="anchor" name="test-helpers" href="#test-helpers">Test Helper Classes</a>
===============================

![Finatra Test classes](/finatra/images/test-classes.png)

### <a class="anchor" name="feature-tests" href="#feature-tests">Feature Tests</a>

If you are familiar with [Gherkin](http://docs.behat.org/en/v2.5/guides/1.gherkin.html) or [Cucumber](https://github.com/cucumber/cucumber/wiki/Feature-Introduction) or other similar testing languages and frameworks, then [feature testing](https://wiki.documentfoundation.org/QA/Testing/Feature_Tests) will feel somewhat familiar. In Finatra, a feature test always consists of an app or a server under test. See the [server/FeatureTest](https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala) trait.

We highly recommend writing feature tests for your services as they provide a very good signal of whether you have correctly implemented the features of your service. If you haven't implemented the feature correctly, it almost doesn't matter that you have lots of unit tests.

For example, to write a feature test for an HTTP server, extend the `c.t.inject.server.FeatureTest` trait. Then override the `server` with an instance of your [`EmbeddedHttpServer`](#embedded-server).

```scala
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class ExampleServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedHttpServer(new ExampleServer)

  "MyTest" should  {

    "perform feature" in {
      server.httpGet(
        path = "/",
        andExpect = Status.Ok)
        ...
    }
  }
}
```
<div></div>

Similarly, to write a feature test for a Thrift server and create a [client](#thrift-tests) to it,

```scala
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest

class ExampleThriftServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(new ExampleThriftServer)

  lazy val client = server.thriftClient[ExampleThrift[Future]](clientId = "client123")

  "MyTest" should {
    "return data accordingly" in {
      Await.result(client.doExample("input")) should equal("output")
    }
  }
}
```
<div></div>

If you are extending both `c.t.finatra.http.HttpServer` **and** `c.t.finatra.thrift.ThriftServer` then you can feature test by constructing an `EmbeddedHttpServer with ThriftClient`, e.g.,

```scala
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class ExampleCombinedServerFeatureTest extends FeatureTest {
  override val server =
    new EmbeddedHttpServer(new ExampleCombinedServer) with ThriftClient

  lazy val client = server.thriftClient[ExampleThrift[Future]](clientId = "client123")

  "MyTest" should {
    "perform feature" in {
      server.httpGet(
        path = "/",
        andExpect = Status.Ok)
        ...
    }

    "return data accordingly" in {
      Await.result(client.doExample("input")) should equal("output")
    }
  }
}
```
<div></div>

#### Note:
The `server` is specified as a `def` in `c.t.inject.server.FeatureTest` trait. If you only want to start **one instance of your server per test file** make sure to override this `def` with a `val`.

For more advanced examples see:

- the [`DoEverythingServerFeatureTest`](https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/test/DoEverythingServerFeatureTest.scala) for an HTTP server.
- the [`DoEverythingThriftServerFeatureTest`](https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/tests/DoEverythingThriftServerFeatureTest.scala) for a Thrift server.
- the [`DoEverythingCombinedServerFeatureTest`](https://github.com/twitter/finatra/blob/develop/inject-thrift-client-http-mapper/src/test/scala/com/twitter/finatra/multiserver/test/DoEverythingCombinedServerFeatureTest.scala) for "combined" HTTP and Thrift server.

### <a class="anchor" name="integration-tests" href="#integration-tests">Integration Tests</a>

Whereas feature tests start the server or app under test thus loading the entire object graph, integration tests generally only test across a few interfaces in the system. In Finatra, we provide the [`c.t.inject.app.TestInjector`](https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/TestInjector.scala) which allows you to pass it a set of modules and flags to construct a minimal object graph.

To write an integration test, extend the `c.t.inject.IntegrationTest` trait. Then override the `injector` val with your constructed instance of `c.t.inject.app.TestInjector`. You'll then be able to access instances of necessary classes to execute tests.

```scala
import com.twitter.inject.IntegrationTest

class ExampleIntegrationTest extends IntegrationTest {
  override val injector =
    TestInjector(
      flags = Map("foo.flag" -> "meaningfulValue"),
      modules = Seq(ExampleModule))

  "MyTest" should  {

    "perform feature" in {
        val exampleThingy = injector.instance[ExampleThingy]
        ...
    }
  }
}

```
<div></div>

### <a class="anchor" name="http-tests" href="#http-tests">Http Tests</a>

If you are writing a test that has an HTTP server under test, you can optionally extend the [`c.t.finatra.http.HttpTest`](https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/HttpTest.scala) trait. This trait provides some common utilities for HTTP testing.

### <a class="anchor" name="thrift-tests" href="#thrift-tests">Thrift Tests</a>

As shown above, thrift servers can be tested through a [`c.t.finatra.thrift.ThriftClient`](https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala). The Finatra test framework provides an easy way get access to a real [Finagle client](http://twitter.github.io/finagle/guide/Clients.html) for making calls to your running server in a test. In the case here, creating a [`c.t.finatra.thrift.ThriftClient`](https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala) requires the thrift service type `T`. This type is expected to be the trait subclass of `c.t.scrooge.ThriftService` in the form of `YourService[+MM[_]]`.

## <a class="anchor" name="startup-tests" href="#startup-tests">Startup Tests</a>
===============================

By default the Finatra embedded testing infrastructure sets the [Guice `com.google.inject.Stage`](https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Stage.html) to `DEVELOPMENT`. For testing we choose the trade-off of a fast start-up time for the embedded server at the expense of some runtime performance as classes are lazily loaded when accessed by the test features.

However, this also means that if you have misconfigured dependencies (e.g., you attempt to inject a type that the injector cannot construct because it either has no no-arg constructor nor was it provided by a module) you may not run into this error during testing as dependencies are satisfied lazily by default.

As such, we recommend creating a simple test -- a `StartupTest` to check that your service can start up and report itself as healthy. This checks the correctness of the Guice dependency graph, catching errors that could otherwise cause the server to fail to start.

* a `StartupTest` should mimic production as closely as possible. Thus:
    - avoid using `@Bind` and [override modules](#override-modules).
    - set the [Guice `com.google.inject.Stage`](https://google.github.io/guice/api-docs/4.0/javadoc/com/google/inject/Stage.html) to `PRODUCTION` so that all singletons will be eagerly created at startup (integration/feature tests run in `Stage.DEVELOPMENT` by default).
    - prevent Finagle clients from making outbound connections during startup tests by setting any `c.t.server.resolverMap` entries to `nil!`.

For example:

```scala
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

  "SampleApiServer" should {
    "startup" in {
      server.assertHealthy()
    }
  }
}
```
<div></div>

**Note:** this works for either `EmbeddedHttpServer` or `EmbeddedThriftServer` as `assertHealthy()` is defined on the super class [`EmbeddedTwitterServer`](https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L144).

## <a class="anchor" name="mocks" href="#mocks">Working with Mocks</a>
===============================

[`c.t.inject.Mockito`](https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/Mockito.scala) provides [Specs2](https://etorreborre.github.io/specs2/) Mockito syntax sugar for [ScalaTest](http://www.scalatest.org/).

This is a drop-in replacement for [`org.specs2.mock.Mockito`](http://etorreborre.github.io/specs2/guide/SPECS2-3.0/org.specs2.guide.UseMockito.html). We encourage you to not use `org.specs2.mock.Mockito` directly. Otherwise, match failures won't be propagated up as ScalaTest test failures.

See the next few sections on how you can use mocks in testing with either [Override Modules](#override-modules) or using [`@Bind`](#at-bind).

## <a class="anchor" name="override-modules" href="#override-modules">Override Modules</a>
===============================

For basic information on Modules in Finatra, see [Modules](/finatra/user-guide/getting-started#modules).

Defining a module is generally used to tell Guice *how* to instantiate an object to be provided to the object graph. When testing, however, we may want to provide an alternative instance of a type to the object graph. For instance, instead of making network calls to an external service through a real client we want to instead use a mock version of the client. Or load an in-memory implementation to which we can keep a reference in order to make assertions on it's internal state. In these cases we can compose a server with a collection of override modules that selectively replace bound instances.

```scala
override val server = new EmbeddedHttpServer(
  twitterServer = new ExampleServer {
    override def overrideModules = Seq(OverrideSomeBehaviorModule)
  },
  ...
```
<div></div>

For instance if you have a controller which takes in a type of `ServiceA`:

```scala
class MyController(serviceA: ServiceA) extends Controller {
  get("/:id") { request: Request => 
    serviceA.lookupInformation(request.params("id"))
  }
}
```
<div></div>

With a [Module](/finatra/user-guide/getting-started/#modules) that provides the implementation of `ServiceA` to the injector:

```scala
object MyServiceAModule extends TwitterModule {
  val key = flag("key", "defaultkey", "The key to use.")

  @Singleton
  @Provides
  def providesServiceA: ServiceA = {
    new ServiceA(key())
  }
}
```
<div></div>

To test you may want to use a mock or stub version of `ServiceA` in your controller instead of the real version. You could do this by writing a re-usable module for testing and compose it into the server when testing as an override module.

```scala
object StubServiceAModule extends TwitterModule {
  @Singleton
  @Provides
  def providesServiceA: ServiceA = {
    new StubServiceA("fake")
  }
}
```

And in your test, add this stub module as a override module:

```scala
override val server = new EmbeddedHttpServer(
  twitterServer = new MyGreatServer {
    override def overrideModules = Seq(StubServiceAModule)
  },
  ...
```
<div></div>

Note, modules used specifically for testing should be placed alongside your test code (as opposed to in your production code) to prevent any mistaken production usage of a test module. Also, it not always necessary to create a test module (see: [`@Bind`](#at-bind) section) for use as an override module. However, we encourage creating a test module when the functionality provided by the module is re-usable across your codebase. 

Also note, that you can always create an override module over a mock, however it is generally preferable to want control over the expected mock behavior per-test and as such it's more common to keep a reference to a mock and use it with the [`@Bind`](#at-bind) functionality in a test.

## <a class="anchor" name="at-bind" href="#at-bind">Using `@Bind`</a>
===============================

First, check out the [Google Guice](https://github.com/google/guice) documentation on Bound Fields [here](https://github.com/google/guice/wiki/BoundFields).

In the cases where we'd like to easily replace a bound instance with another instance in our tests (e.g., like with a mock or a simple stub implementation), we do not need to create a specific module for testing to compose into our server as an override module. Instead we can use the `com.google.inject.testing.fieldbinder.Bind` annotation.

```scala

import com.google.inject.testing.fieldbinder.Bind
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.Mockito

class ExampleFeatureTest
  extends FeatureTest
  with Mockito
  with HttpTest {

  @Bind val mockDownstreamServiceClient = smartMock[DownstreamServiceClient]

  @Bind val mockIdService = smartMock[IdService]

  override val server = new EmbeddedHttpServer(new ExampleServer)  

  "test" in {
    /* Mock GET Request performed by DownstreamServiceClient */
    mockDownstreamServiceClient.get("/tweets/123.json")(manifest[FooResponse]) returns Future(None)
    ...
  }
```
<div></div>

#### N.B.:

* You **MUST** extend from either [`c.t.inject.IntegrationTest`](https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/IntegrationTest.scala) directly or from a sub-class. We recommend using [`c.t.inject.server.FeatureTest`](https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala). See more information on these test traits in the [Feature Tests](#feature-tests) section.
* Prefer to define `@Bind` and `@Inject` variables before the server definition.
* While we support the `com.google.inject.testing.fieldbinder.Bind` annotation, our integration does not currently support the `to` annotation field, e.g., `@Bind(to = classOf[T])`, therefore,
* The type of the variable you annotate with `@Bind` must *exactly* match the type in the object graph you want to override. E.g., if you want to override an implementation bound to an interface with a mock or stub that implements the same interface, you should make sure to type the variable definition. For instance,
```scala
@Bind val idService: IdService = new MockIdServiceImpl
```
* Because of lifecycle reasons, access to the embedded server **MUST** either be from a lazy variable or inside a test method.

For a complete example, see the [TwitterCloneFeatureTest](https://github.com/twitter/finatra/blob/develop/examples/twitter-clone/src/test/scala/finatra/quickstart/TwitterCloneFeatureTest.scala).

There is also another way to user this type of binding in tests (which will eventually become the preferred way) that does not have the above caveats (but instead comes with a different caveat). You can also achieve the above `@Bind` behavior by using [`c.t.inject.server.EmbeddedTwitterServer#bind`](https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L136). Unfortunately, we have not yet migrated from using Scala Manifests and thus this method suffers from not being able to fully support [higher-kinded](http://blogs.atlassian.com/2013/09/scala-types-of-a-higher-kind/) types. 

To use, you can do:

```scala
val mockIdService = smartMock[IdService]

override val server = 
  new EmbeddedHttpServer(new ExampleServer)
    .bind[IdService](mockIdService)
```

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-thrift-server"><span aria-hidden="true">&larr;</span>&nbsp;Building&nbsp;a&nbsp;new&nbsp;Thrift&nbsp;Server</a></li>
    <li></li>
  </ul>
</nav>
