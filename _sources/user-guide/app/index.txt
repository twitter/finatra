.. _app:

Creating an injectable |c.t.app.App|_
=====================================

Lifecycle
---------

If you haven't already, take a look at the |c.t.app.App|_
`lifecycle documentation <../getting-started/lifecycle.html#c-t-app-app-lifecycle>`__.

Getting Started
---------------

To create an injectable |c.t.app.App|_, first depend on the `inject-app` library. Then use the
`inject framework <../getting-started/framework.html#inject>`__ to create an injectable App. Finatra
provides an injectable version of the |c.t.app.App|_ trait: |c.t.inject.app.App|_.

Extending the |c.t.inject.app.App|_ trait creates an injectable |c.t.app.App|_.

This allows for the use of `dependency injection <../getting-started/basics.html#dependency-injection>`__
in a |c.t.app.App|_ with support for `modules <../getting-started/modules.html>`__ which allows for
`powerful feature testing <../testing/index.html#types-of-tests>`__ of the application.

Scala Example
-------------

Given a `module <../getting-started/modules.html>`__ definition:

.. code:: scala

  import com.twitter.inject.TwitterModule

  object MyModule1 extends TwitterModule {

    @Singleton
    @Provides
    def providesFooService: FooService = ???
  }

You could define an |c.t.inject.app.App|_:

.. code:: scala

  import com.google.inject.Module
  import com.twitter.inject.Logging
  import com.twitter.inject.app.App
  import com.twitter.inject.modules.LoggingModule

  object MyAppMain extends MyApp

  class MyApp extends App with Logging  {

    override val modules: Seq[Module] = Seq(
      LoggingModule,
      MyModule1)

    override protected def run(): Unit = {
      // Core app logic goes here.
      val fooService = injector.instance[FooService]
      ???
    }
  }

.. tip::

  We include the `c.t.inject.modules.LoggingModule <https://github.com/twitter/finatra/blob/develop/inject/inject-modules/src/main/scala/com/twitter/inject/modules/LoggerModule.scala>`__ to attempt installation of the 
  `SLF4JBridgeHandler <https://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html>`__ here as an example of
  how to `bridge legacy APIs <https://www.slf4j.org/legacy.html>`__.

Then to test:

.. code:: scala

  import com.twitter.inject.Test
  import com.twitter.inject.app.EmbeddedApp

  class MyAppTest extends Test {

    private val app = new EmbeddedApp(new MyApp)

    test("MyApp#runs") {
      app.main("some.flag1" -> "value1", "some.flag2" -> "value2")
    }
  }

Java Example
------------

.. code:: java

  import java.util.Collection;
  import java.util.Collections;

  import com.google.inject.Module;

  import com.twitter.inject.app.AbstractApp;
  import com.twitter.inject.modules.LoggerModule$;

  public class MyApp extends AbstractApp {

      @Override
      public Collection<Module> javaModules() {
          return Collections.<Module>singletonList(
                  LoggerModule$.MODULE$,
                  MyModule1$.MODULE$);
      }

      @Override
      public void run() {
          // Core app logic goes here.
          FooService fooService = 
            injector().instance(FooService.class);
      }
  }

Then create a "main" class:

.. code:: java

  final class MyAppMain {
      private MyAppMain() {
      }

      public static void main(String[] args) {
          new MyApp().main(args);
      }
  }  

See the Finatra `examples <https://github.com/twitter/finatra/tree/develop/examples>`__ for detailed examples with tests.

`App#run`
---------

The single point of entry to the |c.t.inject.app.App|_ is the ``#run`` method.

The Finatra `Startup Lifecycle <../getting-started/lifecycle.html#startup>`__ ensures that the injector
will be properly configured before access and provides the ``#run`` method as the function for implementing
the app.

Using Injection
---------------

The |c.t.inject.app.App|_ exposes access to the configured `c.t.inject.Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__ which can be used to obtain instances from
the object graph.

.. note:: 

    You should take care to only access the injector in the `App#run` function as this is the correct place in the
    `lifecycle <../getting-started/lifecycle.html#c-t-app-app-lifecycle>`__ where you are ensured to have a fully 
    configured `c.t.inject.Injector`. Accessing the `c.t.inject.Injector` too early in the lifecycle (before it is 
    configured) will result in an Exception being thrown. 

An Example of Handling Signals
------------------------------

There may be cases where you want your application to handle an 
`IPC signal <https://en.wikipedia.org/wiki/Signal_(IPC)>`__ instead of closing normally
once the code execution is done, e.g., handling an `INT` (Ctrl-C) or `TSTP` (Ctrl-Z) signal. 

You can use the 
`com.twitter.util.HandleSignal <https://github.com/twitter/util/blob/aaa3d6e2bf37a3bd565a8c51187fd9b6db8a0b25/util-core/src/main/scala/com/twitter/util/Signal.scala#L75>`__ utility to apply a callback to run on receiving 
the signal.

.. note::

  Please consult the scaladocs for `com.twitter.util.HandleSignal <https://github.com/twitter/util/blob/aaa3d6e2bf37a3bd565a8c51187fd9b6db8a0b25/util-core/src/main/scala/com/twitter/util/Signal.scala#L75>`__
  to make sure you are aware of the limitations of the code in handling signals.

For example, to exit the application upon receiving an `INT` signal:

.. code:: scala

  import com.google.inject.Module
  import com.twitter.inject.app.App
  import com.twitter.inject.Logging

  object MyAppMain extends MyApp

  class MyApp extends App with Logging  {

    override val modules: Seq[Module] = Seq(
      MyModule1)

    HandleSignal("INT") { signal =>
      exitOnError(s"Process is being terminated with signal $signal.")
    }

    override protected def run(): Unit = {
      // Core app logic goes here.
      val fooService = injector.instance[FooService]
      ???
    }
  }

.. |c.t.app.App| replace:: `util-app App`
.. _c.t.app.App: https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala

.. |c.t.inject.app.App| replace:: ``c.t.inject.app.App``
.. _c.t.inject.app.App: https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala
