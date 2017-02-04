.. _modules:

Modules
=======

Modules are used in conjunction with dependency injection to specify *how* to instantiate an instance of a given type. They are especially useful when instantiation of an instance is dependent on some type of external configuration (see: `Flags <flags.html>`__).

We provide the `c.t.inject.TwitterModule <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala>`__ base class which extends the capabilities of the excellent Scala extensions for Google Guice provided by `codingwell/scala-guice <https://github.com/codingwell/scala-guice>`__.

Defining Modules
----------------

Generally, modules are only required for helping the injector instantiate classes that you don't control. Otherwise, you would simply add the `JSR-330 <https://github.com/google/guice/wiki/JSR330>`__ annotations (e.g., ``@Inject``, ``@Singleton``) directly to the class definition.

For example, if we wanted to declare an HTTP `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__ of type `Service[Request, Response]` as a constructor-arg to a `MyService` class:

.. code:: scala

    class MyService @Inject() (
      httpClient: Service[Request, Response]) {
      ...
    }

Adding the ``@Inject`` documents that the `MyService` class participates in dependency injection. Note that the ``@Inject`` annotation can be considered "metadata". Nothing prevents you from instantiating `MyService` manually
and passing it a `Service[Request, Response]` instance.

However, when an instance of `MyService` is requested to be provided by the injector, the injector will attempt to provide all constructor arguments from the object graph -- instantiating classes as necessary.

The injector will look for public no-arg constructors failing if it cannot find one. In this case, `c.t.finagle.Service` *does not* have a public no-arg constructor so the injector needs help to satisfy the `MyService` constructor
(for more information on constructor injection see the Guice `documentation <https://github.com/google/guice/wiki/Injections#constructor-injection>`__).

To provide this assistance, we can create a module which defines a provider of this type to the object graph for the injector to use.

.. code:: scala

    object MyModule1 extends TwitterModule {
      val dest = flag(name = "client.dest", default = "defaultDestIfNoneProvided", help = "The client dest to use.")
      val label = flag(name = "client.label", default = "defaultLabelIfNoneProvided", help = "The client label to use.")

      @Singleton
      @Provides
      def providesHttpClient: Service[Request, Response] = {
        Http.newClient(dest = dest(), label = label())
      }
    }

Here we define a module to construct a singleton `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__ of type `Service[Request, Response]` which uses `flag values provided through command line arguments <flags.html#passing-flag-values-as-command-line-arguments>`__ to set the values of `label` and `dest`.

``@Provides``
^^^^^^^^^^^^^

The instance is provided by a module method annotated with ``@Provides`` (`source <https://github.com/google/guice/blob/master/core/src/com/google/inject/Provides.java>`__) which serves as a provider of the type to the object graph.

Thus for the above example, we have now provided a way for the injector to construct an instance of type `Service[Request, Response]` allowing the injector to satisfy construction of `MyService` when the `MyModule1` is added to the server's list of modules.

See `Module Configuration in Severs <#module-configuration-in-servers>`__.


Using Flags in Modules
^^^^^^^^^^^^^^^^^^^^^^

As seen in the example above, `TwitterUtil Flags <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala>`__ can be defined inside modules.
This allows for re-usable scoping of external configuration that can be composed into a server via the module. See the section on `Flags <flags.html>`__ for more information.

Best Practices
^^^^^^^^^^^^^^

-  We recommend that you prefer using ``@Provides``-annotated methods over using the `toInstance` `bind DSL <https://github.com/google/guice/wiki/InstanceBindings>`__.
-  Modules should usually be defined as Scala *objects* as they typically contain no state and using an object makes usage of the module less verbose.
-  Remember to add ``@Singleton`` to your ``@Provides`` method if you require only **one** instance per JVM process.

Module Configuration in Servers
-------------------------------

A server can be configured with a list of modules:

.. code:: scala

    class Server extends HttpServer {
      override val modules = Seq(
        MyModule1,
        MyModule2,
        ClientIdModule,
        ClientAModule,
        ClientBModule)

      ...
    }


How explicit to be in listing the modules for your server is up to you. If you include a module that is all ready `included by another module <flags.html#modules-depending-on-other-modules>`__, Finatra will de-dupe the module list so there is no penalty, but you may want to prefer to define your list of modules as `DRY <https://en.wikipedia.org/wiki/Don%27t_repeat_yourself>`__ as possible. 

For more information on server configuration see the `HTTP <../http/server.html>`__ or `Thrift <../thrift/server.html>`__ sections.

Module Lifecycle
----------------

Modules also have hooks into the Server lifecycle through the `c.t.inject.TwitterModuleLifecycle <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala>`__ which allows for a module to specify startup and shutdown functionality that is re-usable and scoped to the context of the Module.

If your module provides a resource that requires one-time start-up or initialization you can do this by implementing the `singletonStartup` method in your TwitterModule. Conversely, if you want to clean up resources on graceful shutdown of the server you can implement the `singletonShutdown` method of your TwitterModule to close or shutdown any resources provided by the module.


Additionally, there is also the `TwitterModule#singletonPostWarmupComplete` method which allows modules to hook into the server lifecycle after external ports have been bound, clients have been resolved, and the server is ready to accept traffic but before the `App#run` or `Server#start` callbacks are invoked.

E.g,

.. code:: scala

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

See the `Server Lifecycle <lifecycle.html>`__ diagram for a more visual depiction of the server lifecycle.