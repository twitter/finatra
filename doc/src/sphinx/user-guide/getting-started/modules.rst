.. _modules:

Modules
=======

Modules are used in conjunction with dependency injection to specify *how* to instantiate an instance
of a given type. They are especially useful when instantiation of an instance is dependent on some
type of external configuration (see: `Flags <flags.html>`__).

`c.t.inject.TwitterModule <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala>`__
-------------------------------------------------------------------------------------------------------------------------------------------------------

We provide the `c.t.inject.TwitterModule <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala>`__
base class which extends the capabilities of the excellent Scala extensions for Google
`Guice <https://github.com/google/guice>`__ provided by `codingwell/scala-guice <https://github.com/codingwell/scala-guice>`__.

.. admonition:: Best Practices

    See the `Best Practices <#best-practices>`_ section for general guidelines on working with Modules in Finatra.

Differences with Google Guice `Modules <https://github.com/google/guice/wiki/GettingStarted#guice-modules>`_
------------------------------------------------------------------------------------------------------------

The `c.t.inject.TwitterModule <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala>`__
differs from the regular Google Guice `Module <https://github.com/google/guice/wiki/GettingStarted#guice-modules>`_
in two important ways:

- **It layers in the application lifecycle**. That is, a `TwitterModule` has `lifecycle functions <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala>`_
  which are exposed to users in order to tie Module behavior to the application `lifecycle <lifecycle.html>`__.

- **Integration with** `Flags <flags.html>`__.

These differences are why it is **strongly** encouraged that users **do not** manually install
`TwitterModules`. That is, **do not** use `Binder#install <https://google.github.io/guice/api-docs/4.2/javadoc/com/google/inject/Binder.html#install-com.google.inject.Module->`__
inside of `Module#configure()`.

.. code:: scala

    import com.google.inject.{AbstractModule, Provides}
    import com.twitter.inject.TwitterModule
    import com.twitter.inject.annotations.Flag
    import javax.inject.Singleton

    object MyModule1 extends TwitterModule {
      flag(
        name = "some.flag",
        default = "defaultOfSomeFlag",
        help = "The value of a flag to use."
      )

      @Singleton
      @Provides
      def providesFooInstance(
        @Flag("some.flag") someFlag: String,
        @Flag("client.label") label: String
      ): Foo = new Foo(someFlag)
    }

    ...

    object MyModule2 extends TwitterModule {
      override def configure(): Unit = {
        bindSingleton[Bar].toInstance(new Bar())
        install(MyModule1) // DO NOT DO THIS
      }
    }

    class MyModule2 extends AbstractModule {
      override def configure(): Unit = {
        install(MyModule1) // NOR THIS
      }
    }

Installing a `TwitterModule` this way will cause `lifecycle functions <#module-lifecycle>`__ as well
as Flag parsing of `Flag <flag.html>`__ instances defined within the installed `TwitterModule` to be
skipped. In cases where your `TwitterModule` *only* does instance binding, installing a `TwitterModule`
can work but it is **strongly recommended to follow the guidelines** on how to `add <#module-configuration-in-servers>`__
a `TwitterModule` to your application or how to have one `TwitterModule` `depend <#modules-depending-on-other-modules>`__
on another `TwitterModule` (which is akin to installing a Module inside the `configure()` of another
Module like above).

.. important::

    Reminder: It is important that the framework install all `TwitterModules` such that the `lifecycle functions <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala>`_
    are executed in the proper sequence and any `TwitterModule` defined `Flags <flags.html>`__ are
    parsed properly.

More information on defining `Flags` within a `TwitterModule` and the `TwitterModule` lifecycle below.

Defining Modules
----------------

Generally, Modules are for helping the Injector instantiate classes that you don't control.
Otherwise, you would simply add the `JSR-330 <https://github.com/google/guice/wiki/JSR330>`__
annotations (e.g., ``@Inject``, ``@Singleton``) directly to the class definition.

For example, if we wanted to declare an HTTP `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__
of type `Service[Request, Response]` as a constructor-arg to a `MyService` class:

.. code:: scala

    import com.twitter.finagle.Service
    import com.twitter.finagle.http.{Request, Response}
    import javax.inject.Inject

    class MyService @Inject() (
      httpClient: Service[Request, Response]) {
      ???
    }

Adding the ``@Inject`` documents that the `MyService` class participates in dependency injection.
Note that the ``@Inject`` annotation can be considered "metadata". Nothing prevents you from
instantiating `MyService` manually and passing it a `Service[Request, Response]` instance.

.. code:: scala

    val httpClient: Service[Request, Response] = ???

    val svc: MyService = new MyService(httpClient)

However, when an instance of `MyService` is requested to be provided by the Injector, the Injector
will attempt to provide all constructor arguments from the object graph -- instantiating classes as
necessary.

.. code:: scala

    val svc: MyService = injector.instance[MyService]

    // or

    class MyFoo @Inject()(svc: MyService) {
      ???
    }

The Injector will look for public no-arg constructors. If the Injector cannot find
a public no-arg constructor, it attempts to find a `Provider` of the instance. In this case,
`c.t.finagle.Service` *does not* have a public no-arg constructor so the Injector needs help to
satisfy the `MyService` constructor (for more information on constructor injection see the Guice
`documentation <https://github.com/google/guice/wiki/Injections#constructor-injection>`__).

To help the Injector, we can create a Module which defines a provider of this
type to the object graph for the Injector to use.

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.finagle.Service
    import com.twitter.finagle.http.{Request, Response}
    import com.twitter.inject.TwitterModule
    import com.twitter.inject.annotations.Flag
    import javax.inject.Singleton

    object MyModule1 extends TwitterModule {
      flag(
        name = "client.dest",
        default = "defaultDestIfNoneProvided",
        help = "The client dest to use."
      )
      flag(
        name = "client.label",
        default = "defaultLabelIfNoneProvided",
        help = "The client label to use."
      )

      @Singleton
      @Provides
      def providesHttpClient(
        @Flag("client.dest") dest: String,
        @Flag("client.label") label: String
      ): Service[Request, Response] =
        Http.newClient(dest = dest, label = label)
    }

or in Java:

.. code:: java

    import com.google.inject.Provides;
    import com.twitter.finagle.Service;
    import com.twitter.finagle.http.Request;
    import com.twitter.finagle.http.Response;
    import com.twitter.inject.TwitterModule;
    import com.twitter.inject.annotations.Flag;
    import javax.inject.Singleton;

    public class MyModule1 extends TwitterModule {

      public MyModule1() {
        createFlag(
          /*name*/ "client.dest",
          /*default*/ "defaultDestIfNoneProvided",
          /*help*/ "The client dest to use.",
          /*flaggable*/ Flaggable.ofString()
        );

        createFlag(
          /*name*/ "client.label",
          /*default*/ "defaultLabelIfNoneProvided",
          /*help*/ "The client label to use.",
          /*flaggable*/ Flaggable.ofString()
        );
      }

      @Singleton
      @Provides
      public Service<Request, Response> providesHttpClient(
        @Flag("client.dest") String dest,
        @Flag("client.label") String label
      ) {
        return Http.newClient(dest, label);
      }
    }

Here we define a Module to construct a Singleton `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__
of type `Service[Request, Response]` which uses `parsed Flag values provided through command line arguments <flags.html#passing-flag-values-as-command-line-arguments>`__
for the values of `label` and `dest`.

``@Provides``
~~~~~~~~~~~~~

The instance is provided by a Module method annotated with ``@Provides``
(`source <https://github.com/google/guice/blob/master/core/src/com/google/inject/Provides.java>`__)
which serves as a provider of the type to the object graph.

Thus, we have now provided a way for the Injector to construct an instance of type
`Service[Request, Response]` allowing the Injector to satisfy construction of `MyService` when the
`MyModule1` is added to the server's list of Modules.

.. code:: scala

    val svc: MyService = injector.instance[MyService]

Note: if your Module method annotated with ``@Provides`` has an argument list, all arguments to the
method are provided by the Injector (since it is the Injector calling the method in the first place).

Much like an ``@Inject`` annotated constructor, the Injector will attempt to provide all of the method
arguments from the object graph -- instantiating classes as necessary.

For example:

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.inject.TwitterModule
    import javax.inject.Singleton

    object MyModule1 extends TwitterModule {

      @Singleton
      @Provides
      def providesBar(foo: Foo): Bar = {
        new Bar(foo)
      }
   }

The argument `foo: Foo` will be "injected" in the sense that the Injector will attempt to provide
an instance of `foo` when invoking the method.

See `Module Configuration in Servers <#module-configuration-in-servers>`__.

Flags in Modules
~~~~~~~~~~~~~~~~

As seen in the example above, `TwitterUtil Flags <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala>`__
can be defined inside Modules. This allows for re-usable scoping of external configuration that can
be composed into a server via the Module. See the documentation on `Flags <flags.html>`__ for more
information.

.. note::

    In Java, Flag creation is implemented with two explicit methods: `createFlag<T> <https://github.com/twitter/finatra/blob/9a1380eb6527ef9e3d7f6cc0f7ced620217cdca0/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleFlags.scala#L26>`_
    and `createMandatoryFlag<T> <https://github.com/twitter/finatra/blob/9a1380eb6527ef9e3d7f6cc0f7ced620217cdca0/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleFlags.scala#L44>`_.
    Where "mandatory" means a Flag defined with no default value and thus a command line value is
    required, or "mandatory" in order to use the Flag.

Module Configuration in Servers
-------------------------------

A server can be configured with a list of Modules:

.. code:: scala

    import com.google.inject.Module
    import com.twitter.finatra.http.HttpServer

    class Server extends HttpServer {

      override val modules: Seq[Module] = Seq(
        MyModule1,
        MyModule2,
        ClientIdModule,
        ClientAModule,
        ClientBModule)

      ???
    }

and in Java:

.. code:: java

    import com.google.common.collect.ImmutableList;
    import com.google.inject.Module;
    import com.twitter.finatra.http.AbstractHttpServer;
    import java.util.Collection;

    public class Server extends AbstractHttpServer {

      @Override
      public Collection<Module> javaModules() {
        return ImmutableList.<Module>of(
            MyModule1$.MODULE$,
            MyModule2$.MODULE$,
            ClientIdModule$.MODULE$,
            ClientAModule$.MODULE$,
            ClientBModule$.MODULE$);
        )
      }

      ...
    }

How explicit to be in listing the Modules for your server is up to you. If you include a Module that
is already `included by another Module <modules.html#modules-depending-on-other-modules>`__,
Finatra will de-duplicate the Module list so there is no penalty, but you may want to prefer to define
your list of Modules as `DRY <https://en.wikipedia.org/wiki/Don%27t_repeat_yourself>`__ as possible.

Much like declaring dependencies, we recommend that you be explicit in listing all the
Modules that provide bindings used directly by your code.

For more information on server configuration see the `HTTP <../http/server.html>`__ or
`Thrift <../thrift/server.html>`__ sections.

Module Lifecycle
----------------

Modules can hook into the Server lifecycle through the `c.t.inject.TwitterModuleLifecycle <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala>`__
which allows for a Module to specify startup and shutdown functionality that is re-usable and scoped
to the context of the Module.

If your Module provides a resource that requires one-time start-up or initialization you can do this
by implementing the `singletonStartup` method in your TwitterModule. Conversely, if you want to
clean up resources on graceful shutdown of the server you can implement the `singletonShutdown`
method of your TwitterModule to close or shutdown any resources provided by the Module. Thus, you
are able to bind closable resources with a defined way to release them. This allows users to overcome
some of the limitations of a standard `com.google.inject.AbstractModule <https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/AbstractModule.html>`_.

Additionally, there is also the `TwitterModule#singletonPostWarmupComplete` method which allows
Modules to hook into the server lifecycle after external ports have been bound, clients have been
resolved, and the server is ready to accept traffic but *before* the `App#run` or `Server#start`
callbacks are invoked.

E.g,

.. code:: scala

    import com.twitter.inject.{Injector, TwitterModule}

    object MyModule extends TwitterModule {

      override def singletonStartup(injector: Injector) {
        // initialize JVM-wide resources
      }

      override def singletonShutdown(injector: Injector) {
        // shutdown JVM-wide resources
      }

      override def singletonPostWarmupComplete(injector: Injector) {
        // perform functions that need to happen after we've bound 
        // ports but before the server has started
      }
    }

.. important::

    Please note that the lifecycle is for **Singleton**-scoped resources and users should still
    avoid binding unscoped resources without ways to shutdown or close them.

There is also the option to inline the logic for closing your resource using the
`TwitterModuleLifecycle#closeOnExit(f: => Unit)` function.

For example, assume we have a class, `SomeClient` with a `close()` method that returns a `Future[Unit]`:

.. code:: scala

    class SomeClient(
      configurationParam1: Int,
      configurationParam2: Double) {

      def withAnotherParam(b: Boolean): SomeClient = ???
      def withSomeOtherConfiguration(i: Int): SomeClient = ???

      /** Closes this client, freeing any held resources */
      def close(): Future[Unit] = {
        ???
      }
    }

We could then register a function to close it, which will be run upon graceful shutdown of the application
by doing the following:

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.conversions.DurationOps._
    import com.twitter.inject.{Injector, TwitterModule}
    import com.twitter.inject.annotations.Flag
    import com.twitter.util.Await
    import javax.inject.Singleton

    object MyModule extends TwitterModule {
      flag[Int]("configuration.param1", 42, "This is used to configure an instance of a Wicket")
      flag[Double]("configuration.param2", 123.45d, "This is also used to configure an instance of a Wicket")

      @Provides
      @Singleton
      def providesSomeClient(
        @Flag("configuration.param1") configurationParam1: Int,
        @Flag("configuration.param2") configurationParam2: Double
      ): SomeClient = {
        val client =
          new SomeClient(configurationParam1, configurationParam2)
            .withAnotherParam(b = true)
            .withSomeOtherConfiguration(137)

        closeOnExit {
           Await.result(client.close(), 2.seconds)
        }

        client
      }
    }

This allows for not needing to implement the `singletonShutdown` method which would require that you
obtain an instance of the singleton resource from the Injector to then call the `close()` function.

Any logic passed to the `closeOnExit` function is added to the application's list of `onExit`
functions to be run in the order registered upon graceful shutdown of the application.

For an example, see the `c.t.inject.thrift.modules.ThriftMethodBuilderClientModule <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftMethodBuilderClientModule.scala>`_
where we use `closeOnExit` to ensure that any bound ThriftClient will be closed when the application
gracefully exits.

See the `Application and Server Lifecycle <lifecycle.html>`__ section for more information on the
application and server lifecycle.

Lastly, see Guice's documentation on `Modules should be fast and side-effect free <https://github.com/google/guice/wiki/ModulesShouldBeFastAndSideEffectFree>`__
and `Avoid Injecting Closable Resources <https://github.com/google/guice/wiki/Avoid-Injecting-Closable-Resources>`__
for more thoughts on providing resources with modules.

Modules Depending on Other Modules
----------------------------------

There may be times where you would like to reuse types bound by one Module inside another Module.
For instance, you may have a Module which provides a type `Foo` and need that instance when
constructing a type `Bar` in another Module. E.g.

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.inject.TwitterModule
    import javax.inject.Singleton

    object FooModule extends TwitterModule {

      @Singleton
      @Provides
      def providesFoo: Foo = {
        new Foo(???)
      }
    }

How do you get access to the bound instance of Foo inside of another Module? 

Most often you are trying to inject the bound instance into a class as a class constructor-arg. E.g.,

.. code:: scala

    import javax.inject.{Inject, Singleton}

    @Singleton
    class MyClassFoo @Inject()(foo: Foo) {
      ???
    }

You can do something similar in a Module. However, instead of the injection point being the
constructor annotated with ``@Inject``, it is the argument list of any ``@Provides``-annotated
method.

.. code:: scala

    import com.google.inject.{Module, Provides}
    import com.twitter.inject.TwitterModule
    import javax.inject.Singleton

    object BarModule extends TwitterModule {

      override val modules: Seq[Module] = Seq(FooModule)

      @Singleton
      @Provides
      def providesBar(foo: Foo): Bar = {
        new Bar(foo)
      }
    }

in Java:

.. code:: java

    import com.google.common.collect.ImmutableList;
    import com.google.inject.Module;
    import com.google.inject.Provides;
    import com.twitter.inject.TwitterModule;
    import javax.inject.Singleton;
    import java.util.Collection;

    public class BarModule extends TwitterModule {

      @Override
      public Collection<Module> javaModules() {
        return ImmutableList.<Module>of(
            FooModule$.MODULE$);
      }

      @Singleton
      @Provides
      public Bar providesBar(Foo foo) {
        return new Bar(foo);
      }
    }

What's happening here?

Firstly, we've defined a `BarModule` that overrides the `modules` val with a `Seq` (or the
`javaModules` def with a `Collection` in Java) of Modules that includes the `FooModule`. This
guarantees that if the `FooModule` is not mixed into the list of Modules for a server, the `BarModule`
ensures it will be installed since it's declared as a dependency and thus there will be a bound
instance of `Foo` available for use in providing an instance of `Bar`.

Finatra will de-duplicate all Modules before installing, so it is OK if a Module appears twice in the
server configuration, though you should strive to make this the exception.

Secondly, we've defined a method which provides a `Bar` instance and add an argument of type `Foo`
which will be provided by the Injector since injection is by type and the argument list to an
``@Provides`` annotated method in a Module is an injection point.

Why? 

Because the Injector is what calls the `providesBar` method. When the Injector needs to provide an
instance of `Bar` it looks for a "provider" of `Bar` in the list of Modules. It will thus try to
supply all arguments to the function from the object graph.

We could continue this through another Module. For example, if we wanted to provide a `Baz` which
needs both a `Foo` and a `Bar` instance we could define a `BazModule`:

.. code:: scala

    import com.google.inject.{Module, Provides}
    import com.twitter.inject.TwitterModule
    import javax.inject.Singleton

    object BazModule extends TwitterModule {

      override val modules: Seq[Module] = Seq(
        FooModule,
        BarModule)

      @Singleton
      @Provides
      def providesBaz(
        foo: Foo,
        bar: Bar): Baz = {
        new Baz(foo, bar)
      }
    }

in Java:

.. code:: java

    import com.google.common.collect.ImmutableList;
    import com.google.inject.Module;
    import com.google.inject.Provides;
    import com.twitter.inject.TwitterModule;
    import javax.inject.Singleton;
    import java.util.Collection;

    public class BazModule extends TwitterModule {

      @Override
      public Collection<Module> javaModules() {
        return ImmutableList.<Module>of(
            FooModule$.MODULE$,
            BarModule$.MODULE$);
      }

      @Singleton
      @Provides
      public Baz providesBaz(Foo foo, Bar bar) {
        return new Baz(foo, bar);
      }
    }

Notice that we choose to list both the `FooModule` and `BarModule` in the Modules for the `BazModule`.
Yet, since we know that the `BarModule` includes the `FooModule` we could have choosen to leave it
out. The `providesBaz` method in the Module above takes in both `Foo` and a `Bar` instances as
arguments.

Since it declares the two Modules, we're assured that instances of these types will be available
from the Injector for our `providesBaz` method to use.

Best Practices
--------------

-  Do not install a `TwitterModule` within another Module via `Module#configure` using `Binder#install <https://google.github.io/guice/api-docs/4.2/javadoc/com/google/inject/Binder.html#install-com.google.inject.Module->`__.
   Installing a `TwitterModule` with this mechanism will skip all `lifecycle functions <#module-lifecycle>`_
   and any `Flags <./flags.html>`_ defined within the `TwitterModule` **will not be parsed**.
-  We recommend that you prefer using ``@Provides``-annotated methods over using the `toInstance`
   `bind DSL <https://github.com/google/guice/wiki/InstanceBindings>`__.
-  In Scala, Modules should usually be defined as Scala *objects* as they typically contain no state and using
   an object makes use of the Module less verbose.
-  Remember to add ``@Singleton`` to your ``@Provides`` method if you require only **one** instance
   per JVM process.
-  Avoid `cyclic dependencies <basics.html#avoid-cyclic-dependencies>`_.
-  Avoid `conditional logic <basics.html#avoid-conditional-logic-in-modules>`_ in a `TwitterModule`.
-  Make use of the `TwitterModule` `lifecycle  <#module-lifecycle>`_.
-  Make use of the `TestInjector <../testing/integration_tests.html#id2>`_ for integration testing
   with `TwitterModules` as this will correctly handle the lifecycle and Flag parsing of
   `TwitterModules` to create a `c.t.inject.Injector`.
