.. _injectable_twitter_server:

Creating an injectable |TwitterServer|_
=======================================

Lifecycle
---------

If you haven't already, take a look at the |TwitterServer|_
`lifecycle documentation <../getting-started/lifecycle.html#c-t-server-twitterserver-lifecycle>`__.
You may also want to look at the |c.t.app.App|_
`lifecycle documentation <../getting-started/lifecycle.html#c-t-app-app-lifecycle>`__ as well since
|c.t.server.TwitterServer|_ is an extension of |c.t.app.App|_ and thus inherits the |c.t.app.App|_
lifecycle.

Getting Started
---------------

To create an injectable |c.t.server.TwitterServer|_, first depend on the `inject-server` library. We
also recommend using `Logback <https://logback.qos.ch/>`__ as your
`SLF4J <https://www.slf4j.org/manual.html>`__ implementation. E.g.,

with sbt:

.. parsed-literal::

    "com.twitter" %% "inject-server" % "\ |release|\ ",
    "ch.qos.logback" % "logback-classic" % versions.logback,

For more information on logging with Finatra see:
`Introduction to Logging With Finatra <../logging/index.html#introduction-to-logging-with-finatra>`__.

Create a new class which extends |c.t.inject.server.TwitterServer|_:

Basic Example
-------------

An example of a simple injectable |TwitterServer|_:

.. code:: scala

    import com.twitter.inject.server.TwitterServer

    object MyServerMain extends MyServer

    class MyServer extends TwitterServer {

      override protected def start(): Unit = {
        // It is important to remember to NOT BLOCK this method.
        ...
      }
    }

This will use the `inject framework <../getting-started/framework.html#inject>`__ to create an
injectable `TwitterServer`. Finatra provides |c.t.inject.server.TwitterServer|_ as an injectable
version of the `TwitterServer <https://twitter.github.io/twitter-server/>`__
|c.t.server.TwitterServer|_ trait.

Specifically, this allows for the use of `dependency injection <../getting-started/dependency_injection.html#dependency-injection>`__
in a |TwitterServer|_ with support for `Modules <../getting-started/modules.html>`__ allowing for
`powerful feature testing <../testing/index.html#types-of-tests>`__ of the server.

Advanced Example
----------------

.. code:: scala

    import com.twitter.inject.server.TwitterServer

    object MyTwitterServerMain extends MyTwitterServer

    class MyTwitterServer
      extends TwitterServer {

      override val modules = Seq(
        MyModule1)

      override protected def setup(): Unit = {
        // Create/start a pub-sub component and add it to the list of Awaitables, e.g., await(component)
        // It is important to remember to NOT BLOCK this method
        ...
      }

      override protected def warmup(): Unit = {
        handle[MyWarmupHandler]()
      }

      @Lifecycle
      override protected def postWarmup(): Unit = {
        super.postWarmup()

        // It is important to remember to NOT BLOCK this method and to call SUPER.
        ...
      }

      override protected def start(): Unit = {
        // It is important to remember to NOT BLOCK this method
        ...
      }
    }

Overriding Server Lifecycle Functions
-------------------------------------

You can hook into the server startup lifecycle by overriding ``@Lifecycle``-annotated methods or
provided lifecycle callbacks.

``@Lifecycle``-annotated Methods
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- `postInjectorStartup` - after creation and initialization of the Injector.
- `beforePostWarmup` - this phase `performs object promotion <https://github.com/twitter/twitter-server/blob/5096d7ae20006114707a5124ca895744509b3d75/server/src/main/scala/com/twitter/server/Lifecycle.scala#L93>`__
  before the binding of any external interface (which typically means the server will start
  accepting traffic) occurs in `postWarmup`
- `postWarmup` - the framework binds the external HTTP interface.
- `afterPostWarmup` - signal the server is healthy, via
  `Lifecycle.Warmup.warmupComplete <https://github.com/twitter/twitter-server/blob/5096d7ae20006114707a5124ca895744509b3d75/server/src/main/scala/com/twitter/server/Lifecycle.scala#L100>`__.

.. caution:: When overriding a server ``@Lifecycle``-annotated method you **MUST** first call
    `super.lifecycleMethod()` in your overridden implementation to ensure that the server correctly
    completes the startup process.

Lifecycle Callbacks
^^^^^^^^^^^^^^^^^^^

- `setup()` - called at the end of the `postInjectorStartup()` phase.
- `warmup()` - allows for user-defined server warmup.
- `start()` - called at the end of the server `main` before awaiting on any Awaitables.

The main points of entry to the |c.t.inject.server.TwitterServer|_ are the lifecycle callbacks:
``#setup``, ``#warmup``, and ``#start``.

`TwitterServer#setup`
---------------------

The `#setup()` lifecycle callback method is executed at the end of the
`TwitterServer#postInjectorStartup` ``@Lifecycle``-annotated method
(see: `Startup Lifecycle <../getting-started/lifecycle.html#startup>`__). That is, after the creation
of the Injector but before server warmup has been performed allowing for anything created or started
in this callback to be used in warmup and for instances to be promoted to old gen during object
promotion in the `beforePostWarmup` lifecycle phase.

Note: in the |HttpServer|_ and |ThriftServer|_ traits from |finatra-http|_ and |finatra-thrift|_
respectively, routing is configured in the `postInjectorStartup` lifecycle phase. However any logic
in the `#setup` callback will executed **after** all installed modules have started (see:
`TwitterModule Lifecycle <../getting-started/modules.html#module-lifecycle>`__) and **before**
`HttpRouter` or `ThriftRouter` configuration.

What Goes Here?
^^^^^^^^^^^^^^^

Any logic to execute before object promotion and before the server warmup is performed. This is thus
before any external interface has been bound and thus before the server is announced as "healthy".

Any exception thrown from this method will fail the server startup.

When overriding any lifecycle methods and callbacks, it is important to **not perform any blocking
operations** as you will prevent the server from properly starting. If there is blocking work that
must be done, it is strongly recommended that you perform this work in a
`FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__.

See the Finatra utility: |FuturePools|_ for creating named pools.

`TwitterServer#warmup`
----------------------

For detailed information see `HTTP Server Warmup <../http/warmup.html>`__, or
`Thrift Server Warmup <../thrift/warmup.html>`__.

`TwitterServer#start`
---------------------

Any logic to be run after the server is reported as healthy, bound to an external interface, and
before awaiting on any `Awaitables` is placed in the `#start()` method. This is typically starting
long live background processes, starting any processor that should only be started once the
external interface has been successfully bound to port and is accepting traffic, or any other work
that must be completed as part of server startup. See the `Awaiting Awaitables <#awaiting-awaitables>`__ section for
more information.

What Goes Here?
^^^^^^^^^^^^^^^

Work to happen after the server is bound to any external port, has performed warmup, object promotion,
and is announced as "healthy".

Any exception thrown from this method will fail the server startup.

When overriding any lifecycle methods and callbacks, it is important to **not perform any blocking
operations** in your override as you will prevent the server from properly starting. If there is
blocking work that must be done, it is strongly recommended that you perform this work in a
`FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__.

See the Finatra utility: |FuturePools|_ for creating named pools.

Awaiting `Awaitables`
---------------------

If you have long-lived processes which your server starts that you want to ensure exit when the server
exits or trigger the server to exit if the process exits, you should register them as an |c.t.util.Awaitable|_
using the |c.t.inject.server.TwitterServer#await|_ callback function.

The purpose of using this callback is to `entangle <https://en.wikipedia.org/wiki/Quantum_entanglement>`__
all the `Awaitables` within your server such that if any of the `Awaitables` exit, the entire
server process exits. For example, when starting a regular HTTP or Thrift server, you have two
`ListeningServers <https://github.com/twitter/finagle/blob/d2a415b05f57be76dc26aba67bb3e834a0db5d38/finagle-core/src/main/scala/com/twitter/finagle/Server.scala#L13>`__
in process: the TwitterServer |HTTP Admin Interface|_ and the started external server. If you await
(block) on one of the servers and not the other, you can get into a case where the server not being
awaited exits but the process continues to wait on the other server to satisfy the blocking `Awaitable`
and thus does not exit.

Why is this bad?
^^^^^^^^^^^^^^^^

As an example, if you await on just the external interface, the TwitterServer |HTTP Admin Interface|_
may exit because of an error, causing health checking and metrics reporting to fail but your server
process would remain running until killed. Conversely, if you await on just the |HTTP Admin Interface|_
the external server may exit but the admin interface continues to report itself as healthy.

Register `Awaitables`
^^^^^^^^^^^^^^^^^^^^^

The way to ensure that the exiting of a single `Awaitable` triggers exiting of any other `Awaitable`
is to register each `Awaitable` with the server using the |c.t.inject.server.TwitterServer#await|_
callback function. As the last step of the server startup lifecycle, the server will `entangle <https://en.wikipedia.org/wiki/Quantum_entanglement>`__
all given `Awaitables`.

Testing
-------

For details see the `Testing with Finatra <../testing/index.html>`__ section and the Finatra
`examples <https://github.com/twitter/finatra/tree/develop/examples>`__ for detailed examples with tests.

More Information
----------------

For more information on the server lifecycle see the `Application and Server Lifecycle <../getting-started/lifecycle.html>`__
section which contains details around the order of lifecycle events during `startup <../getting-started/lifecycle.html#startup>`__
and considerations during `shutdown <../getting-started/lifecycle.html#shutdown>`__.

.. |c.t.app.App| replace:: `util-app App`
.. _c.t.app.App: https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala

.. |c.t.inject.server.TwitterServer| replace:: ``c.t.inject.server.TwitterServer``
.. _c.t.inject.server.TwitterServer: https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/main/scala/com/twitter/inject/server/TwitterServer.scala

.. |c.t.server.TwitterServer| replace:: ``c.t.server.TwitterServer``
.. _c.t.server.TwitterServer: https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala

.. |TwitterServer| replace:: `TwitterServer`
.. _TwitterServer: https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala

.. |HttpServer| replace:: ``HttpServer``
.. _HttpServer: ../http/server.html

.. |ThriftServer| replace:: ``ThriftServer``
.. _ThriftServer: ../thrift/server.html

.. |FuturePools| replace:: ``c.t.finatra.utils.FuturePools``
.. _FuturePools: https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/utils/FuturePools.scala

.. |finatra-http| replace:: `finatra-http`
.. _finatra-http: ../http/server.html

.. |finatra-thrift| replace:: `finatra-thrift`
.. _finatra-thrift: ../thrift/server.html

.. |c.t.util.Awaitable| replace:: ``c.t.util.Awaitable``
.. _c.t.util.Awaitable: https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Awaitable.scala

.. |c.t.inject.server.TwitterServer#await| replace:: ``c.t.inject.server.TwitterServer#await``
.. _c.t.inject.server.TwitterServer#await: https://github.com/twitter/finatra/blob/4d662426584d3811fe87f1cd976166e4f2465131/inject/inject-server/src/main/scala/com/twitter/inject/server/TwitterServer.scala#L123

.. |HTTP Admin Interface| replace:: `HTTP Admin Interface`
.. _HTTP Admin Interface: https://twitter.github.io/twitter-server/Features.html#admin-http-interface>
