Creating an injectable |TwitterServer|_
=======================================

To create an injectable |c.t.server.TwitterServer|_, first depend on the `inject-server` library. Then
use the `inject framework <../getting-started/framework.html#inject>`__ to create an injectable
TwitterServer. Finatra provides an injectable version of the |c.t.server.TwitterServer|_ trait:
|c.t.inject.server.TwitterServer|_.

Extending the |c.t.inject.server.TwitterServer|_ trait creates an injectable |TwitterServer|_.

This allows for the use of `dependency injection <../getting-started/basics.html#dependency-injection>`__
in a |TwitterServer|_ with support for `modules <../getting-started/modules.html>`__ which allows
for `powerful feature testing <../testing/index.html#types-of-tests>`__ of the server.

Example
-------

.. code:: scala

	import com.twitter.inject.Logging
	import com.twitter.inject.server.TwitterServer

	object MyTwitterServerMain extends MyTwitterServer

	class MyTwitterServer
	  extends TwitterServer
	  with Logging {

	  override val modules = Seq(
	    MyModule1)

	  @Lifecycle
	  override protected def postWarmup(): Unit = {
	    super.postWarmup()

	    // Bind any external interfaces here, register them to be closed on server exit, and add them
	    // to the list of awaitables that the server will block on by call the this.await(Awaitable*) method.

	    // It is important to remember to NOT BLOCK this method.
	  }

	  override protected def start(): Unit = {
	    // Any additional server startup logic to perform after the server is reported healthy goes here.

	    // It is important to remember to NOT BLOCK this method.
	  }
	}

The two points of entry to the |c.t.inject.server.TwitterServer|_ are the ``#postWarmup`` and ``#start`` methods.

`TwitterServer#postWarmup`
--------------------------

If you look at the `Startup Lifecycle <../getting-started/lifecycle.html#startup>`__ you will see that ``postWarmup`` is executed
after server warmup has been performed and thus after `object promotion and initial garbage collection <../getting-started/lifecycle.html#application-and-server-lifecycle>`__,
but before reporting that the server is healthy (via the `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface#lifecycle-management>`__
``/health`` endpoint).

In the |HttpServer|_ and |ThriftServer|_ traits the framework will bind any external interfaces and
perform announcing of the server in `postWarmup` ``@Lifecycle`` method. External interface binding and
announcing is done at this point in the lifecycle such that if binding the external interface to a
port fails, we do not report the server as healthy in the
`HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface#lifecycle-management>`__.

.. note::

  Put any logic to happen before the server is announced as "healthy" in the ``postWarmup`` lifecycle method. Typically, this
  is any binding of an external interface but could be any logic that must be done to present the server as "healthy".

  Remember, that when overriding any ``@Lifecycle``-annotated method you **MUST** first call `super()` in the method to ensure
  the framework lifecycle events happen accordingly.

  As with all lifecycle methods, it is important to **not perform any blocking operations** as you will prevent the server
  from starting. If there is blocking work that must be done, it is strongly recommended that you perform this work in a
  `FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__.
  See the Finatra utility: |FuturePools|_ for creating named pools.

Any exception thrown from this method will fail the server startup.

`TwitterServer#start`
---------------------

Any logic to be run after the server is reported as healthy, bound to an external interface and fully started is placed in the
``#start`` method. This is typically starting long live background processes, starting a processor for incoming data from an
external interface (e.g., it should only be started once the external interface has been successfully bound to port and is accepting
traffic), or any other work that must be completed as part of server startup.

.. note::

  Put any work to happen after the server is bound to an external port and announced as healthy in the ``start`` lifecycle method.

  Remember, that when overriding any ``@Lifecycle``-annotated method you **MUST** first call `super()` in the method to ensure
  the framework lifecycle events happen accordingly.

  As with all lifecycle methods, it is important to **not perform any blocking operations** as you will prevent the server
  from starting. If there is blocking work that must be done, it is strongly recommended that you perform this work in a
  `FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__.
  See the Finatra utility: |FuturePools|_ for creating named pools.

Any exception thrown from this method will fail the server startup.

.. important::

  Users should prefer using the |HttpServer|_ or |ThriftServer|_ traits from |finatra-http|_ and
  |finatra-thrift|_ respectively, for serving HTTP or Thrift external interfaces over overriding
  lifecycle methods.

Testing
-------

For details see the `Testing with Finatra <../testing/index.html>`__ section and the Finatra `examples <https://github.com/twitter/finatra/tree/develop/examples>`__ for detailed examples with tests.

More Information
----------------

For more information on the server lifecycle see the `Application and Server Lifecycle <../getting-started/lifecycle.html>`__ section
which contains details around the order of lifecycle events during `startup <../getting-started/lifecycle.html#startup>`__ and
considerations during `shutdown <../getting-started/lifecycle.html#shutdown>`__.

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

.. |FuturePools| replace:: ``FuturePools``
.. _FuturePools: https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/utils/FuturePools.scala

.. |finatra-http| replace:: `finatra-http`
.. _finatra-http: ../http/server.html

.. |finatra-thrift| replace:: `finatra-thrift`
.. _finatra-thrift: ../thrift/server.html
