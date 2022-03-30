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
provides an injectable version of the |c.t.app.App|_ trait: |c.t.inject.app.App|_. We
also recommend using `Logback <https://logback.qos.ch/>`__ as your `SLF4J <https://www.slf4j.org/manual.html>`__
implementation.

E.g., with sbt:

.. parsed-literal::

    "com.twitter" %% "inject-app" % "\ |release|\ ",
    "ch.qos.logback" % "logback-classic" % versions.logback,

For more information on logging with Finatra see: `Introduction to Logging With Finatra <../logging/index.html#introduction-to-logging-with-finatra>`__.

Extending the |c.t.inject.app.App|_ trait creates an injectable |c.t.app.App|_.

This allows for the use of `dependency injection <../getting-started/dependency_injection.html#dependency-injection>`__
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

    /**  Java-friendly way to access this module as a singleton instance */
    def get(): this.type = this
  }

You could define an |c.t.inject.app.App|_:

.. code:: scala

  import com.google.inject.Module
  import com.twitter.inject.app.App

  object MyAppMain extends MyApp

  class MyApp
    extends App {

    override val modules: Seq[Module] = Seq(MyModule1)

    override protected def run(): Unit = {
      // Core app logic goes here.
      val fooService = injector.instance[FooService]
      ???
    }
  }

Java Example
------------

