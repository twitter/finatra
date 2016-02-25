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

An example of Finatra's dependency-injection integration is adding controllers to Finatra's [HttpRouter](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala) *by type*:

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
class NonDIServer extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    val controller = new MyController(...)
    router.add(controller)
  }
}
```
<div></div>

### <a class="anchor" name="lifecycle" href="#lifecycle">Server Lifecycle</a>
===============================

At a high-level, the start-up lifecycle of a Finatra server looks like:

![Server Lifecycle](/finatra/images/FinatraLifecycle.png)

Upon *graceful* shutdown, all registered `onExit {...}` blocks are executed (see [`com.twitter.util.App#exits`](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala#L69)). This includes closing the external interface(s), the admin interface, and firing the [`TwitterModule#singletonShutdown`](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala#L25) on all installed modules. See the next section for more information on [Modules](#modules).


### <a class="anchor" name="modules" href="#modules">Modules</a>
===============================

Modules are used in conjunction with dependency injection to specify *how* to instantiate an instance of a given type. They are especially useful when instantiation of an instance is dependent on some type of external configuration (see: [Flags](/finatra/user-guide/getting-started#flags)).

We provide a [TwitterModule](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala) base class which extends the capabilities of the excellent Scala extensions for Google Guice provided by [codingwell/scala-guice](https://github.com/codingwell/scala-guice).

#### Module Definition
* [twitter/util](https://github.com/twitter/util) [Flags](#flags) can be defined inside modules. This allows for re-usable scoping of external configuration that can be composed into a server via the module.
* Prefer using `@Provides`-annotated methods over using the [*toInstance* bind DSL](https://github.com/google/guice/wiki/InstanceBindings).
* Usually modules are Scala *objects* since they contain no state and makes usage of the module less verbose.
* Remember to add `@Singleton` to your `@Provides` method if you require only **one** instance per JVM process.
* Generally, modules are only required for instantiating classes that you don't control. Otherwise, you would simply add the [JSR-330](https://github.com/google/guice/wiki/JSR330) annotations directly to the class. For example, suppose you need to create an `ThirdPartyFoo` class which comes from a thirdparty jar. You could create the following module to construct a singleton `ThirdPartyFoo` class which is created with a key provided through a command line flag.

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
A server is then started with a list of immutable modules:
```scala
class Server extends HttpServer {
  override val modules = Seq(
    MyModule1,
    MyModule2)

  ...
}
```
<div></div>

#### Module Lifecycle

Modules also have a hook into the Server lifecycle through the [TwitterModuleLifecycle](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala) which allows for a module to specify startup and shutdown functionality that is re-usable and scoped to the context of the Module. For example, the framework uses the `singletonStartup` lifecycle method in the [`Slf4jBridgeModule`](https://github.com/twitter/finatra/blob/master/slf4j/src/main/scala/com/twitter/finatra/logging/modules/Slf4jBridgeModule.scala#L7) to install the [`SLF4JBridgeHandler`](http://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html) (see: [Logging](/finatra/user-guide/logging)).

The [`com.twitter.inject.TwitterModule`](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala) exposes two lifecycle methods: `TwitterModule#singletonStartup` and `TwitterModule#singletonShutdown`. If your module provides a resource that requires one-time start-up or initialization you can do this by implementing the `singletonStartup` method in your TwitterModule. Conversely, if you want to clean up resources on graceful shutdown of the server you can implement the `singletonShutdown` method of your TwitterModule to close or shutdown any resources provided by the module.

### <a class="anchor" name="binding-annotations" href="#binding-annotations">Binding Annotations</a>
===============================

Occasionally, you may want multiple bound instances of the same type. For instance you may want both a FooHttpClient and a BarHttpClient. To do this we recommend creating specific [binding annotations](https://github.com/google/guice/wiki/BindingAnnotations).

#### Define an Annotation

Defining a binding annotation is a few lines of java code plus imports. We recommend that you put this in it's own `.java` file.

```scala
package example.http.clients.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@BindingAnnotation
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface FooClient {}
```

For more information on the meta-annotations see the Google Guice [documentation on Binding Annotations](https://github.com/google/guice/wiki/BindingAnnotations).

#### Create a Binding with the Annotation

In your Module annotate the `@Provides` method that is instantiating the specific instance with the Binding Annotation, e.g.,

```scala
object MyHttpClientsModule extends TwitterModule {
  val fooClientDestination = flag("foo.client.dest", "Foo Client Destination")
  val barClientDestination = flag("bar.client.dest", "Bar Client Destination")

  @Singleton
  @Provides
  @FooClient
  def providesFooHttpClient: HttpClient = {
    new HttpClient(fooClientDestination())
  }

  @Singleton
  @Provides
  @BarClient
  def providesBarHttpClient: HttpClient = {
    new HttpClient(barClientDestination())
  }
}
```
<div></div>

#### Depend on the Annotation

Then to depend on the annotated binding, just apply the annotation to the injected parameter:

```scala
class MyService @Inject()(
  @FooClient fooHttpClient: HttpClient,
  @BarClient barHttpClient: HttpClient) {
  ...
}
```
<div></div>

#### Benefits Over Using [`@Named`](https://github.com/google/guice/wiki/BindingAnnotations#named) Binding Annotation

You could also achieve the same behavior using the [`@Named`](https://github.com/google/guice/wiki/BindingAnnotations#named) binding annotation. However we've found that creating specific binding annotations avoids potential naming collisions. Additionally, being able to find all usages of the annotation by type is beneficial over a text-search for the string used in `@Named`.

### <a class="anchor" name="flags" href="#flags">Flags</a>
===============================

Finatra supports the use of [twitter/util](https://github.com/twitter/util) [Flags](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala) as supported within the [TwitterServer](http://twitter.github.io/twitter-server/Features.html#flags) lifecycle. Flags by their definition represent some external configuration that is passed to the system and are thus an excellent way to parameterize external configuration that may be environment specific, e.g., a database host or URL that is different per environment, *production*, *staging*, or *development*.

This type of configuration parameterization is generally preferred over hardcoding logic by a type of "*environment*" String within code. As such, flags are generally defined within a [Module](#module) to allow for scoping of reusable external configuration. In this way, flags can be used to aid in the construction of an instance to be provided to the object graph, e.g., a DatabaseConnection instance that use the database URL flag as an input. The module is then able to tell Guice how to provide this object when injected by defining an `@Provides` annotated method.

In Finatra, we also provide a way to override the objects provided on the object graph through "override modules". See the "Override Modules" section in [testing](testing#override-modules).

#### `@Flag` annotation
Flag is a [binding annotation](#binding-annotations). This annotation allows flag values to be injected into classes (and provider methods), by using the `@Flag` annotation:

```scala
class MyModule extends TwitterModule {
  flag("key", "default", "The key to use")

  @Provides
  @Singleton
  def providesFoo(
    @Flag("key") key: String) = {
    new Foo(key)
  }
}

class MyService @Inject()(
  @Flag("key") key: String) {
}
```
<div></div>

**NOTE**: If a flag is defined in a module, you can dereference that flag directly within the module (instead of using the `@Flag` annotation), e.g.:

```scala
object MyModule1 extends TwitterModule {
  val key = flag("key", "default", "The key to use")

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
HelloWorldServer
  -alarm_durations='1.seconds,5.seconds': 2 alarm durations
  -help='false': Show this help
  -admin.port=':8080': Admin http server port
  -bind=':0': Network interface to use
  -log.level='INFO': Log level
  -log.output='/dev/stderr': Output file
  -key='default': The key to use
```
<div></div>

Flags are set by passing them as arguments to your java application &mdash; as seen above (`-help` is a flag), e.g.,

```bash
$ java -jar finatra-hello-world-assembly-2.0.0.jar -key=value
```

`TwitterModule#flag` is parameterized to return a Flag of type `T` where `T` is the type of the arguement passed as the default. If you do not specify a default value then you must explicitly parameterize your call to `TwitterModule#flag`, e.g,

```scala
object MyModule1 extends TwitterModule {
  val key = flag[String]("key", "The key to use")

  @Singleton
  @Provides
  def providesThirdPartyFoo: ThirdPartyFoo = {
    new ThirdPartyFoo(key())
  }
}
```

### <a class="anchor" name="futures" href="#futures">Futures</a> (`com.twitter.util.Future` vs. `scala.concurrent.Future`)
===============================

Finatra, like other frameworks based on Twitter's [Finagle](https://twitter.github.io/finagle), uses the [twitter/util](https://github.com/twitter/util) [`com.twitter.util.Future`](https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala) class. Twitter's `com.twitter.util.Future` is similar to, but predates, [Scala's](http://docs.scala-lang.org/overviews/core/futures.html) [`scala.concurrent.Future`](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future) (introduced in Scala 2.10 and later backported to Scala 2.9.3) and is *not* compatible without using a [bijection](https://github.com/twitter/bijection) to transform one into the other. It is important to remember that **Finatra uses and expects** [`com.twitter.util.Future`](https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala).

For more information on [twitter/bijection](https://github.com/twitter/bijection) it is highly recommended that you read the [bijection README](https://github.com/twitter/bijection/blob/develop/README.md). For more information on converting between a `com.twitter.util.Future` and a `scala.concurrent.Future` see this [thread](https://groups.google.com/forum/#!searchin/finaglers/UtilBijections/finaglers/DjCqv1Vyw0Q/IWwh9y43CAAJ).

A simple example,

```scala
import com.twitter.bijection.Conversion._
import com.twitter.bijection.twitter_util.UtilBijections.twitter2ScalaFuture
import com.twitter.util.{Future => TwitterFuture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future => ScalaFuture}

val scalaFuture: ScalaFuture[T] = {...} // some call that returns scala.concurrent.Future[T]
scalaFuture.as[TwitterFuture[T]]        // this converts the scala.concurrent.Future[T] to a com.twitter.util.Future[T]
```
<div></div>

<nav>
  <ul class="pager">
  <li class="previous"><a href="/finatra/user-guide/twitter-server-basics"><span aria-hidden="true">&larr;</span>&nbsp;TwitterServer&nbsp;Basics</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-http-server">&nbsp;Building&nbsp;a&nbsp;new&nbsp;HTTP&nbsp;Server&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
