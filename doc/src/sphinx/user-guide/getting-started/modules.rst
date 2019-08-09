.. _modules:

Modules
=======

Modules are used in conjunction with dependency injection to specify *how* to instantiate an instance
of a given type. They are especially useful when instantiation of an instance is dependent on some
type of external configuration (see: `Flags <flags.html>`__).

We provide the `c.t.inject.TwitterModule <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala>`__
base class which extends the capabilities of the excellent Scala extensions for Google
`Guice <https://github.com/google/guice>`__ provided by `codingwell/scala-guice <https://github.com/codingwell/scala-guice>`__.

Defining Modules
----------------

Generally, modules are only required for helping the injector instantiate classes that you don't
control. Otherwise, you would simply add the `JSR-330 <https://github.com/google/guice/wiki/JSR330>`__
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

However, when an instance of `MyService` is requested to be provided by the injector, the injector
will attempt to provide all constructor arguments from the object graph -- instantiating classes as
necessary.

The injector will look for public no-arg constructors. If the injector cannot find
a no-arg constructor, it attempts to find a `Provider` of the instance. In this case,
`c.t.finagle.Service` *does not* have a public no-arg constructor so the injector needs help to
satisfy the `MyService` constructor (for more information on constructor injection see the Guice
`documentation <https://github.com/google/guice/wiki/Injections#constructor-injection>`__).

To help the injector, we can create a module which defines a provider of this
type to the object graph for the injector to use.

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.finagle.Service
    import com.twitter.finagle.http.{Request, Response}
    import com.twitter.inject.TwitterModule
    import javax.inject.Singleton

    object MyModule1 extends TwitterModule {
      val dest =
        flag(
          name = "client.dest",
          default = "defaultDestIfNoneProvided",
          help = "The client dest to use."
        )
      val label =
        flag(
          name = "client.label",
          default = "defaultLabelIfNoneProvided",
          help = "The client label to use."
        )

      @Singleton
      @Provides
      def providesHttpClient: Service[Request, Response] = {
        Http.newClient(dest = dest(), label = label())
      }
    }

or in Java:

.. code:: java

    import com.google.inject.Provides;
    import com.twitter.finagle.Service;
    import com.twitter.finagle.http.Request;
    import com.twitter.finagle.http.Response;
    import com.twitter.inject.TwitterModule;
    import javax.inject.Singleton;

    public class MyModule1 extends TwitterModule {
      private Flag<String> dest = createFlag(
        /*name*/ "client.dest",
        /*default*/ "defaultDestIfNoneProvided",
        /*help*/ "The client dest to use.",
        /*flaggable*/ Flaggable.ofString());

      private Flag<String> label = createFlag(
        /*name*/ "client.label",
        /*default*/ "defaultLabelIfNoneProvided",
        /*help*/ "The client label to use.",
        /*flaggable*/ Flaggable.ofString());

      @Singleton
      @Provides
      public Service<Request, Response> providesHttpClient() {
        return Http.newClient(dest.apply(), label.apply());
      }
    }

Here we define a module to construct a singleton `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__
of type `Service[Request, Response]` which uses `flag values provided through command line arguments <flags.html#passing-flag-values-as-command-line-arguments>`__
to set the values of `label` and `dest`.

``@Provides``
^^^^^^^^^^^^^

The instance is provided by a module method annotated with ``@Provides``
(`source <https://github.com/google/guice/blob/master/core/src/com/google/inject/Provides.java>`__)
which serves as a provider of the type to the object graph.

Thus for the above example, we have now provided a way for the injector to construct an instance of
type `Service[Request, Response]` allowing the injector to satisfy construction of `MyService` when
the `MyModule1` is added to the server's list of modules.

Note: if your module method annotated with ``@Provides`` has an argument list, all arguments to the
method are provided by the injector (since it is the injector calling the method in the first place).

Much like an ``@Inject`` annotated constructor, the injector will attempt to provide all of the method
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

The argument `foo: Foo` will be "injected" in the sense that the injector will attempt to provide
an instance of `foo` when invoking the method.

See `Module Configuration in Severs <#module-configuration-in-servers>`__.

Using Flags in Modules
^^^^^^^^^^^^^^^^^^^^^^

As seen in the example above, `TwitterUtil Flags <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala>`__
can be defined inside modules. This allows for re-usable scoping of external configuration that can
be composed into a server via the module. See the section on `Flags <flags.html>`__ for more information.

Best Practices
^^^^^^^^^^^^^^

-  We recommend that you prefer using ``@Provides``-annotated methods over using the `toInstance`
   `bind DSL <https://github.com/google/guice/wiki/InstanceBindings>`__.
-  Modules should usually be defined as Scala *objects* as they typically contain no state and using
   an object makes usage of the module less verbose.
-  Remember to add ``@Singleton`` to your ``@Provides`` method if you require only **one** instance
   per JVM process.

Module Configuration in Servers
-------------------------------

A server can be configured with a list of modules:

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

How explicit to be in listing the modules for your server is up to you. If you include a module that
is already `included by another module <modules.html#modules-depending-on-other-modules>`__,
Finatra will de-duplicate the module list so there is no penalty, but you may want to prefer to define
your list of modules as `DRY <https://en.wikipedia.org/wiki/Don%27t_repeat_yourself>`__ as possible.

For more information on server configuration see the `HTTP <../http/server.html>`__ or
`Thrift <../thrift/server.html>`__ sections.

.. note::

    In Java, Flag creation is implemented with two explicit methods: `createFlag<T> <https://github.com/twitter/finatra/blob/9a1380eb6527ef9e3d7f6cc0f7ced620217cdca0/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleFlags.scala#L26>`_
    and `createMandatoryFlag<T> <https://github.com/twitter/finatra/blob/9a1380eb6527ef9e3d7f6cc0f7ced620217cdca0/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleFlags.scala#L44>`_.
    Where "mandatory" means a Flag defined with no default value meaning a user-supplied value is
    required, or mandatory in order to use the Flag.

Module Lifecycle
----------------

Modules can hook into the Server lifecycle through the `c.t.inject.TwitterModuleLifecycle <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala>`__
which allows for a module to specify startup and shutdown functionality that is re-usable and scoped
to the context of the Module.

If your module provides a resource that requires one-time start-up or initialization you can do this
by implementing the `singletonStartup` method in your TwitterModule. Conversely, if you want to
clean up resources on graceful shutdown of the server you can implement the `singletonShutdown`
method of your TwitterModule to close or shutdown any resources provided by the module.


Additionally, there is also the `TwitterModule#singletonPostWarmupComplete` method which allows
modules to hook into the server lifecycle after external ports have been bound, clients have been
resolved, and the server is ready to accept traffic but before the `App#run` or `Server#start`
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

See the `Application and Server Lifecycle <lifecycle.html>`__ section for more information on the
application and server lifecycle.

Modules Depending on Other Modules
----------------------------------

There may be times where you would like to reuse types bound by one module inside another module.
For instance, you may have a Module which provides a type `Foo` and need that instance when
constructing a type `Bar` in another module. E.g.

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

You can do something similar in a module. However, instead of the injection point being the
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
`javaModules` def with a `Collection` in Java) of modules that includes the `FooModule`. This
guarantees that if the `FooModule` is not mixed into the list of modules for a server, the `BarModule`
ensures it will be installed since it's declared as a dependency and thus there will be a bound
instance of `Foo` available for use in providing an instance of `Bar`.

Finatra will de-duplicate all modules before installing, so it is OK if a module appears twice in the
server configuration, though you should strive to make this the exception.

Secondly, we've defined a method which provides a `Bar` instance and add an argument of type `Foo`
which will be provided by the Injector since injection is by type and the argument list to an
``@Provides`` annotated method in a module is an injection point.

Why? 

Because the injector is what calls the `providesBar` method. When the injector needs to provide an
instance of `Bar` it looks for a "provider" of `Bar` in the list of modules. It will thus try to
supply all arguments to the function from the object graph.

We could continue this through another module. For example, if we wanted to provide a `Baz` which
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

Notice that we choose to list both the `FooModule` and `BarModule` in the modules for the `BazModule`.
Yet, since we know that the `BarModule` includes the `FooModule` we could have choosen to leave it
out. The `providesBaz` method in the module above takes in both `Foo` and a `Bar` instances as
arguments.

Since it declares the two modules, we're assured that instances of these types will be available
from the injector for our `providesBaz` method to use.