.. code:: java

  import java.util.Collection;
  import java.util.Collections;

  import com.google.inject.Module;

  import com.twitter.inject.app.AbstractApp;

  public class MyApp extends AbstractApp {

      @Override
      public Collection<Module> javaModules() {
          return Collections.<Module>singletonList(MyModule1.get());
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

  object MyAppMain extends MyApp

  class MyApp extends App  {

    override val modules: Seq[Module] = Seq(MyModule1)

    HandleSignal("INT") { signal =>
      exitOnError(s"Process is being terminated with signal $signal.")
    }

    override protected def run(): Unit = {
      // Core app logic goes here.
      val fooService = injector.instance[FooService]
      ???
    }
  }

An Example of Handling Console Output
-------------------------------------

There may be cases where your application needs to output information to the console, such as when you are
writing a command-line utility that prints its result when a computation has finished. There are some gotchas
to making console output testable, so Finatra has exposed a `ConsoleWriter <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/console/ConsoleWriter.scala>`__
that can be used by both Java and Scala applications. The `ConsoleWriter` avoids modifying global JVM state, as modifying
`System.setOut()` and/or `System.setErr()` may result in flaky tests.

.. note::

  Finatra exposes Logging integrations, which are a separate concern from the `ConsoleWriter`. The output
  of a Logger may not be appropriate for a command-line utility. The testing/binding of a `Logger`
  is currently beyond the scope of the `ConsoleWriter`. Additionally, `Logger` configuration is
  extremely flexible and there are no guarantees of messages reaching the console.

  For more information on logging with Finatra see: `Introduction to Logging With Finatra <../logging/index.html#introduction-to-logging-with-finatra>`__.

If your implementation is in Scala, the `ConsoleWriter` acts
as a drop in replacement that allows for capturing any `scala.Console` output (i.e. via `println()`) within the
`c.t.inject.app.App#run()` method via the `TestConsoleWriter <#testconsolewriter>`__ without explicit use of the `ConsoleWriter`.

For example,

.. code:: scala

    import com.twitter.inject.app.App
    import com.twitter.inject.console.ConsoleWriter

    class MyApp extends App {

      override def run(): Unit = {
        val console = injector.instance[ConsoleWriter]
        console.out.println("Hello, World!")
        console.err.println("Oops!")
        // println("Hello, World!") will be forwarded to `console`
        // Console.out.println("Hello, World!") will be forwarded to `console`
        // System.out.println("Hello, World!") will NOT be forwarded to `console`
      }
    }

If your implementation is in Java, the `System.out` and `System.err` calls will not be forwarded
to the `ConsoleWriter` automatically, due to their previously mentioned coupling to global JVM state.
For Java users, it would be preferred to have expected console output of the `c.t.inject.app.App#run()`
method use the `ConsoleWriter` directly.

For example:

.. code:: java

    import com.twitter.inject.app.AbstractApp;
    import com.twitter.inject.console.ConsoleWriter;

    public class MyApp extends AbstractApp {

        @Override
        public void run() {
            // Core app logic goes here.
            ConsoleWriter console =
              injector().instance(ConsoleWriter.class);
            console.out().println("Hello, World!");
            console.err().println("Oops!");
            // System.out.println("Hello, World!") will NOT be forwarded to `console`
        }
    }

See `TestConsoleWriter <#testconsolewriter>`__ for testing and verifying `ConsoleWriter` output.

Testing
-------

First extend the |c.t.inject.Test|_ trait. Then to test, wrap your |c.t.inject.app.App|_ with an `c.t.inject.app.EmbeddedApp <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/EmbeddedApp.scala>`__.

For example,

.. code:: scala

    import com.twitter.inject.Test
    import com.twitter.inject.app.EmbeddedApp

    class MyAppTest extends Test {

      // build an EmbeddedApp
      def app(): EmbeddedApp = app(new MyApp)
      def app(underlying: MyApp): EmbeddedApp = 
        new EmbeddedApp(underlying).bind[Foo].toInstance(new Foo(2))

      test("MyApp#run") {
        app().main("username" -> "jack")
      }

      test("MyApp#works as expected") {
        // create a version of the app local to this test
        // here we could change the configuration of the app under test
        val localTestInstanceApp = app(new MyApp)
        localTestInstanceApp.main("username" -> "jill")

        // expect behavior against instances from the `localTestInstanceApp` injector
        localTestInstanceApp.injector.instance[Foo].value should be(Foo(2))
      }
    }

and in Java:

.. code:: java

    import java.util.*;

    import org.junit.Assert;
    import org.junit.Test;

    import com.twitter.inject.app.EmbeddedApp;

    public class MyAppTest extends Assert {
        private EmbeddedApp app() {
            return this.app(new MyApp());
        }
        private EmbeddedApp app(MyApp underlying) {
            return new EmbeddedApp(underlying).bindClass(Foo.class, Foo.apply(2));
        }

        @Test
        public void testRun() {
          Map<String, Object> flags = new HashMap<String, Object>();
          flags.put("username", "jack");

          app().main(flags);
        }

        @Test
        public void testWorksAsExpected() {
          Map<String, Object> flags = new HashMap<String, Object>();
          flags.put("username", "jill");

          // create a version of the app local to this test
          // here we could change the configuration of the app under test
          MyApp localTestInstanceApp = app(new MyApp());
          app(localTestInstanceApp).main(flags);

          // expect behavior against instances from the `localTestInstanceApp` injector
          Foo foo = localTestInstanceApp.injector().instance(Foo.class);
          assertEquals(/* expected */ Foo.apply(2), /* actual */ foo);
        }
    }

.. important::

    Note: every call to `EmbeddedApp#main` will run the application with the given flags. If your application is stateful, 
    you may want to ensure that a new instance of your application under test is created per test run, like written above.

There may be cases where you want to assert some metrics are written by your application for the purpose of testing functionality, even though your application may not export them for collection.

TestConsoleWriter
-----------------

Finatra provides a `ConsoleWriterModule <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/internal/ConsoleWriterModule.scala>`__ which will bind the `ConsoleWriter <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/console/ConsoleWriter.scala>`__ to the object graph.
The `ConsoleWriter` can be bound for testing with the `TestConsoleWriter <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/TestConsoleWriter.scala>`__ in order to inspect the console output of a command-line style `App`.

Assuming you have an App:

.. code:: scala

    import com.twitter.inject.app.App
    import com.twitter.inject.app.console.ConsoleWriter

    object MyAppMain extends MyApp

    class MyApp extends App {

      override protected def run(): Unit = {
        // Core app logic goes here.
        val console = injector.instance[ConsoleWriter]
        console.out.println("Hello, World!")

        // output to the Scala `Console` will also be captured, the following would be equivalent:
        // Console.out.println("Hello, World!")
        // println("Hello, World!")
      }
    }

and in Java:

.. code:: java

    import com.twitter.inject.app.AbstractApp;
    import com.twitter.inject.console.ConsoleWriter;

    public class MyApp extends AbstractApp {

        @Override
        public void run() {
            // Core app logic goes here.
            ConsoleWriter console =
              injector().instance(ConsoleWriter.class);
            console.out().println("Hello, World!");
            // System.out.println("Hello, World!"); will NOT be captured for testing
        }
    }

You could then test emitted console output like so:

.. code:: scala

    import com.twitter.inject.Test
    import com.twitter.inject.app.EmbeddedApp
    import com.twitter.inject.app.TestConsoleWriter
    import com.twitter.inject.app.console.ConsoleWriter

    class MyAppTest extends Test {
      val console = new TestConsoleWriter()
      // build an EmbeddedApp
      def app(): EmbeddedApp = app(new MyApp).bind[ConsoleWriter].toInstance(console)

      override def beforeEach(): Unit = {
        console.reset()
        super.beforeEach()
      }

      test("assert count") {
        val undertest = app()
        undertest.main()

        assert(console.inspectOut() == "Hello, World!\n")
      }
    }

and in Java:

.. code:: java

    import com.twitter.inject.app.EmbeddedApp;
    import com.twitter.inject.app.TestConsoleWriter;
    import com.twitter.inject.app.console.ConsoleWriter;

    import junit.Assert;
    import junit.Test;

    public class MyAppTest extends Assert {

      @Test
      public void testOutput() throws Exception {
        TestConsoleWriter console = new TestConsoleWriter();
        EmbeddedApp app = new EmbeddedApp(myApp).bindClass(ConsoleWriter.class, console);
        app.main();
        assertEquals(console.inspectOut(), "Hello, World!\n");
      }
    }

InMemoryStatsReceiver
---------------------

Finatra prodives a `StatsReceiverModule <https://github.com/twitter/finatra/blob/develop/inject/inject-modules/src/main/scala/com/twitter/inject/modules/StatsReceiverModule.scala>`__ which will bind the `LoadedStatsReceiver <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/stats/LoadedStatsReceiver.scala>`__ to the object graph.

.. note::

    Finagle uses service-loading to allow for users to pick their `com.twitter.finagle.stats.StatsReceiver` implementation. We recommend
    using Twitter's `util-stats <https://twitter.github.io/util/guide/util-stats/index.html>`__ implementation
    which will be service-loaded as the implementation of `LoadedStatsReceiver` if the dependency is on your 
    classpath.

Finagle provides an `com.twitter.finagle.stats.InMemoryStatsReceiver <https://github.com/twitter/util/blob/develop/util-stats/src/main/scala/com/twitter/finagle/stats/InMemoryStatsReceiver.scala>`__
which stores metrics in an in-memory Map to make it simple to query and assert their values. You can bind an instance of the `InMemoryStatsReceiver` to the underlying app's object graph as the implementation of the app's `StatsReceiver` when testing to be able to assert metrics emitted by the application.

Assuming you have an App:

.. code:: scala

    import com.google.inject.Module
    import com.twitter.finagle.stats.StatsReceiver
    import com.twitter.inject.app.App
    import com.twitter.inject.modules.StatsReceiverModule

    object MyAppMain extends MyApp

    class MyApp extends App {

      override val modules: Seq[Module] = Seq(
        StatsReceiverModule
        MyModule1)

      override protected def run(): Unit = {
        // Core app logic goes here.
        val statsReceiver = injector.instance[StatsReceiver]
        statsReceiver.counter("my_counter").incr()
        val fooService = injector.instance[FooService]
        ???
      }
    }

You could then test emitted metrics like so:

.. code:: scala

    import com.twitter.finagle.stats.InMemoryStatsReceiver
    import com.twitter.inject.Test
    import com.twitter.inject.app.EmbeddedApp

    class MyAppTest extends Test {
      private val inMemoryStatsReceiver: InMemoryStatsReceiver = new InMemoryStatsReceiver

      // build an EmbeddedApp
      def app(): EmbeddedApp = app(new MyApp)
      def app(underlying: MyApp): EmbeddedApp = 
        new EmbeddedApp(underlying)
          .bind[Foo].toInstance(new Foo(2))
          .bind[StatsReceiver].toInstance(inMemoryStatsReceiver)

      test("assert count") {
        val undertest = app()
        undertest.main("username" -> "jack")

        val statsReceiver = undertest.injector.instance[StatsReceiver].asInstanceOf[InMemoryStatsReceiver]
        statsReceiver.counter("my_counter")() shouldEqual 1
      }
    }

For more information on the Embedded testing utilities, including on testing with `GlobalFlags <../testing/embedded.html#testing-with-global-flags>`__ see the documentation `here <../testing/embedded.html>`_. Also see the documentation for more information on the `#bind[T] DSL <../testing/bind_dsl.html>`__ used above.

And lastly, for the complete testing guide, see the Testing `table of contents <../#testing>`__.

.. |c.t.app.App| replace:: `util-app App`
.. _c.t.app.App: https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala

.. |c.t.inject.Test| replace:: `c.t.inject.Test`
.. _c.t.inject.Test: https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/Test.scala

.. |c.t.inject.app.App| replace:: `c.t.inject.app.App`
.. _c.t.inject.app.App: https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala
