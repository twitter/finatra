---
layout: user_guide
title: "Getting Started"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Getting Started</li>
</ol>

Finatra at it's core is agnostic to the *type* of service being created. It can be used for anything based on [twitter/util](https://github.com/twitter/util): [com.twitter.app.App](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala"). Finatra builds on top of the [features](http://twitter.github.io/twitter-server/Features.html) of [TwitterServer](http://twitter.github.io/twitter-server/) and [Finagle](https://twitter.github.io/finagle) by allowing you to easily define a [Server](http://twitter.github.io/finagle/guide/Servers.html) and controllers (a [Service](http://twitter.github.io/finagle/guide/ServicesAndFilters.html#services)-like abstraction) which define and handle endpoints of the Server. You can also compose [Filters](http://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters) either per controller, per route in a controller, or across controllers.

## Basics
===============================

Finatra internally uses the Google [Guice](https://github.com/google/guice) dependency injection library extensively which is also available for service writers if they choose to use dependency injection.

**NOTE: You are not required to use Guice dependency injection when using Finatra**. Creating servers, wiring in controllers and applying filters can all be done without using any dependency injection. However, you will not be able to take full-advantage of Finatra's [testing](/finatra/user-guide/testing) features.

An example of Finatra's Guice integration is adding controllers to Finatra's [HttpRouter](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala) by type:

```scala
class Server extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    router.add[MyController]
  }
}
```
<div></div>

As mentioned, it is also possible to do this without using Guice, simply instantiate your controller and add the instance to the router:

```scala
class NonGuiceServer extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    val controller = new MyController(...)
    router.add(controller)
  }
}
```
<div></div>

### <a class="anchor" name="modules">Modules</a>
===============================

Modules are used in conjunction with Guice dependency injection to specify *how* to instantiate an instance of a given type. They are especially useful when instantiation of an instance is dependent on some type of external configuration (see: [Flags](/finatra/user-guide/getting-started#flags)).

We provide a [TwitterModule](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala) base class which extends the capabilities of the excellent Scala extensions for Google Guice provided by [codingwell/scala-guice](https://github.com/codingwell/scala-guice). Modules also have a hook into the Server lifecycle through the [TwitterModuleLifecycle](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala) which allows for a Module to specify startup and shutdown functionality that is re-usable and scoped to the context of the Module. For example, the framework uses the `singletonStartup` lifecycle method in the [`Slf4jBridgeModule`](https://github.com/twitter/finatra/blob/master/slf4j/src/main/scala/com/twitter/finatra/logging/modules/Slf4jBridgeModule.scala#L7) to install the [`SLF4JBridgeHandler`](http://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html) (see: [Logging](/finatra/user-guide/logging)).

#### Module Definition
* [twitter/util](https://github.com/twitter/util) [Flags](#flags) can be defined inside modules. This allows for re-usable scoping of external configuration that can be composed into a server via the module.
* Prefer using an `@Provides` methods over using the [*toInstance* bind DSL](https://github.com/google/guice/wiki/InstanceBindings).
* Usually modules are Scala *objects* since they contain no state and usage of the module is less verbose.
* Remember to add `@Singleton` to your `@Provides` method if you require only **one** instance per JVM process.
* Generally, modules are only required for instantiating classes that you don't control. Otherwise, you would simply add the [JSR-330](https://github.com/google/guice/wiki/JSR330) annotations directly to the class. For example, suppose you need to create an `ThirdPartyFoo` class which comes from a thirdparty jar. You could create the following Guice module to construct a singleton `ThirdPartyFoo` class which is created with a key provided through a command line flag.

```scala
object MyModule1 extends TwitterModule {
  val key = flag("key", "defaultkey", "The key to use.")

  @Singleton
  @Provides
  def providesThirdPartyFoo: ThirdPartyFoo = {
    new ThirdPartyFoo(key())
  }
}
```
<div></div>

You would then be able to inject an instance of the type `ThirdPartyFoo` using the `@Inject` annotation:

```scala
class MyService @Inject() (
  thirdPartyFoo: ThirdPartyFoo
) {
  ...
}
```
<div></div>

#### Module Configuration
A server is then started with a list of immutable Guice modules:
```scala
class Server extends HttpServer {
  override val modules = Seq(
    MyModule1,
    MyModule2)

  ...
}
```
<div></div>

### <a class="anchor" name="flags">Flags</a>
===============================

Finatra supports the use of [twitter/util](https://github.com/twitter/util) [Flags](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala) as supported within the [TwitterServer](http://twitter.github.io/twitter-server/Features.html#flags) lifecycle. Flags by their definition represent some external configuration that is passed to the system and are thus an excellent way to parameterize external configuration that may be environment specific, e.g., a database host or URL that is different per environment, *production*, *staging*, or *development*.

This type of configuration parameterization is generally preferred over hardcoding logic by a type of "*environment*" String within code. As such, flags are generally defined within a [Module](#module) to allow for scoping of reusable external configuration. In this way, flags can be used to aid in the construction of an instance to be provided to the object graph, e.g., a DatabaseConnection instance that use the database URL flag as an input. The module is then able to tell Guice how to provide this object when injected by defining an `@Provides` annotated method.

In Finatra, we also provide a way to override the objects provided on the object graph through "override modules". See the "Override Modules" section in [testing](testing#override-modules).

#### `@Flag` annotation
Flag values can be injected into classes (and provider methods), by using the `@Flag` annotation:

```scala
class MyService @Inject()(
  @Flag("key") key: String) {
}

class MyModule extends TwitterModule {
  @Provides
  @Singleton
  def providesFoo(@Flag("key") key: String) = {
    new Foo(key)
  }
}
```
<div></div>

**NOTE**: If a flag is defined in a module, you can dereference that flag directly within the module (instead of using the `@Flag` annotation), e.g.:

```scala
object MyModule1 extends TwitterModule {
  val key = flag("key", "defaultkey", "The key to use.")

  @Singleton
  @Provides
  def providesThirdPartyFoo: ThirdPartyFoo = {
    new ThirdPartyFoo(key())
  }
}
```
<div></div>

Use the `-help` flag to see usage for running a Finatra server, e.g.

```bash
$ java -jar finatra-hello-world-assembly-2.0.0.jar -help
```
<div></div>

### <a class="anchor" name="futures">Futures</a> (`com.twitter.util.Future` vs. `scala.concurrent.Future`)
===============================

Finatra, like other frameworks based on Twitter's [Finagle](https://twitter.github.io/finagle), uses the [twitter/util](https://github.com/twitter/util) [`com.twitter.util.Future`](https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala) class. Twitter's `com.twitter.util.Future` is similar to, but predates, [Scala's](http://docs.scala-lang.org/overviews/core/futures.html) [`scala.concurrent.Future`](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future) (introduced in Scala 2.10 and later backported to Scala 2.9.3) and is *not* compatible without using [bijections](https://github.com/twitter/bijection) to transform one into the other. It is important to remember that Finatra uses and expects [`com.twitter.util.Future`](https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala).

<nav>
  <ul class="pager">
    <li></li>
    <li class="next"><a href="/finatra/user-guide/build-new-http-server">&nbsp;Building&nbsp;a&nbsp;new&nbsp;HTTP&nbsp;Server&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
