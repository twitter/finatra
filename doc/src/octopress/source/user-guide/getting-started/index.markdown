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

Finatra at it's core is agnostic to the *type* of service being created. It can be used for anything based on [twitter/util](https://github.com/twitter/util): [c.t.app.App](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala"). Finatra builds on top of the [features](http://twitter.github.io/twitter-server/Features.html) of [TwitterServer](http://twitter.github.io/twitter-server/) and [Finagle](https://twitter.github.io/finagle) by allowing you to easily define a [Server](http://twitter.github.io/finagle/guide/Servers.html) and controllers (a [Service](http://twitter.github.io/finagle/guide/ServicesAndFilters.html#services)-like abstraction) which define and handle endpoints of the Server. You can also compose [Filters](http://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters) either per controller, per route in a controller, or across controllers.

## <a class="no-pad-anchor" name="dependencies" href="#dependencies">Basics</a>
===============================

To get started, add a dependency on either [finatra-http](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20a%3A%22finatra-http_2.11%22) or [finatra-thrift](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20a%3A%22finatra-thrift_2.11%22) (depending on if you are building an HTTP or Thrift server), e.g., with [sbt](http://www.scala-sbt.org/):

```
"com.twitter" %% "finatra-http" % VERSION
```

or 

```
"com.twitter" %% "finatra-thrift" % VERSION
```

Where, `VERSION` is the [released version](https://github.com/twitter/finatra/releases) you want to use of the Finatra framework. Similarily, with [Maven](http://maven.apache.org/):

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra-http_2.11</artifactId>
  <version>VERSION</version>
</dependency>
```

or 

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra-thrift_2.11</artifactId>
  <version>VERSION</version>
</dependency>
```

*Note*: with Maven, you **must** append the appropriate scala version to the artifact name (in this example, `_2.11`). See the Finatra [hello-world](https://github.com/twitter/finatra/tree/finatra-2.2.0/examples/hello-world) example for a more in-depth example.

#### Additional Repository

Finatra (through it's dependency on [Finagle](http://twitter.github.io/finagle/)) has transitive dependencies that are not published to maven central and are **only available** via the Twitter `maven.twttr.com` repository. We realize that this is less than ideal, and we hope to be able to publish these dependencies to maven central in the future. For now it requires that you also add a way to resolve these dependencies from the `maven.twttr.com` repository.

To do so with [sbt](http://www.scala-sbt.org/) you would add an additional resolver:

```
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "Twitter Maven" at "https://maven.twttr.com"
)
```

And in [Maven](http://maven.apache.org/), list as a `<respository/>`:

```
<repositories>
    <repository>
      <id>twitter-repo</id>
      <name>twitter-repo</name>
      <url>https://maven.twttr.com</url>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>twitter-repo</id>
      <name>twitter-repo</name>
      <url>https://maven.twttr.com</url>
    </pluginRepository>
  </pluginRepositories>
```

#### <a class="anchor" name="test-dependencies" href="#test-dependencies">Test dependencies</a>

Finatra publishes [test-jars](https://maven.apache.org/guides/mini/guide-attached-tests.html) for most modules. The `test-jars` include re-usable utilities for use in testing (e.g., the [EmbeddedTwitterServer](https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala)). To add a `test-jar` dependency, depend on the appropriate module with the `tests` classifier. Additionally, these dependencies are typically only needed in the `test` scope for your project. E.g., with [sbt](http://www.scala-sbt.org/):

```
"com.twitter" %% "finatra-http" % VERSION % "test" classifier "tests"
```

or 

```
"com.twitter" %% "finatra-thrift" % VERSION % "test" classifier "tests"
```

See the sbt documentation for more information on using [ivy configurations and classifiers](http://www.scala-sbt.org/0.13/docs/Library-Management.html). And with [Maven](http://maven.apache.org/):

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra-http_2.11</artifactId>
  <scope>test</scope>
  <type>test-jar</type>
  <version>VERSION</version>
</dependency>
```

or

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra-thrift_2.11</artifactId>
  <scope>test</scope>
  <type>test-jar</type>
  <version>VERSION</version>
</dependency>
``` 

There is a **[downside](https://maven.apache.org/plugins/maven-jar-plugin/examples/create-test-jar.html)** to this manner of publishing `test-jars`: you don't get the transitive test-scoped dependencies automatically. Maven and sbt only resolve compile-time dependencies transitively, so you'll have to specify all other required test-scoped dependencies manually. 

For example, the finatra-http `test-jar` depends on inject-app `test-jar` (among others). You will have to manually add a dependency on the inject-app `test-jar` since it will not be picked up transitively. You can infer the dependency graph of the test-jar based on the dependencies of the main module that are listed as being in the "test->test" or ("test" and "test->compile") ivy configurations.

Additionally, using the [sbt-dependency-graph](https://github.com/jrudolph/sbt-dependency-graph) plugin, you can list the dependencies of the `finatra/http` test configuration for the `packageBin` task:

```
$ ./sbt -Dsbt.log.noformat=true http/test:packageBin::dependencyList 2>&1 | grep 'com\.twitter:finatra\|com\.twitter:inject'
[info] com.twitter:finatra-http_2.11:2.8.0-SNAPSHOT
[info] com.twitter:finatra-httpclient_2.11:2.8.0-SNAPSHOT
[info] com.twitter:finatra-jackson_2.11:2.8.0-SNAPSHOT
[info] com.twitter:finatra-slf4j_2.11:2.8.0-SNAPSHOT
[info] com.twitter:finatra-utils_2.11:2.8.0-SNAPSHOT
[info] com.twitter:inject-app_2.11:2.8.0-SNAPSHOT
[info] com.twitter:inject-core_2.11:2.8.0-SNAPSHOT
[info] com.twitter:inject-modules_2.11:2.8.0-SNAPSHOT
[info] com.twitter:inject-request-scope_2.11:2.8.0-SNAPSHOT
[info] com.twitter:inject-server_2.11:2.8.0-SNAPSHOT
[info] com.twitter:inject-slf4j_2.11:2.8.0-SNAPSHOT
[info] com.twitter:inject-utils_2.11:2.8.0-SNAPSHOT
```

In this case, when executing the `packageBin` task for `finatra/http` in the test configuration these dependencies are necessary. Unfortunately, this listing does not explicity state if it's the compile-time or test-jar version of the dependency that is necessary. However, it is safe to assume that if you want a dependency on the `finatra/http` test-jar you will also need to add dependencies on any test-jar from the listed dependencies as well.

#### Lightbend Activator

Finatra also has [Lightbend](https://www.lightbend.com/activator/download) Activator [templates](https://www.lightbend.com/activator/templates#filter:finatra v2.x) for generating project scaffolds:

* HTTP [template](https://github.com/twitter/finatra-activator-http-seed). [Activator](https://www.lightbend.com/activator/download) instructions [here](https://www.lightbend.com/activator/template/finatra-http-seed).
* Thrift [template](https://github.com/twitter/finatra-activator-thrift-seed). [Activator](https://www.lightbend.com/activator/download) instructions [here](https://www.lightbend.com/activator/template/finatra-thrift-seed).

See the Lightbend Activator [documentation](https://www.lightbend.com/activator/docs) for information how to use these templates with the activator application.

## <a class="anchor" name="examples" href="#examples">Examples</a>
===============================

Finatra includes a few working examples which highlight various features of the framework and include tests. In the [develop branch](https://github.com/twitter/finatra/tree/develop/examples) these examples are included in the root [sbt](http://www.scala-sbt.org/) build and are thus buildable as part of the entire project. In the [master branch](https://github.com/twitter/finatra/tree/master/examples) (or a [release branch](https://github.com/twitter/finatra/tree/finatra-2.2.0/examples)) these examples can be built using their invididual [sbt](http://www.scala-sbt.org/) (or [Maven](http://maven.apache.org/)) build files.

Please take a look through the examples for more detailed information on features, testing, and building with sbt (or Maven).

## <a class="anchor" name="concepts" href="#concepts">Concepts</a>
===============================

Finatra internally uses the Google [Guice](https://github.com/google/guice) dependency injection library extensively which is also available for service writers if they choose to use dependency injection.

**NOTE: You are not required to use Guice dependency injection when using Finatra**. Creating servers, wiring in controllers and applying filters can all be done without using any dependency injection. However, you will not be able to take full-advantage of Finatra's [testing](/finatra/user-guide/testing) features.

An example of Finatra's dependency-injection integration is adding controllers to Finatra's [HttpRouter](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala) *by type*:

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

Upon *graceful* shutdown, all registered `onExit {...}` blocks are executed (see [`c.t.util.App#exits`](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala#L69)). This includes closing the external interface(s), the admin interface, and firing the [`c.t.inject.TwitterModuleLifecycle#singletonShutdown`](https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala#L25) on all installed modules. See the next section for more information on [Modules](#modules).


### <a class="anchor" name="modules" href="#modules">Modules</a>
===============================

Modules are used in conjunction with dependency injection to specify *how* to instantiate an instance of a given type. They are especially useful when instantiation of an instance is dependent on some type of external configuration (see: [Flags](/finatra/user-guide/getting-started#flags)).

We provide a [`c.t.inject.TwitterModule`](https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala) base class which extends the capabilities of the excellent Scala extensions for Google Guice provided by [codingwell/scala-guice](https://github.com/codingwell/scala-guice).

#### Module Definition
* [twitter/util](https://github.com/twitter/util) [Flags](#flags) can be defined inside modules. This allows for re-usable scoping of external configuration that can be composed into a server via the module.
* Prefer using `@Provides`-annotated methods over using the [*toInstance* bind DSL](https://github.com/google/guice/wiki/InstanceBindings).
* Usually modules are Scala *objects* since they contain no state and makes usage of the module less verbose.
* Remember to add `@Singleton` to your `@Provides` method if you require only **one** instance per JVM process.
* Generally, modules are only required for instantiating classes that you don't control. Otherwise, you would simply add the [JSR-330](https://github.com/google/guice/wiki/JSR330) annotations directly to the class. For example, suppose you need to create an `ThirdPartyFoo` class which comes from a thirdparty library. E.g., you want to instantiate a new [Finagle Client](http://twitter.github.io/finagle/guide/Clients.html) (and perhaps externalize the `dest` or `label` parameters to be settable via a command line [flag](#flags)). 

You could create the following module to construct a singleton `ThirdPartyFoo` class which is created with a key provided through a command line flag.

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

You would then be able to inject an instance of type `ThirdPartyFoo` into a class by anotating the constructor of said class with the `@Inject` annotation:

```scala
class MyService @Inject() (
  thirdPartyFoo: ThirdPartyFoo) {
  ...
}
```
<div></div>

Note that the `@Inject` annotation can be considered "metadata" in this usage. Nothing prevents you from instantiating `MyService` directly (and passing it a `thirdPartyFoo` instance). But when an instance of `MyService` is asked for from the injector the `@Inject` annotation tells the injector it should attempt to provide the constructor arguments -- instantiating any classes as necessary. Because we created a module with an `@Provides` annotated method for constructing a `ThirdPartyFoo`, the injector is thus able to satisfy construction of `MyService` when asked. For more information on constructor injection see the Guide [documentation](https://github.com/google/guice/wiki/Injections#constructor-injection).

#### Modules Depending on Other Modules

There may be times where you would like to reuse state defined in a given module inside another module. For
instance, we have a module which defines a configuration flag that is useful in other context. As an
example, let's assume we have a module which defines a flag for a *client id* String that is necessary for constructing
different clients:

```scala
object ClientIdModule extends TwitterModule {
  flag[String]("client.id", "System-wide client id for identifying this server as a client to other services.")
}
```
<div></div>

You could choose to build and provide every client which needs this *client id* in the same module or you could decide to break up the client
creation into separate modules. If you do the latter, how do you get access to the set *client id* value from the `ClientIdModule`? 
Typically, you would inject this value where you need it annotated with the [`@Flag` binding annotation](#at-flag). And you can do the same within 
a module; however instead of the injection point being a constructor annotated with `@Inject`, it is the argument list to a method annotated with `@Provides`:

```scala
object ClientAModule extends TwitterModule {
  override val modules = Seq(ClientIdModule)

  @Singleton
  @Provides
  def provideClientA(
    @Flag("client.id") clientId): ClientA = {
    new ClientA(clientId)
  }
}
```
<div></div>

What's happening here? 

Firstly, we define a `ClientAModule` and override the `modules` val to be a `Seq` of modules that
includes the `ClientIdModule`. This guarantees that if the `ClientIdModule` is not mixed into the list of modules for a server (see
the next section "Module Configuration in Servers"), the `ClientAModule` ensures it will be installed since it's declared as a dependency. 
This ensures that there will be a bound value for the `client.id` flag. Finatra will de-dupe all modules before installing, so it's OK if a module 
appears twice in the server configuration (though you should strive to make this the exception). 

Secondly, we then define a method which provides a `ClientA`. Since injection is by type (and the argument list to an `@Provides` annotated method in a module is an injection point) and `String` is not specific enough we use the [`@Flag` binding annotation](#at-flag). See the next sections for more information on [flags](#flags) and [binding annotations](#binding-annotations).

We could continue this through another module. For example, if we wanted to provide a `ClientB` which needs both the `clientId` and a
`ClientA` we could define a `ClientBModule`:

```scala
object ClientBModule extends TwitterModule {
  override val modules = Seq(
    ClientIdModule,
    ClientAModule)

  @Singleton
  @Provides
  def provideClientB(
    @Flag("client.id") clientId,
    clientA: ClientA): ClientB = {
    new ClientB(clientId, clientA)
  }
}
```
<div></div>

Notice that we choose to list both the `ClientIdModule` and `ClientAModule` in the modules for the `ClientBModule`. Yet, since we
know that the `ClientAModule` includes the `ClientIdModule` we could have choosen to leave it out. The "provides" method in the module
above takes in both a `clientId` String and a `ClientA`. Since it declares the two modules we're assured that these types will be available
from the injector for our "provides" method to use.

#### Module Configuration in Servers

A server is then configured with a list of modules:

```scala
class Server extends HttpServer {
  override val modules = Seq(
    MyModule1,
    MyModule2,
    ClientIdModule,
    ClientAModule,
    ClientBModule)

  ...
}
```
<div></div>

How explicit to be in listing the modules for your server is up to you. If you include a module that is all ready included by another module, Finatra
will de-dupe the module list so there is no penalty but you may want to prefer to define your list of modules as [DRY](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) as possible. For more information on server configuration see the sections on building either [HTTP](/finatra/user-guide/build-new-http-server) or [Thrift](/finatra/user-guide/build-new-thrift-server) servers.

#### Module Lifecycle

Modules also have hooks into the Server lifecycle through the [`c.t.inject.TwitterModuleLifecycle`](https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala) which allows for a module to specify startup and shutdown functionality that is re-usable and scoped to the context of the Module.

If your module provides a resource that requires one-time start-up or initialization you can do this by implementing the `singletonStartup` method in your TwitterModule. Conversely, if you want to clean up resources on graceful shutdown of the server you can implement the `singletonShutdown` method of your TwitterModule to close or shutdown any resources provided by the module.

Additionally, there is also the `TwitterModule#singletonPostWarmupComplete` method which allows modules to hook into the server lifecycle after external ports have been bound, clients have been resolved, and the server is ready to accept traffic but before the `App#run` or `Server#start` callbacks are invoked.

See the [Server Lifecycle](#lifecycle) diagram for a more visual depiction of the server lifecycle.

### <a class="no-pad-anchor" name="binding-annotations" href="#binding-annotations">Binding Annotations</a>
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

Finatra supports the use of [twitter/util](https://github.com/twitter/util) [Flags](https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala) as supported within the [TwitterServer](http://twitter.github.io/twitter-server/Features.html#flags) lifecycle. Flags by their definition represent some external configuration that is passed to the server and are thus an excellent way to parameterize external configuration that may be environment specific, e.g., a database host or URL that is different per environment, *production*, *staging*, *development*, or *jills-staging*. This allows you to work with the same application code in different environments.

This type of configuration parameterization is generally preferred over hardcoding logic by a type of "*environment*" string within code (e.g. `if (env == "production") { ... }`). It is generally good practice to make flags granular controls that are fully orthogonal to one another. They can then be independently managed for each deploy and this scales consistently as the number of supported "environments" scales.

Flags can be [defined](https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/modules/DoEverythingModule.scala#L13) within a [Module](#module) to allow for scoping of reusable external configuration. Though, you can also choose to define a flag [directly in a server](https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/DoEverythingServer.scala#L22). When defined within a [Module](#module), flags can be used to aid in the construction of an instance to be *provided* to the object graph, e.g., a DatabaseConnection instance that use the database URL flag as an input. The module is then able to tell Guice how to provide this object when injected by defining an `@Provides` annotated method.

In Finatra, we also provide a way to override the objects provided on the object graph through "override modules". See the "Override Modules" section in [testing](/finatra/testing#override-modules).

#### <a class="anchor" name="at-flag" href="#at-flag">`@Flag` annotation</a>
Flag is a [binding annotation](#binding-annotations). This annotation allows flag values to be injected into classes (and provider methods), by using the `@Flag` annotation:

Define a flag (in this case within a `TwitterModule`)

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
```
<div></div>

The flag can then be injected as a constructor-arg to a class. When the class is obtained from the injector the correctly parse flag value will be injected. Note, you can also always instantiate this class manually. When doing so, you obviously will need to pass all the constructor args manually including a value for the flag argument.

```scala
class MyService @Inject()(
  @Flag("key") key: String) {
}
```
<div></div>

**NOTE**: when defining a flag, you can dereference that flag directly within the module or server (instead of using the `@Flag` annotation), e.g.:

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

When testing

`TwitterModule#flag` is parameterized to return a Flag of type `T` where `T` is the type of the argument passed as the default. If you do not specify a default value then you must explicitly parameterize your call to `TwitterModule#flag`, e.g,

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

### <a class="anchor" name="futures" href="#futures">Futures</a> (`c.t.util.Future` vs. `scala.concurrent.Future`)
===============================

Finatra, like other frameworks based on Twitter's [Finagle](https://twitter.github.io/finagle), uses the [twitter/util](https://github.com/twitter/util) [`c.t.util.Future`](https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala) class. Twitter's `com.twitter.util.Future` is similar to, but predates, [Scala's](http://docs.scala-lang.org/overviews/core/futures.html) [`scala.concurrent.Future`](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future) (introduced in Scala 2.10 and later backported to Scala 2.9.3) and is *not* compatible without using a [bijection](https://github.com/twitter/bijection) to transform one into the other. It is important to remember that **Finatra uses and expects** [`c.t.util.Future`](https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala).

For more information on [twitter/bijection](https://github.com/twitter/bijection) it is highly recommended that you read the [bijection README](https://github.com/twitter/bijection/blob/develop/README.md). For more information on converting between a `c.t.util.Future` and a `scala.concurrent.Future` see this [thread](https://groups.google.com/forum/#!searchin/finaglers/UtilBijections/finaglers/DjCqv1Vyw0Q/IWwh9y43CAAJ).

A simple example,

```scala
import com.twitter.bijection.Conversion._
import com.twitter.bijection.twitter_util.UtilBijections.twitter2ScalaFuture
import com.twitter.util.{Future => TwitterFuture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future => ScalaFuture}

val scalaFuture: ScalaFuture[T] = {...} // some call that returns scala.concurrent.Future[T]
scalaFuture.as[TwitterFuture[T]]        // this converts the scala.concurrent.Future[T] to a c.t.util.Future[T]
```
<div></div>

<nav>
  <ul class="pager">
  <li class="previous"><a href="/finatra/user-guide/twitter-server-basics"><span aria-hidden="true">&larr;</span>&nbsp;TwitterServer&nbsp;Basics</a></li>
    <li class="next"><a href="/finatra/user-guide/logging">&nbsp;Logging&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
