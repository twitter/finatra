Creating an injectable |c.t.app.App|_
=====================================

To create an injectable |c.t.app.App|_, first depend on the `inject-app` library. Then use the
`inject framework <../getting-started/framework.html#inject>`__ to create an injectable App. Finatra
provides an injectable version of the |c.t.app.App|_ trait: |c.t.inject.app.App|_.

Extending the |c.t.inject.app.App|_ trait creates an injectable |c.t.app.App|_.

This allows for the use of `dependency injection <../getting-started/basics.html#dependency-injection>`__
in a |c.t.app.App|_ with support for `modules <../getting-started/modules.html>`__ which allows for
`powerful feature testing <../testing/index.html#types-of-tests>`__ of the application.

Scala Example
-------------

.. code:: scala

  import com.google.inject.Module
	import com.twitter.inject.app.App
	import com.twitter.inject.Logging

	object MyAppMain extends MyApp

	class MyApp extends App with Logging  {

	  override val modules: Seq[Module] = Seq(
	    MyModule1)

	  override protected def run(): Unit = {
	    // Core app logic goes here.
	  }
	}

Then to test:

.. code:: scala

  import com.twitter.inject.Test

  class MyAppTest extends Test {

    private val app = EmbeddedApp(new MyApp)

    test("MyApp#runs") {
      app.main("some.flag1" -> "value1", "some.flag2" -> "value2")
    }
  }

Java Example
------------

.. code:: java

  import java.util.Collection;

  import com.google.common.collect.ImmutableList;
  import com.google.inject.Module;

  import com.twitter.inject.app.AbstractApp;

  public class MyApp extends AbstractApp {

      @Override
      public Collection<Module> javaModules() {
          return ImmutableList.<Module>of(
                  MyModule1$.MODULE$);
      }

      @Override
      public void run() {
          // Core app logic goes here.
      }
  }

See the Finatra `examples <https://github.com/twitter/finatra/tree/develop/examples>`__ for detailed examples with tests.

`App#run`
---------

The single point of entry to the |c.t.inject.app.App|_ is the ``#run`` method.

The Finatra `Startup Lifecycle <../getting-started/lifecycle.html#startup>`__ ensures that the injector
will be properly configured before access and provides the ``#run`` method as the function for implementing
the app.

.. |c.t.app.App| replace:: `util-app App`
.. _c.t.app.App: https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala

.. |c.t.inject.app.App| replace:: ``c.t.inject.app.App``
.. _c.t.inject.app.App: https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala
