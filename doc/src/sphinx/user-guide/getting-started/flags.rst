.. _flags:

Flags
=====

Finatra builds on the support of `TwitterUtil <https://github.com/twitter/util>`__ `Flags <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala>`__
from `TwitterServer <https://twitter.github.io/twitter-server/Features.html#flags>`__, adding the
ability to inject parsed Flag values into your classes.

Flags by their definition represent some external configuration that is passed to the server and are
thus an excellent way to parameterize configuration that may be environment specific, e.g., a
database host or URL that is different per environment: *production*, *staging*, *development*, or
*jills-staging*. Having a strict separation of configuration from code allows for deploying the
exact same application code across different execution environments which maximizes the portability
of your code.

This type of configuration parameterization is generally preferred over hardcoding logic by a type
of "*environment*\ " string within code, e.g.

.. code:: bash

  if (env == "production") { ... }

It is generally good practice to make Flags *granular* controls that are fully orthogonal to one
another. They can then be independently managed for each deploy and this scales consistently as the
number of supported  "environments" scales.

.. admonition:: Best Practices

    See the `Best Practices <#id4>`_ section for general guidelines on working with Flags in Finatra.

Global Flags
------------

`TwitterUtil <https://github.com/twitter/util>`__ `Flags <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala>`__
also has the concept of a "global" flag. That is, a flag that is "global" to the JVM process (as it is
generally defined as a Scala object). In the discussion of Flags with Finatra we **do not** mean
"global" flags unless it is explicitly stated.

See the `scaladoc <https://twitter.github.io/util/docs/com/twitter/app/GlobalFlag.html>`__ for
`c.t.app.GlobalFlag` for more information.

But I have a lot of Flags
-------------------------

If you find that you end up with a lot of Flags to configure for your service and your deployment
system provides no relief -- there are still alternatives to moving external configuration into code.

See `this thread <https://groups.google.com/forum/#!searchin/finatra-users/typesafe$20config%7Csort:relevance/finatra-users/kkZgI5dG9CY/lzDPAmUxAwAJ>`__
on using `Lightbend Config <https://github.com/lightbend/config>`__ with Finatra.

When Are Flags Parsed?
----------------------

It is important to understand *when* in the application lifecycle Flag values are parsed from command
line input into an instance of the the Flag's defined `Flaggable[T] <https://github.com/twitter/util/blob/ed6f6a73a41d1b7e8331687567e3191cd5ead19e/util-app/src/main/scala/com/twitter/app/Flag.scala#L55>`__ type `T`.

.. admonition:: Overview

  Flag values are parsed from command line input *after* the `c.t.app.App#init` application lifecycle
  phase and *before* the `c.t.app.App#premain` lifecycle phase.

Flags are not parsed during class loading or initialization, rather, Flag parsing happens
*in-between* the running of all registered `init()` functions and the running of all registered
`premain` functions. Thus care should be taken when attempting to access a Flag value to ensure it
is not done until at least the **premain** application lifecycle phase.

For more information on the application lifecycle see the documentation `here <lifecycle.html>`__.

`c.t.app.App#failfastOnFlagsNotParsed`
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Note that Finatra defaults the `c.t.app.App#failfastOnFlagsNotParsed <https://github.com/twitter/util/blob/5e326a1109e2cd608515ce87badfb792bd346a3d/util-app/src/main/scala/com/twitter/app/App.scala#L57>`_
option as mentioned in the `TwitterServer documentation <https://twitter.github.io/twitter-server/Features.html#flags>`__ to
**'true'**. This is done in the Finatra extension `c.t.inject.app.App <https://github.com/twitter/finatra/blob/c1b49edebb0ad513f2b3439ee4f2f5e0541e2b26/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L127>`__
which overrides the superclass implementation with **'true'**.

Having this option turned on means that if a Flag value is accessed before the Flag has been parsed
then an `IllegalStateException` will be thrown. This "fail fast" behavior is preferable over silently
reading a default value by mistake and not a parsed value from the command line.

Modules
^^^^^^^

Flags created within a Finatra `TwitterModule` are collected and added to the application's
`c.t.app.Flags <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flags.scala>`_
collection during the `init` lifecycle phase. Thus, they will not inherit the application's setting
for "fail fast" until then.

The default for the "fail fast" behavior of Flags created within a Finatra `TwitterModule` is governed
by the value set for `c.t.inject.TwitterModuleFlags#failfastOnFlagsNotParsed` in the `TwitterModule`
which is also defaulted to **'true'** to mirror the application container default.

