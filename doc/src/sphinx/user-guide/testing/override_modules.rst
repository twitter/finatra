.. _override_modules:

Override Modules
================

For basic information on Modules in Finatra, see `Modules <../getting-started/modules.html>`__.

Defining a module is generally used to tell `Guice <https://github.com/google/guice>`__ *how* to
instantiate an object to be provided to the object graph. When testing, however, we may want to
provide an alternative instance of a type to the object graph. For instance, instead of making network
calls to an external service through a real client we want to instead use a mock version of the client.
Or load an in-memory implementation to which we can keep a reference in order to make assertions on
its internal state. In these cases we can compose a server with a collection of override modules that
selectively replace bound instances.

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


With a `Module <../getting-started/modules.html>`__ that provides the implementation of `ServiceA`
to the injector:

.. code:: scala

    object MyServiceAModule extends TwitterModule {
      val key = flag("key", "defaultkey", "The key to use.")

      @Singleton
      @Provides
      def providesServiceA: ServiceA = {
        new ServiceA(key())
      }
    }


In order to test, you may want to use a mock or stub version of `ServiceA` in your controller instead
of the real version. You could do this by writing a re-usable module for testing and compose it into
the server when testing by including it as an override module.

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


An "override module" does what it sounds like. It overrides any bound instance in the object graph
with the version it provides. As seen above, the `StubServiceAModule` provided a version of `ServiceA`
that happens to be a stub. In this manner the main server does not need to change and we can replace
parts of its object graph during testing.

Note, modules used specifically for testing should be placed alongside your test code (as opposed to
with your production code) to prevent any mistaken production usage of a test module. Also, it not always
necessary to create a test module (see: `Explicit Binding with #bind[T] <bind_dsl.html>`__ section)
for use as an override module. However, we encourage creating a test module when the functionality
provided by the module is re-usable across your codebase.

Also note, that you can always create an override module over a mock, however it is generally preferable
to want control over the expected mock behavior per-test and as such it's more common to keep a
reference to a mock and use it with the `Explicit Binding with #bind[T] <bind_dsl.html>`__
functionality in a test.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`feature_tests`
- :doc:`integration_tests`
- :doc:`startup_tests`
- :doc:`mocks`
- :doc:`mixins`
- :doc:`bind_dsl`