This means that like a Flag created with the application, any attempt to access a `TwitterModule`
created Flag's value too eagerly will also result in an `IllegalStateException` being thrown.

.. important::

    We highly recommend that all applications keep `c.t.inject.app.App#failfastOnFlagsNotParsed <https://github.com/twitter/finatra/blob/c1b49edebb0ad513f2b3439ee4f2f5e0541e2b26/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L127>`_
    and `c.t.inject.TwitterModuleFlags#failfastOnFlagsNotParsed <https://github.com/twitter/finatra/blob/8435309bd5d729537db4960e4f09d55b537fc75b/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleFlags.scala#L29>`_
    not only set to the **same value** but also **kept to their default of 'true'**.

    This should arguably be the default behavior for Flag instances but for legacy reasons is not.

Flags With No Command Line Value
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If no command line input is given for a defined Flag, accessing the Flag value should still not
occur until after parsing of command line input has been attempted. The `c.t.app.Flags <https://github.com/twitter/util/blob/ed6f6a73a41d1b7e8331687567e3191cd5ead19e/util-app/src/main/scala/com/twitter/app/Flags.scala#L89>`__
instance within your application is stateful and is only fully initialized *after parsing* of any
command line input and thus Flags should not be considered "ready" for accessing until this step
has completed.

If the Flag defines a default, the `default value <https://github.com/twitter/util/blob/ed6f6a73a41d1b7e8331687567e3191cd5ead19e/util-app/src/main/scala/com/twitter/app/Flag.scala#L186>`__ will be returned when no command line value is
given. If the Flag is defined without a default and no command line value is given, any attempt to
read the Flag's value via the `apply()` function will fail with an `IllegalArgumentException`.

For more information on defining and accessing Flags without default values, see
`here <#flags-without-defaults>`__.

How To Define Flags
-------------------

Flags should be defined as part of instance creation/instantiation (i.e., via the constructor) in 
either a `c.t.inject.TwitterModule` or an application (any extension of `c.t.app.App`) and thus 
declared before command line input has been parsed.

Within an App or a Server
~~~~~~~~~~~~~~~~~~~~~~~~~

While Flags are most typically `defined <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/modules/DoEverythingModule.scala#L13>`__
within a `TwitterModule <modules.html>`__ to allow for scoping of reusable external configuration
(since Modules are meant to be re-usable), you can also choose to define a Flag
`directly in an or a server <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/DoEverythingServer.scala#L22>`__.

In this case within an `HttpServer <../http/server.html>`__,

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import com.twitter.finatra.http.{Contoller, HttpServer}
    import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.inject.annotations.Flag
    import javax.inject.Inject

    class ExampleController @Inject()(
      @Flag("magic.num") magicNum: String
    ) extends Controller {
        get("/foo") { request: Request =>
          magicNum
        }
    }

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      flag("magic.num", "42", "Defines a magic number flag.")

      override val modules = Seq(
        DoEverythingModule)

      override def configureHttp(router: HttpRouter): Unit =
        router
          .filter[LoggingMDCFilter[Request, Response]]
          .filter[TraceIdMDCFilter[Request, Response]]
          .filter[CommonFilters]
          .add[ExampleController]
    }

or in Java:

.. code:: java

    import DoEverythingModule;
    import ExampleController;
    import com.google.inject.Module;
    import com.twitter.finagle.http.Request;
    import com.twitter.finatra.http.AbstractController;
    import com.twitter.finatra.http.AbstractHttpServer;
    import com.twitter.finatra.http.filters.CommonFilters;
    import com.twitter.finatra.http.filters.LoggingMDCFilter;
    import com.twitter.finatra.http.filters.TraceIdMDCFilter;
    import com.twitter.finatra.http.routing.HttpRouter;
    import com.twitter.inject.annotations.Flag
    import java.util.Collection;
    import java.util.Collections;
    import javax.inject.Inject;
    import scala.reflect.ManifestFactory;

    public class ExampleController extends AbstractController {
      private final String magicNum;

      @Inject
      public ExampleController() {
        @Flag("magic.num") String magicNum) {
        this.magicNum = magicNum;
      }

      public void configureRoutes() {
        get("/foo", (Request request) -> magicNum)
      }
    }

    ...

    public final class ExampleServerMain {
      private ExampleServerMain() {
      }

      public static void main(String[] args) {
        new ExampleServer().main(args);
      }
    }

    ...

    public class ExampleServer extends AbstractHttpServer {

      public ExampleServer() {
        createFlag(
          /* name      = */ "magic.num",
          /* default   = */ 42,
          /* help      = */ "Defines a magic number flag.",
          /* flaggable = */ Flaggable.ofJavaInteger());
      }

      @Override
      public Collection<Module> javaModules() {
        return Collections.singletonList((
            new DoEverythingModule());
      }

      @Override
      public void configureHttp(HttpRouter router) {
        router
          .filter(ManifestFactory.classType(LoggingMDCFilter.class))
          .filter(ManifestFactory.classType(TraceIdFilter.class))
          .filter(CommonFilters.class)
          .add(ExampleController.class)
    }

The parsed value of the Flag, `magic.num` would be available to be injected where necessary using
the |@Flag|_ binding annotation.

Or it can be obtained directly from the Injector:

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.inject.annotations.Flags
    import javax.inject.Inject

    // Note: we define our Controller without the `@Inject()` annotation on the construction,
    // thus args need to always be passed in since the injector will not be able to instantiate.
    // Also note: this is just an example.
    class ExampleController(magicNum: String) {
        get("/foo") { request: Request =>
          ???
        }
    }

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      flag("magic.num", "42", "Defines a magic number flag.")

      override val modules = Seq(
        DoEverythingModule)

      override def configureHttp(router: HttpRouter): Unit =
        router
          .filter[LoggingMDCFilter[Request, Response]]
          .filter[TraceIdMDCFilter[Request, Response]]
          .filter[CommonFilters]
          .add(new ExampleController(injector.instance[String](Flags.named("magic.num"))))
    }

Within a `TwitterModule <modules.html>`__
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When defined within a `TwitterModule <modules.html>`__, Flags can be used to aid in the construction
of an instance to be `provided to the object graph <modules.html#provides>`__, e.g., a `DatabaseConnection`
instance with the database URL specified by a Flag. The module is then able to tell the Injector how
to provide an instance of this type when necessary by defining an ``@Provides`` annotated method.
More information on defining Modules can be found `here <modules.html>`__.

In Finatra, we also provide a way to override bound instances in the object graph when testing
through `Override Modules <../testing/index.html#override-modules>`__ or by using
`Embedded Server #bind[T] <../testing/index.html##embedded-server-bind-t>`__.

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.inject.TwitterModule
    import com.twitter.inject.annotations.Flag
    import javax.inject.Singleton

    class MyModule extends TwitterModule {
      flag(name = "key", default = "default", help = "The key to use")

      @Provides
      @Singleton
      def provideFoo(
        @Flag("key") key: String): Foo = {
        new Foo(key)
      }
    }

and in Java:

.. code:: java

    import com.google.inject.Provides;
    import com.twitter.inject.TwitterModule;
    import com.twitter.inject.annotations.Flag;
    import javax.inject.Singleton;

    public class MyModule extends TwitterModule {

      public MyModule() {
        createFlag(
          /* name      = */ "key",
          /* default   = */ "default",
          /* help      = */ "The key to use",
          /* flaggable = */ Flaggable.ofString());
      }

      @Provides
      @Singleton
      public Foo provideFoo(
        @Flag("key") String key) {
        return new Foo(key);
      }
    }

In the examples above, notice that we **do not save a local reference to the created Flag** but instead
reference its value by use of the |@Flag|_ binding annotation or by obtaining the parsed value
directly from the Injector.

|@Flag| annotation
^^^^^^^^^^^^^^^^^^

|@Flag|_ is a `binding annotation <../getting-started/binding_annotations.html>`__. This annotation
allows parsed Flag values to be injected into classes (and provider methods).

Understanding Flag Binding
^^^^^^^^^^^^^^^^^^^^^^^^^^

The key component of Flag binding is |Flaggable[T]|_, a type-class that defines how a Flag of
type `T` can be parsed from a given string. In Finatra, you can bind / inject any Flag value as
long as you register its corresponding `Flaggable` instance within a framework middleware (e.g.,
a module). The most common `Flaggable` instances are already registered so primitive types as well as
comma-separated lists (either as `scala.Seq` or `java.util.List` of primitive types will work off
the shelf.

These additional `Flag` types are also registered (as both primitive and comma-separated):

 - `java.net.InetSocketAddress`
 - `java.time.LocalTime`
 - `com.twitter.util.Duration`
 - `com.twitter.util.Time`
 - `com.twitter.util.StorageUnit`

For anything that falls outside of these types (and their `scala.Seq[_]` and `java.util.List[_]`)
you'd need to register a `Flaggable[T]` to be able to inject a Flag of type `T`.

.. code:: scala

    import com.twitter.inject.TwitterModule

    class MyModule extends TwitterModule {
       def configure(): Unit = {
         addFlagConverter[List[(Int, Int)]]
       }
    }

And in Java:

.. code:: java

    import com.twitter.inject.TwitterModule;
    import java.util.List;
    import com.google.inject.TypeLiteral;
    import com.twitter.app.Flaggable;

    public class MyModule extends TwitterModule {

      @Override
      public void configure() {
        addFlagConverter(
          new TypeLiteral<List<scala.Tuple2<Integer, Integer>>>() {},
          Flaggable.ofJavaList(Flaggable.ofTuple(Flaggable.ofJavaInteger, Flaggable.ofJavaInteger)
        );
      }
    }

Holding a Reference
^^^^^^^^^^^^^^^^^^^

When defining a Flag you can also dereference the Flag value directly within the Module or server
(in lieu of using the |@Flag|_ annotation). However, you should be **extremely cautious** when doing
so.

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.inject.TwitterModule
    import com.foo.bar.ThirdPartyFoo
    import javax.inject.Singleton

    object MyModule1 extends TwitterModule {
      val key = flag(name = "key", default = "default", help = "The key to use")

      @Singleton
      @Provides
      def provideThirdPartyFoo: ThirdPartyFoo = {
        new ThirdPartyFoo(key())
      }
    }

.. warning::

    This is potentially dangerous. See the next sections for details.

Caution
+++++++

.. important::

    Flags are distinct by name only.

Note that holding onto a reference of a Flag can be potentially dangerous since Flag definitions
can be overridden with another definition. Flags are distinct `by name only <https://github.com/twitter/util/blob/ed6f6a73a41d1b7e8331687567e3191cd5ead19e/util-app/src/main/scala/com/twitter/app/Flags.scala#L251>`__.
The Flag you are referencing can be replaced in the stateful `c.t.app.Flags <https://github.com/twitter/util/blob/ed6f6a73a41d1b7e8331687567e3191cd5ead19e/util-app/src/main/scala/com/twitter/app/Flags.scala#L89>`__
instance of your application with another instance created with the same name. The last Flag added
wins and thus when the Flags are parsed your reference may not be updated with the parsed value,
resulting in the reference retaining its default value or no value if it has no specified default.

You should only do this if you are guaranteed that the Flag defined for which you keep a
reference will not be redefined making your reference obsolete.

Even More Caution
+++++++++++++++++

.. important::

    Eagerly evaluating a Flag value before the Flag has been parsed will not always fail.

Additionally, having a reference can lead to unintentionally trying to dereference the Flag value
before the command-line value has been parsed. If the Flag has a reasonable default, your code
may even appear to work until the passed command-line value is changed, which will have no effect on
the Flag because the Flag is being evaluated too early in the `Application Lifecycle <./lifecycle.html#c-t-inject-app-app-lifecycle>`_.

The recommendation is to not hold a reference to a created Flag and instead obtain the parsed Flag
value via injection. See the `Flag Value Injection <#flag-value-injection>`_ section for details.

Ok, But I Want To Live Dangerously
++++++++++++++++++++++++++++++++++

If you find you must keep a local reference to the created Flag, then you can use the `Flag#apply <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L171>`__,
`Flag#get <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L205>`__
or other methods, depending, to obtain the parsed Flag value. Again, this is not recommended and
caution should be exercised when using Flags in this manner to respect the application lifecycle
with regards to `when Flags are parsed <#when-are-flags-parsed>`_.

Flag Value Injection
^^^^^^^^^^^^^^^^^^^^

The parsed value of a Flag can be injected as a constructor-arg to a class using the |@Flag|_
`binding annotation <./binding_annotations.html>`_. When the class is obtained from the Injector,
the correctly parsed Flag value will be injected.

.. code:: scala

    import com.twitter.inject.annotations.Flag
    import javax.inject.Inject

    class MyService @Inject()(
      @Flag("key") key: String) {
    }

Note, you can also always instantiate the above class manually. When doing so, you will need to pass
all the constructor args manually including a value for the |@Flag|_ annotated argument.

.. code:: scala

    val svc: MyService = new MyService(key = "foo")

You can also ask the Injector directly for a Flag value using `Flags.named` (similar to Guice's
|Names.named|_):

.. code:: scala

    import com.twitter.inject.Injector
    import com.twitter.inject.annotations.Flags

    val key: String = injector.instance[String](Flags.named("key"))

.. caution:: Attempting to get a Flag value from the Injector for a Flag **without** a default
    nor a user-specified value will result in a `ProvisionException`.

Flag Value Injection Benefits
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A side-effect of not holding onto Flag references in a `TwitterModule` is that it increases the possibility
of using the `@Provides`-annotated method in a non-injection context. Everything needed to construct
the returned type can be defined as an argument to the method, essentially making the `@Provides`-annotated
method a type of `Factory method <https://en.wikipedia.org/wiki/Factory_method_pattern>`_.

When the `@Provides`-annotated method directly applies a held Flag reference it means the method
is tied to the lifecycle of the Flag reference. The method cannot be properly called until the Flag
reference has been parsed.

Removing usage of a held Flag reference and instead allowing the Flag value to be `injected <#id3>`_
(like `any other needed dependency <modules.html#provides>`_) means the method can be used
independently of the Flag lifecycle or even injection.

For example, if we had a class `Notifier`:

.. code:: scala

    import com.twitter.util.Duration

    class Notifier(
      connection: DatabaseConnection,
      emailer: Emailer ,
      serializer: Serializer,
      notificationFrequency: Duration)

and a module, `NotifierModule`:

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.conversions.DurationOps._
    import com.twitter.inject.TwitterModule
    import com.twitter.inject.annotations.Flag
    import com.twitter.util.Duration
    import javax.inject.Singleton

    object NotifierModule extends TwitterModule {
      flag(name = "frequency.interval.minutes", default = 60.minutes, help = "Interval for notifications")

      @Provides
      @Singleton
      def provideNotifier(
        connection: DatabaseConnection,
        emailer: Emailer ,
        serializer: Serializer,
        @Flag("frequency.interval.minutes") interval: Duration): Notifier = {
        new Notifier(
          connection,
          emailer,
          serializer,
          interval)
      }
    }

You could use this Module in non-injection context -- like providing a test fixture, since you have
static utility to construct a `Notifier` over its necessary parts. That is, you could do something
along the lines of:

.. code:: scala

      import com.twitter.conversions.DurationOps._

      val mockDatabaseConnection: DatabaseConnection = mock[DatabaseConnection]
      val mockEmailer: Emailer = mock[Emailer]
      val mockSerializer: Serializer = mock[Serializer]

      val notifierStub: Notifier =
        NotifierModule.provideNotifier(
          connection = mockDatabaseConnection,
          emailer = mockEmailer
          serializer = mockSerializer
          notificationFrequency = 10.seconds)

See the `The Tao of Testing: Chapter 3 - Dependency Injection <https://jasonpolites.github.io/tao-of-testing/ch3-1.1.html>`__
for more information and examples of Dependency Injection approaches to writing testable code.

Flags Without Defaults
----------------------

Flags defined without a default value are typically considered to be "mandatory" flags. That is, a
command line value MUST be supplied and it is not expected that the server will be able to correctly
function without a supplied value.

When creating a Flag, the returned Flag instance is parameterized to the type of the supplied default
argument, e.g., the method signature looks like this:

.. code:: scala

    import com.twitter.app.{Flag, Flaggable}

    def apply[T: Flaggable](name: String, default: => T, help: String): Flag[T]

Thus when you do not specify a default value, you must *explicitly* parameterize calling `flag[T](...)`
with a defined type `T`.

E.g.,

A Bad Example
~~~~~~~~~~~~~

.. code:: scala

    import com.foo.bar.ThirdPartyFoo
    import com.google.inject.Provides
    import com.twitter.inject.TwitterModule
    import javax.inject.Singleton

    object MyModule1 extends TwitterModule {
      val key = flag[String](name = "key", help = "The key to use")

      @Singleton
      @Provides
      def provideThirdPartyFoo: ThirdPartyFoo = {
        val myKey = key.get match {
          case Some(value) => value
          case _ => "DEFAULT"
        }
        new ThirdPartyFoo(myKey)
      }
    }

Keep in mind that the specified `T` in this case must be a `Flaggable <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flaggable.scala>`__
type. `Flag#get` will return a `None` when no value is passed on the command line for a Flag with no
default.

.. warning::

    This is not a recommended way of defining or using a Flag within a TwitterModule but is
    included to show aspects of the Flag API. Please see the section on why `holding a reference <#holding-a-reference>`_ to a Flag in a Module is dangerous and why this is not recommended.

Note that you should not call `Flag#apply <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L171>`__
on a Flag without a default (as this will result in an `IllegalArgumentException`) but instead use
`Flag#get <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L205>`__ or `Flag#getWithDefault <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala#L213>`__
which return an `Option[T]` on which you can then pattern match.

Injection
~~~~~~~~~

Flags without a default nor a user-supplied value will fail injection (since de-referencing the value
in this case results in an `IllegalArgumentException`). This means if you try to inject the value of
a non-defaulted Flag that has not been supplied a value from the command-line using the |@Flag|_
binding annotation, a `ProvisionException` will be thrown caused by the `IllegalArgumentException`
`here <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/inject/inject-app/src/main/scala/com/twitter/inject/app/internal/FlagsModule.scala#L34>`__.

A Better Example
~~~~~~~~~~~~~~~~

A better example of injecting a parsed value from a Flag defined without a default:

.. code:: scala

    import com.foo.bar.ThirdPartyFoo
    import com.google.inject.Provides
    import com.twitter.inject.TwitterModule
    import com.twitter.inject.annotations.Flag
    import javax.inject.Singleton

    object MyModule1 extends TwitterModule {
      flag[String](name = "key", help = "The key to use")

      @Singleton
      @Provides
      def provideThirdPartyFoo(
        @Flag("key") myKey: String): ThirdPartyFoo =
        new ThirdPartyFoo(myKey)
    }

and in Java:

.. code:: java

    import com.foo.bar.ThirdPartyFoo;
    import com.google.inject.Provides;
    import com.twitter.inject.TwitterModule;
    import com.twitter.inject.annotations.Flag;
    import javax.inject.Singleton;

    public final class MyModule1 extends TwitterModule {

      public MyModule1() {
        createMandatoryFlag(
          /* name      = */ "key",
          /* help      = */ "The key to use",
          /* usage     = */ "Pass -key=value",
          /* flaggable = */ Flaggable.ofString());
      }

      @Singleton
      @Provides
      public ThirdPartyFoo provideThirdPartyFoo(
        @Flag("key") String myKey) {
        return new ThirdPartyFoo(myKey);
      }
    }

In this example, we are assured that we will have the parsed Flag value obtained from the Injector
in our `@Provides`-annotated method. If there is no supplied command-line value this will fail
(as mentioned previously) at server startup with a `ProvisionException`. Thus, this ensures that
we cannot start the server without a command-line value being supplied. This fits the contract
of a Flag defined without a default in that the Flag is meant to be treated as **required** for the
server.

Modules Depending on Other Modules - Flags Edition
--------------------------------------------------

As we saw in the `Modules section <modules.html#modules-depending-on-other-modules>`__, Modules can
"depend" on other Modules. In that case we wanted an already bound type for use in another Module.

Flags are special since they are bound to the object graph by the framework due to the fact that
their values are parsed from the command line at a specific point in the server lifecycle. But the
principle is the same. What if we have a Module which defines a configuration Flag that is useful
in other contexts?

As an example, let's assume we have a Module which defines a Flag for the service's "Client Id"
String -- how it identifies itself as a client to other services -- that is necessary for
constructing different clients:

.. code:: scala

    import com.twitter.inject.TwitterModule

    object ClientIdModule extends TwitterModule {
      flag[String]("client.id", "System-wide client id for identifying this server as a client to other services.")
    }


You could choose to build and provide every client which needs the `client.id` Flag value in the
same Module, e.g.,

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.inject.TwitterModule
    import javax.inject.Singleton

    object ClientsModule extends TwitterModule {
      val clientIdFlag = flag[String]("client.id", "System-wide client id for identifying this server as a client to other services.")

      @Singleton
      @Provides
      def provideClientA: ClientA = {
        new ClientA(clientIdFlag())
      }

      @Singleton
      @Provides
      def provideClientB: ClientB = {
        new ClientB(clientIdFlag())
      }

      @Singleton
      @Provides
      def provideClientC: ClientC = {
        new ClientA(clientIdFlag())
      }
    }

But this starts to break down as your add more clients, especially if each client in turn requires
specific configuration or Flags in order to be constructed. For the purposes of encapsulation, we'd
want to collocate all the relevant Flags and logic to create a given client into it's own re-usable
Module, thus allowing them to be used and tested independently.

If we do so, then how do we get access to the parsed `client.id` Flag value from the `ClientIdModule`
inside of another Module?

Most often you are trying to inject the Flag value into a class using the |@Flag|_
`binding annotation <binding_annotations.html>`__ on a class constructor-arg. E.g.,

.. code:: scala

    import com.twitter.inject.annotations.Flag
    import javax.inject.{Inject, Singleton}

    @Singleton
    class MyClassFoo @Inject() (
      @Flag("client.id") clientId: String) {
      ???
    }

You can do something similar in a Module. However, instead of the injection point being the
constructor annotated with ``@Inject``, it is the argument list of any ``@Provides``-annotated
method.

E.g.,

.. code:: scala

    import ClientIdModule
    import com.google.inject.{Module, Provides}
    import com.twitter.inject.TwitterModule
    import com.twitter.inject.annotations.Flag
    import javax.inject.Singleton

    object ClientAModule extends TwitterModule {
      override val modules: Seq[Module] = Seq(ClientIdModule)

      @Singleton
      @Provides
      def provideClientA(
        @Flag("client.id") clientId: String): ClientA = {
        new ClientA(clientId)
      }
    }

of in Java:

.. code:: java

    import ClientIdModule$;
    import com.google.inject.Module;
    import com.google.inject.Provides;
    import com.twitter.inject.TwitterModule;
    import com.twitter.inject.annotations.Flag;
    import java.util.Collection;
    import java.util.Collections;
    import javax.inject.Singleton;

    public final class ClientAModule extends TwitterModule {

      @Override
      public Collection<Module> javaModules() {
        return Collections.singletonList((
            ClientIdModule$.MODULE$);
      }

      @Singleton
      @Provides
      public ClientA provideClientA(
        @Flag("client.id") String clientId) {
        return new ClientA(clientId);
      }
    }

What's happening here?

Firstly, we've defined a `ClientAModule` and override the `modules` val to be a `Seq` of Modules
that includes the `ClientIdModule`. This guarantees that if the `ClientIdModule` is not mixed into
the list of Modules for a server, the `ClientAModule` ensures it will be installed since it's
declared as a dependency.

This ensures that there will be a bound value for the `ClientId` Flag. Otherwise, our Module
definition is brittle in that we are trying to make use of a Flag which may never be defined
within the scope of our server. With TwitterUtil Flags, trying to use an undefined Flag
`could cause your server to fail to start <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flags.scala#L118>`__.

Thus we want to ensure that:

a. we are only using Flags we define in our Module or
b. we include the Module that does.

Note that it is an `error to try to define the same Flag twice <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flags.scala#L251>`__.

Finatra will de-dupe all Modules before installing, so it is OK if a Module appears twice in the
server configuration, though you should strive to make this the exception.

.. important::

    Reminder: It is important that the framework install all `TwitterModules` such that the `lifecycle functions <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala>`_
    are executed in the proper sequence and any `TwitterModule` defined `Flags <flags.html>`__ are
    parsed properly.

    Thus users SHOULD NOT install a `TwitterModule` within another Module via `Module#configure
    using `Binder#install <https://google.github.io/guice/api-docs/4.2/javadoc/com/google/inject/Binder.html#install-com.google.inject.Module->`__.

Secondly, we've defined a method which provides a `ClientA`. Since injection is by type (and the
argument list to an ``@Provides`` annotated method in a Module is an injection point) and `String`
is not specific enough we use the |@Flag|_ `binding annotation <binding_annotations.html>`__.

We could continue this through another Module. For example, if we wanted to provide a `ClientB`
which needs both the `ClientId` and a `ClientA` we could define a `ClientBModule`:

.. code:: scala

    import ClientIdModule
    import ClientAModule
    import com.google.inject.{Module, Provides}
    import com.twitter.inject.TwitterModule
    import com.twitter.inject.annotations.Flag
    import javax.inject.Singleton

    object ClientBModule extends TwitterModule {
      override val modules: Seq[Module] = Seq(
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

or in Java:

.. code:: java

    import ClientIdModule$;
    import com.google.inject.Module;
    import com.google.inject.Provides;
    import com.twitter.inject.TwitterModule;
    import com.twitter.inject.annotations.Flag;
    import java.util.Arrays;
    import java.util.Collection;
    import java.util.Collections;
    import javax.inject.Singleton;

    public final class ClientBModule extends TwitterModule {

      @Override
      public Collection<Module> javaModules() {
        return Collections.unmodifiableList(
          Arrays.asList(
            ClientIdModule$.MODULE$,
            ClientAModule$.MODULE$));
      }

      @Singleton
      @Provides
      public ClientB provideClientB(
        @Flag("client.id") clientId,
        clientA: ClientA) {
        return new ClientB(clientId, clientA);
      }
    }

Notice that we choose to include both the `ClientIdModule` and `ClientAModule` in the list of Modules
for the `ClientBModule`. Yet, since we know that the `ClientAModule` includes the `ClientIdModule`
we could have chosen to leave it out.

The `provideClientB` method in the Module above takes in both a `ClientId` String and a `ClientA`.
Since it declares the two Modules, we're assured that these types will be available from the
Injector for our `provideClientB` method to use.

This is just an Example
-----------------------

Note that usage of a `client.id` Flag is just an example. In Finatra, we provide a
`ThriftClientIdModule <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftClientIdModule.scala>`__
for binding a `c.t.finagle.thrift.ClientId` type so that you do not need to rely on the Flag value.

You'll see that this type is then expected to be bound in other Modules like the
`FilteredThriftClientModule <https://github.com/twitter/finatra/blob/ec8d584eb914f50f92314c740dc68fb7abb47eff/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/FilteredThriftClientModule.scala#L234>`__
which is a utility for building filtered thrift clients.

The framework does not assume that you are using the
`ThriftClientIdModule <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftClientIdModule.scala>`__
for providing the bound `ClientId` type thus the `FilteredThriftClientModule <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/FilteredThriftClientModule.scala>`__
does **not** specify the `ThriftClientIdModule` in it's list of Modules to allow users to bind an
instance of the `ClientId` type in any manner they choose.

Passing Flag Values as Command-Line Arguments
---------------------------------------------

Flags are set by passing them as arguments to your java application. E.g.,

.. code:: bash

    $ java -jar finatra-http-server-assembly-2.0.0.jar -key=value

An example of this is passing the `-help` Flag to see usage for running a Finatra server, e.g.

.. code:: bash

    $ java -jar finatra-http-server-assembly-2.0.0.jar -help
    HelloWorldServer
      -alarm_durations='1.seconds,5.seconds': 2 alarm durations
      -help='false': Show this help
      -admin.port=':8080': Admin http server port
      -bind=':0': Network interface to use
      -log.level='INFO': Log level
      -log.output='/dev/stderr': Output file
      -key='default': The key to use

Best Practices
--------------

-  Prefer defining Flags as *granular* as possible. i.e., do not define an "environment" Flag which is
   then used to choose application functionality in code. It is considered best practice
   to keep configuration separated from code [`1 <https://12factor.net/config>`_, `2 <https://microservices.io/patterns/externalized-configuration.html>`_, `3 <https://dzone.com/articles/microservices-externalized-configuration>`_].
-  Prefer to define all Flags which help to configure an application resource in the `TwitterModule`
   which provides the resource to the object graph.
-  Do not hold a reference to a created Flag unless necessary. Prefer obtaining the parsed Flag
   value from the Injector.
-  If you have a lot of external configuration and your deployment system does not provide ways to
   manage a large amount of application parameters, consider `other mechanisms <#but-i-have-a-lot-of-flags>`_
   for reading and parsing external configuration. But prefer to keep the application configuration
   externalized and not moved into the code.
-  Make use of the `TestInjector <../testing/integration_tests.html#id2>`_ for integration testing
   with `TwitterModules` as this will correctly handle the lifecycle and Flag parsing of
   `TwitterModules` to create a `c.t.inject.Injector`.

.. |@Flag| replace:: ``@Flag``
.. _@Flag: https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/java/com/twitter/inject/annotations/Flag.java

.. |Flaggable[T]| replace:: ``Flaggable[T]``
.. _Flaggable[T]: https://github.com/twitter/util/blob/1bdeab56e49015c1f4c097ef76e47b93a079a239/util-app/src/main/scala/com/twitter/app/Flaggable.scala#L19

.. |Names.named| replace:: `Names.named`
.. _Names.named: https://github.com/google/guice/blob/master/core/src/com/google/inject/name/Names.java
